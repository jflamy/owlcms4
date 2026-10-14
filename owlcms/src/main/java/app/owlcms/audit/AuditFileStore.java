package app.owlcms.audit;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** File lifecycle is confined to the audit I/O worker; signing publishes through each stream's output lock. */
public final class AuditFileStore implements AutoCloseable {
	static final DateTimeFormatter NAME_TIME =
			DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH-mm-ss.SSS'Z'").withZone(ZoneOffset.UTC);
	static final String SEALED_INFIX = "_sealed_";
	static final String PLAIN_INFIX = "_plain_";
	static final String NAME_TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}\\.\\d{3}Z";
	private final Path directory;
	private final AuditSigningKey key;
	private final AuditSigningQueue signing;
	private final AuditSealLimits limits;
	private final Clock clock;
	private final LongSupplier nanos;
	private final String runId;
	private final String version;
	/** Notified after each event's plain line is written; audit.open headers are not plain lines. */
	private final BiConsumer<String, String> readable;
	private final Map<String, FileState> streams = new HashMap<>();
	private final Map<String, String> filenames = new HashMap<>();
	/*
	 * Records reach this worker in capture order, so an audit.open header stamped with the time of an adjacent
	 * record keeps timestamps in sequence order; the worker's own clock may already be later than queued records.
	 */
	private final Map<String, OffsetDateTime> lastTimestamps = new HashMap<>();
	private boolean closed;

	public AuditFileStore(Path directory, AuditSigningKey key, AuditSealLimits limits,
			Clock clock, LongSupplier nanos, String runId, String version,
			BiConsumer<String, String> readable) throws IOException {
		Files.createDirectories(directory);
		this.directory = directory;
		this.key = key;
		this.signing = new AuditSigningQueue(limits.queueBytes());
		this.limits = limits;
		this.clock = clock;
		this.nanos = nanos;
		this.runId = runId;
		this.version = version;
		this.readable = readable;
	}

	public void append(String platform, List<AuditRecordSnapshot> records, boolean closeBlock) throws IOException {
		requireOpen();
		String stream = platform == null || platform.isBlank() ? "competition" : platform;
		FileState state = this.streams.get(stream);
		if (state == null) {
			String prefix = sanitize(stream);
			String owner = this.filenames.putIfAbsent(prefix, stream);
			if (owner != null && !owner.equals(stream)) {
				throw new IOException("Audit platform names collide after filename sanitization");
			}
			OffsetDateTime openedAt = records.isEmpty() ? OffsetDateTime.now(this.clock)
					: records.getFirst().timestamp();
			state = open(stream, latest(prefix), null, null, openedAt);
			this.streams.put(stream, state);
		}
		for (AuditRecordSnapshot record : records) {
			state = rollIfNeeded(stream, state, record.timestamp());
			long sequence = state.segment.nextSequence();
			state.segment.append(record.full(sequence));
			state.segment.awaitSeals();
			String plainLine = record.readable(sequence);
			state.plain.append(plainLine);
			this.readable.accept(stream, plainLine);
			this.lastTimestamps.put(stream, record.timestamp());
			state = rollIfNeeded(stream, state, record.timestamp());
		}
		if (closeBlock) {
			state.segment.closeBlock();
			state.segment.awaitSeals();
			rollIfNeeded(stream, state, this.lastTimestamps.get(stream));
		}
	}

	public void closeBlock(String platform) throws IOException {
		requireOpen();
		FileState state = this.streams.get(platform);
		if (state != null) {
			state.segment.closeBlock();
			state.segment.awaitSeals();
			rollIfNeeded(platform, state, this.lastTimestamps.get(platform));
		}
	}

	public void tick() throws IOException {
		requireOpen();
		for (Map.Entry<String, FileState> entry : new ArrayList<>(this.streams.entrySet())) {
			entry.getValue().segment.tick();
			entry.getValue().segment.awaitSeals();
			rollIfNeeded(entry.getKey(), entry.getValue(), this.lastTimestamps.get(entry.getKey()));
		}
	}

	/** The sealed files currently being written, one per stream. */
	public Set<Path> openFiles() {
		Set<Path> open = new HashSet<>();
		this.streams.values().forEach(state -> open.add(state.path));
		return open;
	}

	public void awaitSeals() throws IOException {
		for (FileState state : this.streams.values()) {
			state.segment.awaitSeals();
		}
	}

	public void abort() throws IOException {
		finish(false);
	}

	@Override
	public void close() throws IOException {
		finish(true);
	}

	private void finish(boolean finalize) throws IOException {
		if (this.closed) {
			return;
		}
		this.closed = true;
		IOException failure = null;
		for (FileState state : this.streams.values()) {
			try {
				if (finalize) {
					state.segment.close();
					verifyCurrentSegment(state);
				} else {
					state.segment.abort();
				}
			} catch (IOException | RuntimeException e) {
				IOException failed = e instanceof IOException io ? io : new IOException("Audit finalization failed", e);
				if (failure == null) {
					failure = failed;
				} else {
					failure.addSuppressed(failed);
				}
			}
			try {
				state.plain.close();
			} catch (IOException e) {
				if (failure == null) {
					failure = e;
				} else {
					failure.addSuppressed(e);
				}
			}
		}
		try {
			this.signing.close();
		} catch (IOException e) {
			if (failure == null) {
				failure = e;
			} else {
				failure.addSuppressed(e);
			}
		}
		if (failure != null) {
			throw failure;
		}
	}

	private FileState rollIfNeeded(String stream, FileState state, OffsetDateTime openedAt) throws IOException {
		if (state.output.bytes < this.limits.fileBytes()) {
			return state;
		}
		return roll(stream, state, openedAt);
	}

	/**
	 * Finalizes every open file and continues each stream in a new one; returns the files now open.
	 *
	 * @param openedAt time for the new files' audit.open headers, captured in record order by the caller
	 */
	public Set<Path> rollAll(OffsetDateTime openedAt) throws IOException {
		requireOpen();
		Set<Path> open = new HashSet<>();
		for (Map.Entry<String, FileState> entry : new ArrayList<>(this.streams.entrySet())) {
			open.add(roll(entry.getKey(), entry.getValue(), openedAt).path);
		}
		return open;
	}

	private FileState roll(String stream, FileState state, OffsetDateTime openedAt) throws IOException {
		state.segment.close();
		state.plain.close();
		verifyCurrentSegment(state);
		String previousHash = state.segment.lastBlockHash();
		FileState next = open(stream, null, state.path.getFileName().toString(), previousHash, openedAt);
		this.streams.put(stream, next);
		return next;
	}

	private FileState open(String stream, Path latest, String previousFile, String previousHash,
			OffsetDateTime requestedOpenedAt) throws IOException {
		OffsetDateTime previousStamp = this.lastTimestamps.get(stream);
		OffsetDateTime openedAt = requestedOpenedAt != null ? requestedOpenedAt : OffsetDateTime.now(this.clock);
		if (previousStamp != null && openedAt.isBefore(previousStamp)) {
			openedAt = previousStamp;
		}
		String prefix = sanitize(stream);
		Path path;
		FileChannel channel;
		if (latest != null && Files.size(latest) < this.limits.fileBytes()) {
			path = latest;
			channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		} else {
			Instant stamp = this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
			Path newest = latest(prefix);
			if (newest != null) {
				String name = newest.getFileName().toString();
				Instant lastStamp = Instant.from(NAME_TIME.parse(
						name.substring(prefix.length() + SEALED_INFIX.length(), name.length() - 4)));
				if (!stamp.isAfter(lastStamp)) {
					stamp = lastStamp.plusMillis(1);
				}
			}
			while (true) {
				path = this.directory.resolve(prefix + SEALED_INFIX + NAME_TIME.format(stamp) + ".log");
				try {
					channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
					break;
				} catch (FileAlreadyExistsException e) {
					stamp = stamp.plusMillis(1);
				}
			}
		}
		ChannelOutput output = new ChannelOutput(channel, channel.size());
		try {
			if (output.bytes > 0) {
				try (FileChannel read = FileChannel.open(path, StandardOpenOption.READ)) {
					ByteBuffer last = ByteBuffer.allocate(1);
					read.read(last, output.bytes - 1);
					if (last.array()[0] != '\n') {
						output.write('\n');
					}
				}
			}
			long start = output.bytes;
			String detail = AuditFormat.kvs("version", this.version, "runId", this.runId,
					"publicKey", this.key.publicKeyBase64(), "keyFingerprint", this.key.fingerprint(),
					"keySource", this.key.isBuiltin() ? "builtin" : "configured");
			if (previousFile != null) {
				detail += "," + AuditFormat.kvs("previousFile", previousFile, "previousBlockSha256", previousHash);
			}
			AuditRecordSnapshot header = AuditRecordSnapshot.capture(AuditEntry.builder(stream, "audit.open")
					.actor(AuditActor.system()).detail(detail).build(), openedAt);
			SealedAuditStream segment = new SealedAuditStream(output, this.key, this.signing, this.limits,
					this.clock, this.nanos, stream, this.runId, path.getFileName().toString(), this.version,
					header.full(1), previousFile, previousHash);
			this.lastTimestamps.put(stream, openedAt);
			return new FileState(path, start, output, segment, new PlainFile(plainPath(prefix, path)));
		} catch (IOException | RuntimeException e) {
			try {
				output.close();
			} catch (IOException closeError) {
				e.addSuppressed(closeError);
			}
			throw e;
		}
	}

	private Path latest(String prefix) throws IOException {
		String pattern = Pattern.quote(prefix) + SEALED_INFIX + NAME_TIME_PATTERN + "\\.log";
		try (Stream<Path> files = Files.list(this.directory)) {
			return files.filter(path -> path.getFileName().toString().matches(pattern))
					.max(Comparator.comparing(path -> path.getFileName().toString())).orElse(null);
		}
	}

	private void verifyCurrentSegment(FileState state) throws IOException {
		AuditPublicKey publicKey = AuditPublicKey.fromBase64(this.key.publicKeyBase64());
		AuditBlockChain chain = new AuditBlockChain();
		Map<Long, byte[]> records = new TreeMap<>();
		try (InputStream input = new BufferedInputStream(Files.newInputStream(state.path))) {
			input.skipNBytes(state.start);
			ByteArrayOutputStream line = new ByteArrayOutputStream();
			int value;
			while ((value = input.read()) >= 0) {
				line.write(value);
				if (value != '\n') {
					continue;
				}
				byte[] raw = line.toByteArray();
				line.reset();
				String text = new String(raw, 0, raw.length - 1, StandardCharsets.UTF_8);
				if (text.startsWith("FIRST ") || text.startsWith("CONTINUE ")
						|| text.startsWith("SEAL ") || text.startsWith("FINAL ")) {
					AuditMarkerCodec.Signed signed = AuditMarkerCodec.parse(text);
					if (!signed.marker().identity().artifactName().equals(state.path.getFileName().toString())
							|| !signed.marker().identity().runId().equals(this.runId)) {
						throw new IOException("Read-back audit segment identity does not match");
					}
					AuditMarker.Block block = blockOf(signed.marker());
					ByteArrayOutputStream covered = new ByteArrayOutputStream();
					if (block != null) {
						for (long sequence = block.firstLine(); sequence <= block.lastLine(); sequence++) {
							byte[] record = records.remove(sequence);
							if (record == null) {
								throw new IOException("Read-back audit block has a missing record");
							}
							covered.writeBytes(record);
						}
					}
					chain.accept(signed, covered.toByteArray(), publicKey);
				} else {
					int separator = text.indexOf(" | ");
					if (separator < 0) {
						throw new IOException("Read-back audit record has no sequence separator");
					}
					long sequence;
					try {
						sequence = Long.parseLong(text.substring(0, separator).strip());
					} catch (NumberFormatException e) {
						throw new IOException("Read-back audit sequence is invalid", e);
					}
					if (records.put(sequence, raw) != null) {
						throw new IOException("Read-back audit sequence is duplicated");
					}
				}
			}
			if (line.size() != 0 || !records.isEmpty() || !chain.isFinalized()) {
				throw new IOException("Read-back audit segment is incomplete");
			}
		}
	}

	static AuditMarker.Block blockOf(AuditMarker marker) {
		return switch (marker) {
			case AuditMarker.First first -> first.block();
			case AuditMarker.Continue continuation -> continuation.block();
			case AuditMarker.Seal seal -> seal.block();
			case AuditMarker.Final _ -> null;
		};
	}

	static String sanitize(String stream) {
		return stream.replaceAll("[^A-Za-z0-9_-]", "_");
	}

	private void requireOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Audit file store is closed");
		}
	}

	/** The plain file pairs with its sealed file: same stream prefix and timestamp. */
	private Path plainPath(String prefix, Path sealed) {
		String name = sealed.getFileName().toString();
		return this.directory.resolve(prefix + PLAIN_INFIX + name.substring(prefix.length() + SEALED_INFIX.length()));
	}

	private record FileState(Path path, long start, ChannelOutput output, SealedAuditStream segment,
			PlainFile plain) {
	}

	/** Opened on its first line, so a file holding no events yet is never created. */
	static final class PlainFile {
		private final Path path;
		private FileChannel channel;

		PlainFile(Path path) {
			this.path = path;
		}

		void append(String line) throws IOException {
			if (this.channel == null) {
				boolean needsNewline = false;
				if (Files.exists(this.path) && Files.size(this.path) > 0) {
					try (FileChannel read = FileChannel.open(this.path, StandardOpenOption.READ)) {
						ByteBuffer last = ByteBuffer.allocate(1);
						read.read(last, read.size() - 1);
						needsNewline = last.array()[0] != '\n';
					}
				}
				this.channel = FileChannel.open(this.path, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
						StandardOpenOption.APPEND);
				if (needsNewline) {
					write(new byte[] { '\n' });
				}
			}
			write((line + "\n").getBytes(StandardCharsets.UTF_8));
		}

		private void write(byte[] data) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			while (buffer.hasRemaining()) {
				this.channel.write(buffer);
			}
		}

		void close() throws IOException {
			if (this.channel != null) {
				try {
					this.channel.force(false);
				} finally {
					this.channel.close();
					this.channel = null;
				}
			}
		}
	}

	private static final class ChannelOutput extends OutputStream {
		private final FileChannel channel;
		private volatile long bytes;

		ChannelOutput(FileChannel channel, long bytes) {
			this.channel = channel;
			this.bytes = bytes;
		}

		@Override
		public void write(int value) throws IOException {
			write(new byte[] { (byte) value });
		}

		@Override
		public void write(byte[] data) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			while (buffer.hasRemaining()) {
				this.bytes += this.channel.write(buffer);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				this.channel.force(false);
			} finally {
				this.channel.close();
			}
		}
	}
}
