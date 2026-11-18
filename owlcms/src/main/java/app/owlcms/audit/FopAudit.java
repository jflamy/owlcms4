package app.owlcms.audit;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.uievents.BreakType;

public final class FopAudit {
	private FopAudit() {
	}

	public static void write(FieldOfPlay fop, FOPEvent event, boolean refused, String state, Integer clockBefore) {
		String action = action(event);
		if (action == null) {
			return;
		}
		boolean review = concernsReview(fop, event);
		Athlete athlete = event.getAthlete() != null ? event.getAthlete()
				: review && fop.getAthleteUnderReview() != null ? fop.getAthleteUnderReview() : fop.getCurAthlete();
		String platform = fop.getName();
		Map<String, Object> details = details(fop, event);
		if (refused) {
			action += ".refused";
			details.put("state", state);
		}
		AuditEntry.Builder builder = AuditEntry.builder(platform, action)
				.actor(actor(event))
				.athlete(athlete)
				.session(event instanceof FOPEvent.SwitchGroup switchGroup ? AuditEntry.sessionOf(switchGroup.getGroup())
						: session(fop, athlete))
				.attempt(review ? reviewedAttempt(athlete) : attempt(athlete))
				.cause(AuditContext.cause())
				.details(details);
		if (event instanceof FOPEvent.ForceTime forceTime) {
			builder.field("clock").oldValue(clockBefore).newValue(forceTime.timeAllowed);
		}
		AuditLog.writeBatch(List.of(builder.build()), !refused && event instanceof FOPEvent.JuryDecision);
	}

	/**
	 * A session loaded on the platform without a field-of-play event: a page opened with the session in its URL. The
	 * actor is captured on the page.
	 */
	public static void sessionLoaded(FieldOfPlay fop, AuditActor actor, String cause) {
		Athlete athlete = fop.getCurAthlete();
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("session", AuditEntry.sessionOf(fop.getGroup()));
		AuditLog.write(AuditEntry.builder(fop.getName(), "session.load")
				.actor(actor != null ? actor : AuditActor.system())
				.athlete(athlete)
				.session(AuditEntry.sessionOf(fop.getGroup()))
				.attempt(attempt(athlete))
				.cause(cause)
				.details(details)
				.build());
	}

	/** The athlete's session; for a record without an athlete in a session, the session on the platform. */
	private static String session(FieldOfPlay fop, Athlete athlete) {
		if (athlete != null && athlete.getGroup() != null) {
			return athlete.getGroup().getName();
		}
		return AuditEntry.sessionOf(fop.getGroup());
	}

	/**
	 * The result of a review vote, written by the field of play when the last vote is in. {@code vote} counts the
	 * votes completed on this lift: more than one when a result was cancelled and the vote taken again.
	 */
	public static void reviewResult(FieldOfPlay fop, Map<String, Object> details) {
		Athlete athlete = fop.getAthleteUnderReview() != null ? fop.getAthleteUnderReview() : fop.getCurAthlete();
		AuditLog.write(AuditEntry.builder(fop.getName(), "jury.result")
				.actor(AuditActor.system())
				.athlete(athlete)
				.session(session(fop, athlete))
				.attempt(reviewedAttempt(athlete))
				.cause(AuditContext.cause())
				.details(details)
				.build());
	}

	private static AuditActor actor(FOPEvent event) {
		AuditActor actor = event.getAuditActor() != null ? event.getAuditActor() : AuditActor.system();
		if (event instanceof FOPEvent.DecisionUpdate decision) {
			return actor.atStation("REFEREE", decision.getRefIndex() + 1, actor.inferred());
		}
		if (event instanceof FOPEvent.JuryMemberDecisionUpdate decision) {
			return actor.atStation("JURY_MEMBER", decision.refIndex + 1, actor.inferred());
		}
		if (event instanceof FOPEvent.DecisionFullUpdate) {
			return actor.atStation("REFEREES", null, false);
		}
		// a jury decision keeps the station that entered it: JURY_CONTROL, JURY_BOX or ANNOUNCER (the Speaker)
		return actor;
	}

	private static String action(FOPEvent event) {
		return switch (event.getClass().getSimpleName()) {
			case "TimeStarted" -> "clock.start";
			case "TimeStopped" -> "clock.stop";
			case "ForceTime" -> "clock.set";
			case "TimeOver" -> "clock.timeover";
			case "BreakStarted" -> "break.start";
			case "BreakPaused" -> "break.pause";
			case "BreakDone" -> "break.end";
			case "StartLifting" -> "lifting.start";
			case "SwitchGroup" -> "session.load";
			case "CeremonyStarted" -> "ceremony.start";
			case "CeremonyDone" -> "ceremony.end";
			case "DecisionUpdate" -> "referee.vote";
			case "DecisionFullUpdate", "ExplicitDecision" -> "referee.decision";
			case "DownSignal" -> "referee.down";
			case "JuryMemberDecisionUpdate" -> "jury.vote";
			case "JuryDecision" -> "jury.decision";
			case "JuryResume" -> "jury.resume";
			case "SummonReferee" -> "jury.summon";
			case "BarbellOrPlatesChanged" -> "tc.equipment";
			default -> null;
		};
	}

