package app.owlcms.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public sealed interface AuditMarker
		permits AuditMarker.First, AuditMarker.Continue, AuditMarker.Seal, AuditMarker.Final {
	int VERSION = 1;
	long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

	enum Type {
		FIRST, CONTINUE, SEAL, FINAL
	}

	record Identity(String stream, String runId, String artifactName, Instant sealedAt,
			String applicationVersion, String keyFingerprint) {
		public Identity {
			requireText(stream, "stream");
			requireText(runId, "runId");
			requireFilename(artifactName);
			Objects.requireNonNull(sealedAt, "sealedAt");
			requireText(applicationVersion, "applicationVersion");
			AuditBlockHash.requireHash(keyFingerprint);
		}
	}

	record Block(long firstLine, long lastLine, long byteCount, String sha256) {
		public Block {
			requirePositive(firstLine, "firstLine");
			requirePositive(lastLine, "lastLine");
			requirePositive(byteCount, "byteCount");
			if (lastLine < firstLine) {
				throw new IllegalArgumentException("Audit block range is reversed");
			}
			AuditBlockHash.requireHash(sha256);
		}

		public static Block of(long firstLine, long lastLine, byte[] bytes) {
			return new Block(firstLine, lastLine, bytes.length, AuditBlockHash.sha256(bytes));
		}

		public boolean matches(byte[] bytes) {
			return byteCount == bytes.length && sha256.equals(AuditBlockHash.sha256(bytes));
		}
	}

	record First(Identity identity, Block block) implements AuditMarker {
		public First {
			requireHeader(identity, block);
		}

		@Override
		public Type type() {
			return Type.FIRST;
		}
	}

	record Continue(Identity identity, Block block, String previousFile, String previousBlockSha256)
			implements AuditMarker {
		public Continue {
			requireHeader(identity, block);
			requireFilename(previousFile);
			if (previousFile.equals(identity.artifactName())) {
				throw new IllegalArgumentException("An audit file cannot continue itself");
			}
			AuditBlockHash.requireHash(previousBlockSha256);
		}

		@Override
		public Type type() {
			return Type.CONTINUE;
		}
	}

	record Seal(Identity identity, Block block, String previousBlockSha256) implements AuditMarker {
		public Seal {
			Objects.requireNonNull(identity, "identity");
			Objects.requireNonNull(block, "block");
			if (block.firstLine() <= 1) {
				throw new IllegalArgumentException("SEAL must follow the header block");
			}
			AuditBlockHash.requireHash(previousBlockSha256);
		}

		@Override
		public Type type() {
			return Type.SEAL;
		}
	}

	record Final(Identity identity, long recordCount, String lastBlockSha256) implements AuditMarker {
		public Final {
			Objects.requireNonNull(identity, "identity");
			requirePositive(recordCount, "recordCount");
			AuditBlockHash.requireHash(lastBlockSha256);
		}

		@Override
		public Type type() {
			return Type.FINAL;
		}
	}

	Type type();

	Identity identity();

	default Map<String, Object> payload() {
		Identity identity = identity();
		Map<String, Object> fields = new TreeMap<>();
		fields.put("version", VERSION);
		fields.put("marker", type().name());
		fields.put("stream", identity.stream());
		fields.put("runId", identity.runId());
		fields.put("artifactName", identity.artifactName());
		fields.put("sealedAt", identity.sealedAt().toString());
		fields.put("applicationVersion", identity.applicationVersion());
		fields.put("keyFingerprint", identity.keyFingerprint());
		Block block;
		switch (this) {
			case First first -> block = first.block();
			case Continue continuation -> {
				block = continuation.block();
				fields.put("previousFile", continuation.previousFile());
				fields.put("previousBlockSha256", continuation.previousBlockSha256());
			}
			case Seal seal -> {
				block = seal.block();
				fields.put("previousBlockSha256", seal.previousBlockSha256());
			}
			case Final end -> {
				fields.put("recordCount", end.recordCount());
				fields.put("lastBlockSha256", end.lastBlockSha256());
				return fields;
			}
		}
		fields.put("firstLine", block.firstLine());
		fields.put("lastLine", block.lastLine());
		fields.put("byteCount", block.byteCount());
		fields.put("sha256", block.sha256());
		return fields;
	}

	private static void requireHeader(Identity identity, Block block) {
		Objects.requireNonNull(identity, "identity");
		Objects.requireNonNull(block, "block");
		if (block.firstLine() != 1 || block.lastLine() != 1) {
			throw new IllegalArgumentException("Header markers must cover sequence 1 only");
		}
	}

	private static void requirePositive(long value, String name) {
		if (value < 1 || value > MAX_SAFE_INTEGER) {
			throw new IllegalArgumentException(name + " must be a positive JSON-safe integer");
		}
	}

	private static void requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " is required");
		}
	}

	private static void requireFilename(String filename) {
		requireText(filename, "artifact filename");
		if (filename.equals(".") || filename.equals("..") || filename.contains("/")
				|| filename.contains("\\") || filename.contains(":")
				|| filename.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("Audit artifact names must be plain relative filenames");
		}
	}
}
