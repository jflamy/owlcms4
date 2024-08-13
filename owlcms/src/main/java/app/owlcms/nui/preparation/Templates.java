package app.owlcms.nui.preparation;

public enum Templates {
	START_LIST("/templates/start"),
	SCHEDULE("/templates/schedule"),
	OFFICIALS("/templates/officials"),
	CHECKIN("/templates/checkin"),
	
	CARDS("/templates/cards"),
	WEIGHIN("/templates/weighin"),

	INTRODUCTION("/templates/introduction"),
	EMPTY_PROTOCOL("/templates/emptyProtocol"),
	JURY("/templates/jury")
	;

	String folder;

	Templates(String folder) {
		this.folder = folder;
	}
	
}