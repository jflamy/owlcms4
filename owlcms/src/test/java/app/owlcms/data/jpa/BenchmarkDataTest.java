package app.owlcms.data.jpa;

import org.junit.Test;

import app.owlcms.data.athlete.Gender;

public class BenchmarkDataTest {

	@Test
	public void test() {
		sessionsVsPlatforms(4);
	}

	String[][] bwcats = {
	        { "45", "49", "55", "59", "64", "71", "76", "81", "87", "+87" },
	        { "55", "61", "67", "73", "81", "89", "96", "102", "109", "+109" } };
	String[][] ageGroups = {
	        { "YTH", "JR", "SR", "W35", "W40", "W45", "W50", "W55", "W60", "W65", "W70", "W75", "W80", "W85" },
	        { "YTH", "JR", "SR", "M35", "M40", "W45", "W50", "M55", "M60", "M65", "M70", "M75", "M80", "M85" } };

	private void sessionsVsPlatforms(int nbPlatforms) {
		int sessionCount = 0;

		for (char groupName = 'D'; groupName >= 'A'; groupName--) {
			for (int bwCatIndex = 0; bwCatIndex < 10; bwCatIndex++) {
				for (int genderIndex = 0; genderIndex < 2; genderIndex++) {
					Gender g = Gender.values()[genderIndex];

					int platformIndex = sessionCount % nbPlatforms;
					String sessionName = "P"+(platformIndex+1) + "-" + g.name()
					        + bwcats[genderIndex][bwCatIndex]
					        + groupName;
					sessionCount++;

					// probability of being in A B C D group varies with age group, we ignore this.
					// we could do something more sophisticated like.
					// group D and C = 3 YTH + one each masters
					// group B is 50% JR and 50% SR
					// group A is 75% SR and 25% JR
					System.out.println(sessionName);
					for (int ageGroupIndex = 0; ageGroupIndex < ageGroups[genderIndex].length; ageGroupIndex++) {
						System.out.println("   " + ageGroups[genderIndex][ageGroupIndex]);
					}
				}
			}
		}
		int nbSess = sessionCount+1;
		System.out.println("sessions: "+ nbSess+" athletes: "+ nbSess*ageGroups[0].length);
	}

}
