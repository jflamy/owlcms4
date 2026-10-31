/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import app.owlcms.data.competition.Competition;

public enum PreCompetitionTemplate {
	BY_CATEGORY("/templates/categories", ".xlsx",
	        () -> Competition.getCurrent().getCategoriesListTemplateFileName(),
	        Competition::setCategoriesListTemplateFileName),
	BY_BODYWEIGHT("/templates/bwStart", ".xlsx",
	        () -> Competition.getCurrent().getBodyWeightListTemplateFileName(),
	        Competition::setBodyWeightListTemplateFileName),
	BY_TEAM("/templates/teams", ".xlsx",
	        () -> Competition.getCurrent().getTeamsListTemplateFileName(),
	        Competition::setTeamsListTemplateFileName),

	START_LIST("/templates/start", ".xlsx",
	        () -> Competition.getCurrent().getStartListTemplateFileName(),
	        Competition::setStartListTemplateFileName),
	SCHEDULE("/templates/schedule", ".xlsx",
	        () -> Competition.getCurrent().getScheduleTemplateFileName(),
	        Competition::setScheduleTemplateFileName),
	OFFICIALS("/templates/officials", ".xlsx",
	        () -> Competition.getCurrent().getOfficialsListTemplateFileName(),
	        Competition::setOfficialsListTemplateFileName),
	CHECKIN("/templates/checkin", ".xlsx",
	        () -> Competition.getCurrent().getCheckInTemplateFileName(),
	        Competition::setCheckInTemplateFileName),

	CARDS("/templates/cards", ".xlsx",
	        () -> Competition.getCurrent().getCardsTemplateFileName(),
	        Competition::setCardsTemplateFileName),
	ATHLETE_CREDENTIALS("/templates/credentials", ".xlsx",
	        () -> Competition.getCurrent().getAthleteCredentialsTemplateFileName(),
	        Competition::setAthleteCredentialsTemplateFileName),
	TO_CREDENTIALS("/templates/credentials", ".xlsx",
	        () -> Competition.getCurrent().getToCredentialsTemplateFileName(),
	        Competition::setToCredentialsTemplateFileName),
	COACH_CREDENTIALS("/templates/credentials", ".xlsx",
	        () -> Competition.getCurrent().getCoachCredentialsTemplateFileName(),
	        Competition::setCoachCredentialsTemplateFileName),
	WEIGHIN("/templates/weighin", ".xlsx",
	        () -> Competition.getCurrent().getWeighInFormTemplateFileName(),
	        Competition::setWeighInFormTemplateFileName),

	INTRODUCTION("/templates/introduction", ".xlsx",
	        () -> Competition.getCurrent().getIntroductionTemplateFileName(),
	        Competition::setIntroductionTemplateFileName),
	EMPTY_PROTOCOL("/templates/emptyProtocol", ".xlsx",
	        () -> Competition.getCurrent().getEmptyProtocolTemplateFileName(),
	        Competition::setEmptyProtocolTemplateFileName),
	JURY("/templates/jury", ".xlsx",
	        () -> Competition.getCurrent().getJuryTemplateFileName(),
	        Competition::setJuryTemplateFileName),
	RECORDS("/templates/records", ".xlsx",
	        () -> Competition.getCurrent().getCurrentRecordsTemplateFileName(),
	        Competition::setCurrentRecordsTemplateFileName),
	PRE_WEIGHIN("", ".zip", null, null),
	POST_WEIGHIN("", ".zip", null, null),
	;

	String folder;
	String extension;
	Supplier<String> templateFileNameSupplier;
	/** Applied to the stored competition row by {@code CompetitionRepository.apply}. */
	BiConsumer<Competition, String> templateFileNameSetter;

	PreCompetitionTemplate(String folder, String extension, Supplier<String> templateFileNameSupplier,
	        BiConsumer<Competition, String> templateFileNameSetter) {
		this.folder = folder;
		this.extension = extension;
		this.templateFileNameSupplier = templateFileNameSupplier;
		this.templateFileNameSetter = templateFileNameSetter;
	}

}