	private static Map<String, Object> details(FieldOfPlay fop, FOPEvent event) {
		Map<String, Object> details = new LinkedHashMap<>();
		if (event instanceof FOPEvent.TimeStarted || event instanceof FOPEvent.TimeStopped
				|| event instanceof FOPEvent.TimeOver || event instanceof FOPEvent.StartLifting) {
			details.put("clockMillis", clockMillis(fop));
		}
		if (event instanceof FOPEvent.BreakStarted breakStarted) {
			details.put("type", breakStarted.getBreakType());
			details.put("countdown", breakStarted.getCountdownType());
			details.put("remainingMillis", breakStarted.getTimeRemaining());
			details.put("target", breakStarted.getTargetTime());
		} else if (event instanceof FOPEvent.BreakPaused breakPaused) {
			details.put("remainingMillis", breakPaused.getTimeRemaining());
		} else if (event instanceof FOPEvent.BreakDone breakDone) {
			details.put("type", breakDone.getBreakType());
		} else if (event instanceof FOPEvent.DecisionUpdate decision) {
			details.put("decision", decision.isDecision());
		} else if (event instanceof FOPEvent.DecisionFullUpdate decision) {
			details.put("lights", Arrays.asList(decision.ref1, decision.ref2, decision.ref3));
		} else if (event instanceof FOPEvent.ExplicitDecision decision) {
			details.put("result", decision.success);
			details.put("lights", Arrays.asList(decision.ref1, decision.ref2, decision.ref3));
		} else if (event instanceof FOPEvent.JuryMemberDecisionUpdate decision) {
			details.put("decision", decision.decision);
		} else if (event instanceof FOPEvent.JuryDecision decision) {
			details.put("decision", decision.success);
			details.put("reason", decision.getReasonCode());
		} else if (event instanceof FOPEvent.SummonReferee summon) {
			details.put("referee", summon.getRefNumber());
		} else if (event instanceof FOPEvent.SwitchGroup switchGroup) {
			details.put("session", switchGroup.getGroup() != null ? switchGroup.getGroup().getName() : null);
		} else if (event instanceof FOPEvent.CeremonyDone ceremony) {
			details.put("type", ceremony.getCeremonyType());
		}
		return details;
	}

	public static Integer clockMillis(FieldOfPlay fop) {
		IProxyTimer timer = fop == null ? null : fop.getAthleteTimer();
		return timer == null ? null : timer.liveTimeRemaining();
	}

	public static String timeDetail(String key, Integer millis) {
		return AuditFormat.kv(key, formatClock(millis)) + (millis == null ? "" : " (" + millis + "ms)");
	}

	public static String formatClock(Integer millis) {
		return millis == null ? "indefinite" : formatClock(millis.intValue());
	}

	public static String formatClock(int millis) {
		long seconds = (Math.max(0L, millis) + 999) / 1000;
		long hours = seconds / 3600;
		long minutes = (seconds / 60) % 60;
		return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds % 60)
				: String.format("%d:%02d", minutes, seconds % 60);
	}

	/**
	 * Jury actions and jury or challenge breaks, the referees' votes during a review break, and a referee decision that
	 * committed the lift concern the lift under review, which is already counted as done: not the athlete's next
	 * attempt, and not necessarily the current athlete.
	 */
	private static boolean concernsReview(FieldOfPlay fop, FOPEvent event) {
		if (event instanceof FOPEvent.JuryDecision || event instanceof FOPEvent.JuryMemberDecisionUpdate
				|| event instanceof FOPEvent.SummonReferee || event instanceof FOPEvent.JuryResume) {
			return true;
		}
		if (event instanceof FOPEvent.BreakStarted breakStarted) {
			return isReviewBreak(breakStarted.getBreakType());
		}
		if (event instanceof FOPEvent.BreakDone breakDone) {
			return isReviewBreak(breakDone.getBreakType());
		}
		boolean decision = event instanceof FOPEvent.DecisionUpdate || event instanceof FOPEvent.DecisionFullUpdate
				|| event instanceof FOPEvent.ExplicitDecision;
		return decision && (fop.isLiftCommittedByCurrentEvent()
				|| (fop.getState() == FOPState.BREAK && fop.isPanelReviewing()));
	}

	private static boolean isReviewBreak(BreakType type) {
		return type == BreakType.JURY || type == BreakType.CHALLENGE;
	}

	private static String attempt(Athlete athlete) {
		return athlete == null ? null : label(Math.min(athlete.getAttemptsDone() + 1, 6));
	}

	private static String reviewedAttempt(Athlete athlete) {
		if (athlete == null) {
			return null;
		}
		int done = athlete.getAttemptsDone();
		return done < 1 ? attempt(athlete) : label(Math.min(done, 6));
	}

	private static String label(int attempt) {
		return attempt <= 3 ? "SN" + attempt : "CJ" + (attempt - 3);
	}

}