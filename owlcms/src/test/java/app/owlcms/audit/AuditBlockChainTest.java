package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.Test;

public class AuditBlockChainTest {
	private final byte[] header = "1 | audit.open\n".getBytes(StandardCharsets.UTF_8);
	private final byte[] records = "2 | clock.start\n3 | referee.decision\n".getBytes(StandardCharsets.UTF_8);

	@Test
	public void validSegmentChecksEveryBlockThenFinal() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditPublicKey verifier = AuditPublicKey.fromBase64(key.publicKeyBase64());
		AuditMarker.Identity identity = identity(key, "old.log", "run-123");
		AuditMarker.First first = first(identity);
		AuditMarker.Seal seal = seal(identity, first.block().sha256());
		AuditBlockChain chain = new AuditBlockChain();
		chain.accept(AuditMarkerCodec.sign(first, key), this.header, verifier);
		chain.accept(AuditMarkerCodec.sign(seal, key), this.records, verifier);
		assertFalse(chain.isFinalized());
		assertEquals(3, chain.lastValidatedLine());
		AuditMarker.Final end = new AuditMarker.Final(identity, 3, seal.block().sha256());
		chain.accept(AuditMarkerCodec.sign(end, key), new byte[0], verifier);
		assertTrue(chain.isFinalized());
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(seal, key), this.records, verifier));
	}

	@Test
	public void gapsDuplicatesReorderingAndAlteredRecordsFail() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditPublicKey verifier = AuditPublicKey.fromBase64(key.publicKeyBase64());
		AuditMarker.Identity identity = identity(key, "old.log", "run-123");
		AuditMarker.First first = first(identity);
		AuditMarker.Seal seal = seal(identity, first.block().sha256());
		AuditBlockChain empty = new AuditBlockChain();
		assertThrows(IOException.class, () -> empty.accept(AuditMarkerCodec.sign(seal, key), this.records, verifier));

		AuditBlockChain chain = new AuditBlockChain();
		chain.accept(AuditMarkerCodec.sign(first, key), this.header, verifier);
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(first, key), this.header, verifier));
		byte[] changed = this.records.clone();
		changed[0] ^= 1;
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(seal, key), changed, verifier));
		AuditMarker.Seal gap = new AuditMarker.Seal(identity,
				AuditMarker.Block.of(4, 5, this.records), first.block().sha256());
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(gap, key), this.records, verifier));
		AuditMarker.Seal brokenLink = new AuditMarker.Seal(identity, seal.block(), "0".repeat(64));
		assertThrows(IOException.class,
				() -> chain.accept(AuditMarkerCodec.sign(brokenLink, key), this.records, verifier));
		chain.accept(AuditMarkerCodec.sign(seal, key), this.records, verifier);
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(seal, key), this.records, verifier));
	}

	@Test
	public void finalMustMatchLastBlockAndSegmentCount() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditPublicKey verifier = AuditPublicKey.fromBase64(key.publicKeyBase64());
		AuditMarker.Identity identity = identity(key, "old.log", "run-123");
		AuditMarker.First first = first(identity);
		AuditBlockChain chain = new AuditBlockChain();
		chain.accept(AuditMarkerCodec.sign(first, key), this.header, verifier);
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(
				new AuditMarker.Final(identity, 2, first.block().sha256()), key), new byte[0], verifier));
		assertThrows(IOException.class, () -> chain.accept(AuditMarkerCodec.sign(
				new AuditMarker.Final(identity, 1, "0".repeat(64)), key), new byte[0], verifier));
		assertFalse(chain.isFinalized());
		assertEquals(1, chain.lastValidatedLine());
	}

	@Test
	public void continuationChecksLocallyAndAgainstNamedPriorFinal() throws Exception {
		AuditSigningKey key = AuditSigningKey.builtin();
		AuditPublicKey verifier = AuditPublicKey.fromBase64(key.publicKeyBase64());
		AuditMarker.Identity oldIdentity = identity(key, "old.log", "run-123");
		AuditMarker.Identity newIdentity = identity(key, "new.log", "run-123");
		AuditMarker.Final end = new AuditMarker.Final(oldIdentity, 3, AuditBlockHash.sha256(this.records));
		AuditMarker.Continue continuation = new AuditMarker.Continue(newIdentity,
				AuditMarker.Block.of(1, 1, this.header), "old.log", end.lastBlockSha256());
		AuditBlockChain chain = new AuditBlockChain();
		chain.accept(AuditMarkerCodec.sign(continuation, key), this.header, verifier);
		assertEquals(1, chain.lastValidatedLine());
		AuditBlockChain.checkContinuation(continuation, AuditMarkerCodec.sign(end, key), verifier);
		AuditMarker.Continue wrongFile = new AuditMarker.Continue(newIdentity, continuation.block(),
				"other.log", end.lastBlockSha256());
		assertThrows(IOException.class,
				() -> AuditBlockChain.checkContinuation(wrongFile, AuditMarkerCodec.sign(end, key), verifier));
		AuditMarker.Continue wrongRun = new AuditMarker.Continue(identity(key, "new.log", "run-456"),
				continuation.block(), "old.log", end.lastBlockSha256());
		assertThrows(IOException.class,
				() -> AuditBlockChain.checkContinuation(wrongRun, AuditMarkerCodec.sign(end, key), verifier));
	}

	private AuditMarker.First first(AuditMarker.Identity identity) {
		return new AuditMarker.First(identity, AuditMarker.Block.of(1, 1, this.header));
	}

	private AuditMarker.Seal seal(AuditMarker.Identity identity, String previousHash) {
		return new AuditMarker.Seal(identity, AuditMarker.Block.of(2, 3, this.records), previousHash);
	}

	private static AuditMarker.Identity identity(AuditSigningKey key, String filename, String run) {
		return new AuditMarker.Identity("A", run, filename, Instant.parse("2026-10-08T11:12:00Z"),
				"69.0.0", key.fingerprint());
	}
}
