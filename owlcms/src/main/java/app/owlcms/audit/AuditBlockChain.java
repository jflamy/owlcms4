package app.owlcms.audit;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Checks a segment incrementally; a missing external predecessor is reported by the caller, not trusted here. */
public final class AuditBlockChain {
	private AuditMarker.Identity identity;
	private long lastLine;
	private String lastHash;
	private boolean finalized;

	public void accept(AuditMarkerCodec.Signed signed, byte[] recordBytes, AuditPublicKey key) throws IOException {
		try {
			if (!AuditMarkerCodec.verify(signed, key)) {
				throw new IOException("Audit marker signature or public-key fingerprint does not match");
			}
		} catch (GeneralSecurityException e) {
			throw new IOException("Unable to verify audit marker signature", e);
		}
		AuditMarker marker = signed.marker();
		if (this.finalized) {
			throw new IOException("Audit marker follows FINAL in the same segment");
		}
		if (this.identity != null && !sameSegment(this.identity, marker.identity())) {
			throw new IOException("Audit marker changes the segment identity");
		}
		switch (marker) {
			case AuditMarker.First first -> {
				requireUnopened();
				checkBlock(first.block(), recordBytes);
				setBlock(marker.identity(), first.block());
			}
			case AuditMarker.Continue continuation -> {
				requireUnopened();
				checkBlock(continuation.block(), recordBytes);
				setBlock(marker.identity(), continuation.block());
			}
			case AuditMarker.Seal seal -> {
				if (this.identity == null || seal.block().firstLine() != this.lastLine + 1
						|| !seal.previousBlockSha256().equals(this.lastHash)) {
					throw new IOException("Audit block has a gap, overlap or broken predecessor link");
				}
				checkBlock(seal.block(), recordBytes);
				setBlock(marker.identity(), seal.block());
			}
			case AuditMarker.Final end -> {
				if (this.identity == null || end.recordCount() != this.lastLine
						|| !end.lastBlockSha256().equals(this.lastHash) || recordBytes.length != 0) {
					throw new IOException("FINAL does not match the segment's last sealed block and count");
				}
				this.finalized = true;
			}
		}
	}

	public static void checkContinuation(AuditMarker.Continue next, AuditMarkerCodec.Signed previousFinal,
			AuditPublicKey key) throws IOException {
		try {
			if (!(previousFinal.marker() instanceof AuditMarker.Final end)
					|| !AuditMarkerCodec.verify(previousFinal, key)
					|| !next.previousFile().equals(end.identity().artifactName())
					|| !next.previousBlockSha256().equals(end.lastBlockSha256())
					|| !next.identity().runId().equals(end.identity().runId())
					|| !next.identity().stream().equals(end.identity().stream())
					|| !next.identity().keyFingerprint().equals(end.identity().keyFingerprint())) {
				throw new IOException("CONTINUE does not match the named previous file's signed FINAL");
			}
		} catch (GeneralSecurityException e) {
			throw new IOException("Unable to verify previous FINAL", e);
		}
	}

	public long lastValidatedLine() {
		return this.lastLine;
	}

	public boolean isFinalized() {
		return this.finalized;
	}

	private void requireUnopened() throws IOException {
		if (this.identity != null) {
			throw new IOException("Duplicate FIRST or CONTINUE in the same segment");
		}
	}

	private static void checkBlock(AuditMarker.Block block, byte[] bytes) throws IOException {
		if (!block.matches(bytes)) {
			throw new IOException("Audit block byte count or SHA-256 does not match");
		}
	}

	private void setBlock(AuditMarker.Identity identity, AuditMarker.Block block) {
		if (this.identity == null) {
			this.identity = identity;
		}
		this.lastLine = block.lastLine();
		this.lastHash = block.sha256();
	}

	private static boolean sameSegment(AuditMarker.Identity first, AuditMarker.Identity next) {
		return first.stream().equals(next.stream()) && first.runId().equals(next.runId())
				&& first.artifactName().equals(next.artifactName())
				&& first.applicationVersion().equals(next.applicationVersion())
				&& first.keyFingerprint().equals(next.keyFingerprint());
	}
}
