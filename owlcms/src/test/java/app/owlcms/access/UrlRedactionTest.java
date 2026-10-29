package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.owlcms.utils.URLUtils;

public class UrlRedactionTest {

	@Test
	public void tokenValuesAreRedactedWherever() {
		assertEquals("displays/publicScoreboard?fop=A&token=***&video=true",
		        URLUtils.redactToken("displays/publicScoreboard?fop=A&token=s3cr3t-Value&video=true"));
		assertEquals("displays/rankings?token=***", URLUtils.redactToken("displays/rankings?token=abc"));
		assertEquals("token=***&fop=A", URLUtils.redactToken("token=abc&fop=A"));
		assertEquals("https://host/x?token=***&token=***", URLUtils.redactToken("https://host/x?token=a&token=b"));
		assertEquals("https://host/x?token=***#frag", URLUtils.redactToken("https://host/x?token=a#frag"));
	}

	@Test
	public void otherParametersAreUnchanged() {
		assertEquals("displays/x?fop=A&mytoken=keep", URLUtils.redactToken("displays/x?fop=A&mytoken=keep"));
		assertEquals("displays/x?fop=A", URLUtils.redactToken("displays/x?fop=A"));
		assertNull(URLUtils.redactToken(null));
	}
}
