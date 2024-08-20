package app.owlcms.nui.preparation;

import java.util.function.Consumer;
import java.util.function.Supplier;

import app.owlcms.data.competition.Competition;

public enum PreCompetitionTemplates {
	BY_CATEGORY("/templates/categories", ".xlsx", 
			() -> Competition.getCurrent().getCategoriesListTemplateFileName(),
	        (v) -> Competition.getCurrent().setCategoriesListTemplateFileName(v)),
	BY_BODYWEIGHT("/templates/bwStart", ".xlsx", 
			() -> Competition.getCurrent().getBodyWeightListTemplateFileName(),
	        (v) -> Competition.getCurrent().setBodyWeightListTemplateFileName(v)),
	BY_TEAM("/templates/teams", ".xlsx", 
			() -> Competition.getCurrent().getTeamsListTemplateFileName(),
	        (v) -> Competition.getCurrent().setTeamsListTemplateFileName(v)),
	
	START_LIST("/templates/start", ".xlsx", 
			() -> Competition.getCurrent().getStartListTemplateFileName(),
	        (v) -> Competition.getCurrent().setStartListTemplateFileName(v)),
	SCHEDULE("/templates/schedule", ".xlsx", 
			() -> Competition.getCurrent().getScheduleTemplateFileName(),
	        (v) -> Competition.getCurrent().setScheduleTemplateFileName(v)),
	OFFICIALS("/templates/officials", ".xlsx", 
			() -> Competition.getCurrent().getOfficialsListTemplateFileName(),
	        (v) -> Competition.getCurrent().setOfficialsListTemplateFileName(v)),
	CHECKIN("/templates/checkin", ".xlsx", 
			() -> Competition.getCurrent().getCheckInTemplateFileName(),
	        (v) -> Competition.getCurrent().setCheckInTemplateFileName(v)),

	CARDS("/templates/cards", ".xlsx", 
			() -> Competition.getCurrent().getCardsTemplateFileName(),
	        (v) -> Competition.getCurrent().setCardsTemplateFileName(v)),
	WEIGHIN("/templates/weighin", ".xlsx", 
			() -> Competition.getCurrent().getWeighInFormTemplateFileName(),
	        (v) -> Competition.getCurrent().setWeighInFormTemplateFileName(v)),

	INTRODUCTION("/templates/introduction", ".xlsx", 
			() -> Competition.getCurrent().getIntroductionTemplateFileName(),
	        (v) -> Competition.getCurrent().setIntroductionTemplateFileName(v)),
	EMPTY_PROTOCOL("/templates/emptyProtocol", ".xlsx",
			() -> Competition.getCurrent().getEmptyProtocolTemplateFileName(),
	        (v) -> Competition.getCurrent().setEmptyProtocolTemplateFileName(v)),
	JURY("/templates/jury", ".xlsx", 
			() -> Competition.getCurrent().getJuryTemplateFileName(),
	        (v) -> Competition.getCurrent().setJuryTemplateFileName(v)),
	PRE_WEIGHIN("", ".zip", null, null),
	POST_WEIGHIN("", ".zip", null, null), 
	;
	

	String folder;
	String extension;
	Supplier<String> templateFileNameSupplier;
	Consumer<String> templateFileNameSetter;

	PreCompetitionTemplates(String folder, String extension, Supplier<String> templateFileNameSupplier, Consumer<String> templateFileNameSetter) {
		this.folder = folder;
		this.extension = extension;
		this.templateFileNameSupplier = templateFileNameSupplier;
		this.templateFileNameSetter = templateFileNameSetter;
	}

}