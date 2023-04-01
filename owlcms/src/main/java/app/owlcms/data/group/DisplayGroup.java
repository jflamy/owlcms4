package app.owlcms.data.group;

import app.owlcms.data.platform.Platform;

public class DisplayGroup {
	private String competitionShortDateTime = "";

	private String description = "";

	private String name = "?";

	private String platform = "";

	private String weighInShortDateTime = "";

	public DisplayGroup(String name2, String description2, Platform platform2, String weighInShortDateTime2,
	        String competitionShortDateTime2) {
		this.name = name2;
		this.description = description2;
		this.platform = platform2 != null ? platform2.getName() : "";
		this.weighInShortDateTime = weighInShortDateTime2;
		this.competitionShortDateTime = competitionShortDateTime2;
	}

	public String getCompetitionShortDateTime() {
		return competitionShortDateTime;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public String getPlatform() {
		return platform;
	}

	public String getWeighInShortDateTime() {
		return weighInShortDateTime;
	}

	public void setCompetitionShortDateTime(String competitionShortDateTime) {
		this.competitionShortDateTime = competitionShortDateTime;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public void setWeighInShortDateTime(String weighInShortDateTime) {
		this.weighInShortDateTime = weighInShortDateTime;
	}
}
