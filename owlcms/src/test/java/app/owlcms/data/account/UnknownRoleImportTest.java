package app.owlcms.data.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import app.owlcms.access.Role;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** An export from a newer version may grant a role this version does not know: the import keeps the rest. */
public class UnknownRoleImportTest {

	private static final JsonMapper MAPPER = JsonMapper.builder()
	        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	        .build();

	@Test
	public void anUnknownRoleReadsAsNullAndOnlyThatGrantIsDropped() {
		UserAccount account = MAPPER.readValue("""
		        {"username":"streamer","grants":[
		          {"role":"ROLE_FROM_A_NEWER_VERSION","platformName":null},
		          {"role":"RESULTS","platformName":null},
		          {"role":"DISPLAYS","platformName":"A"}]}
		        """, UserAccount.class);
		assertNull(account.getGrants().get(0).getRole());

		UserAccountRepository.dropUnknownRoles(account);

		assertEquals(List.of(new RoleGrant(Role.RESULTS, null), new RoleGrant(Role.DISPLAYS, "A")),
		        account.getGrants());
	}
}
