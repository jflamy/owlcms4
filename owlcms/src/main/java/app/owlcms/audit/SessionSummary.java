package app.owlcms.audit;

import java.util.ArrayList;
import java.util.List;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.fieldofplay.FieldOfPlay;

public final class SessionSummary {
	private SessionSummary() {
	}

	public static void write(FieldOfPlay fop, AuditActor actor) {
		Group group = fop.getGroup();
		if (group == null) {
			return;
		}
		String platform = fop.getName();
		Competition competition = Competition.getCurrent();
		AuditLog.write(AuditEntry.builder(platform, "summary.session").actor(actor)
				.newValue(group.getName() + " " + group.getDescription())
				.detail(AuditFormat.kvs("competition", competition.getCompetitionName(),
						"date", competition.getLocalizedCompetitionDate(), "site", competition.getCompetitionSite(),
						"city", competition.getCompetitionCity(), "organizer", competition.getCompetitionOrganizer(),
						"weighIn", group.getWeighInTime(), "competitionTime", group.getCompetitionTime()))
				.build());

		for (OfficialPosition position : officials(group)) {
			TechnicalOfficial official = position.official();
			if (official != null) {
				AuditLog.write(AuditEntry.builder(platform, "summary.official").actor(actor).field(position.role())
						.newValue(official.getFullName() + " (" + official.getFederation() + ")").build());
			}
		}

		for (Athlete athlete : fop.getDisplayOrder()) {
			if (athlete.getCategory() == null || athlete.getBodyWeight() == null || athlete.getBodyWeight() <= 0.01) {
				continue;
			}
			String detail = AuditFormat.kvs("start", athlete.getStartNumber(), "lot", athlete.getLotNumber(),
					"birth", athlete.getFormattedBirth(), "membership", athlete.getMembership(), "team", athlete.getTeam(),
					"category", athlete.getDisplayCategory(), "bodyWeight", athlete.getBodyWeight(),
					"snatch1", athlete.getSnatch1Declaration(), "cleanJerk1", athlete.getCleanJerk1Declaration());
			AuditLog.write(AuditEntry.builder(platform, "summary.athlete").actor(actor).athlete(athlete)
					.detail(detail).build());
		}
	}

	private static List<OfficialPosition> officials(Group group) {
		List<OfficialPosition> result = new ArrayList<>();
		add(result, "announcer", group.getAnnouncerAsTO());
		add(result, "competitionDirector", group.getCompetitionDirectorAsTO());
		add(result, "competitionSecretary1", group.getCompetitionSecretaryAsTO());
		add(result, "competitionSecretary2", group.getCompetitionSecretary2AsTO());
		add(result, "juryPresident", group.getJury1AsTO());
		add(result, "jury2", group.getJury2AsTO());
		add(result, "jury3", group.getJury3AsTO());
		add(result, "jury4", group.getJury4AsTO());
		add(result, "jury5", group.getJury5AsTO());
		add(result, "reserveJury", group.getReserveJuryAsTO());
		add(result, "referee1", group.getReferee1AsTO());
		add(result, "referee2", group.getReferee2AsTO());
		add(result, "referee3", group.getReferee3AsTO());
		add(result, "reserveReferee", group.getReserveAsTO());
		add(result, "marshal1", group.getMarshallAsTO());
		add(result, "marshal2", group.getMarshal2AsTO());
		add(result, "technicalController1", group.getTechnicalControllerAsTO());
		add(result, "technicalController2", group.getTechnicalController2AsTO());
		add(result, "technicalController3", group.getTechnicalController3AsTO());
		add(result, "timekeeper", group.getTimeKeeperAsTO());
		add(result, "doctor1", group.getDoctorAsTO());
		add(result, "doctor2", group.getDoctor2AsTO());
		add(result, "doctor3", group.getDoctor3AsTO());
		add(result, "weighIn1", group.getWeighIn1AsTO());
		add(result, "weighIn2", group.getWeighIn2AsTO());
		add(result, "tis1", TechnicalOfficialRepository.safeFindByName(group.getTis1()));
		add(result, "tis2", TechnicalOfficialRepository.safeFindByName(group.getTis2()));
		return result;
	}

	private static void add(List<OfficialPosition> positions, String role, TechnicalOfficial official) {
		positions.add(new OfficialPosition(role, official));
	}

	private record OfficialPosition(String role, TechnicalOfficial official) {
	}
}