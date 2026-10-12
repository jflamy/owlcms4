package app.owlcms.audit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit files written when integrity mode is off: {@code <stream>_plain_<time>.log} holds the readable event lines and
 * {@code <stream>_full_<time>.log} the full records, with no markers. Each process run starts a new pair; a pair also
 * rolls when the full file reaches the size limit. Callers serialize access.
 */
final class UnsealedAuditFiles {
	static final String FULL_INFIX = "_full_";

	private final Path directory;
	private final long fileBytes;
	private final String openDetail;
	private final Map<String, Pair> streams = new HashMap<>();

	UnsealedAuditFiles(Path directory, long fileBytes, String openDetail) {
		this.directory = directory;
		this.fileBytes = fileBytes;
		this.openDetail = openDetail;
	}

	void write(AuditEntry entry, OffsetDateTime now) throws IOException {
		String stream = entry.platform() == null || entry.platform().isBlank() ? "competition" : entry.platform();
		Pair pair = this.streams.get(stream);
		if (pair == null || pair.fullBytes >= this.fileBytes) {
			if (pair != null) {
				pair.close();
			}
			pair = open(stream, now);
			this.streams.put(stream, pair);
		}
		long sequence = ++pair.sequence;
		pair.plain.append(AuditFormat.format(sequence, entry, now, false));
		pair.appendFull(AuditFormat.format(sequence, entry, now, true));
	}

	private Pair open(String stream, OffsetDateTime now) throws IOException {
		Files.createDirectories(this.directory);
		String prefix = AuditFileStore.sanitize(stream);
		Instant stamp = now.toInstant().truncatedTo(ChronoUnit.MILLIS);
		while (true) {
			String time = AuditFileStore.NAME_TIME.format(stamp);
			Path full = this.directory.resolve(prefix + FULL_INFIX + time + ".log");
			Path plain = this.directory.resolve(prefix + AuditFileStore.PLAIN_INFIX + time + ".log");
			if (!Files.exists(plain)) {
				try {
					FileChannel channel = FileChannel.open(full, StandardOpenOption.CREATE_NEW,
							StandardOpenOption.WRITE);
					Pair pair = new Pair(channel, new AuditFileStore.PlainFile(plain));
					pair.appendFull(AuditFormat.format(++pair.sequence, AuditEntry.builder(stream, "audit.open")
							.actor(AuditActor.system()).detail(this.openDetail).build(), now, true));
					return pair;
				} catch (FileAlreadyExistsException e) {
					// another file has this time; try the next millisecond
				}
			}
			stamp = stamp.plusMillis(1);
		}
	}

	private static final class Pair {
		private final FileChannel full;
		private final AuditFileStore.PlainFile plain;
		private long fullBytes;
		private long sequence;

		Pair(FileChannel full, AuditFileStore.PlainFile plain) {
			this.full = full;
			this.plain = plain;
		}

		void appendFull(String line) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap((line + "\n").getBytes(StandardCharsets.UTF_8));
			while (buffer.hasRemaining()) {
				this.fullBytes += this.full.write(buffer);
			}
		}

		void close() throws IOException {
			try {
				this.plain.close();
			} finally {
				this.full.close();
			}
		}
	}
}
