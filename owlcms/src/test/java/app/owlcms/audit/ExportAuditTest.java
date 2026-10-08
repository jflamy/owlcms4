package app.owlcms.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class ExportAuditTest {

	@Test
	public void successDetailCarriesFormatChannelLengthAndDigest() {
		String sha = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
		assertEquals("format=2,channel=download,bytes=2948571,sha256=" + sha,
				ExportAudit.successDetail(2, ExportAudit.CHANNEL_DOWNLOAD, 2948571L, sha));
	}

	@Test
	public void failureDetailHasNoDigest() {
		String detail = ExportAudit.failureDetail(1, ExportAudit.CHANNEL_HTTP, new IOException("Pipe closed"));
		assertEquals("format=1,channel=http,outcome=failed,reason=\"Pipe closed\"", detail);
		assertFalse(detail.contains("sha256"));
	}
}
