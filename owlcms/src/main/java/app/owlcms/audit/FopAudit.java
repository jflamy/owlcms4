package app.owlcms.audit;

import java.util.StringJoiner;
import java.util.List;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;

public final class FopAudit {
	private FopAudit() {
	}

	public static void write(FieldOfPlay fop, FOPEvent event, boolean refused, String state, String clockBefore) {
		String action = action(event);
		if (action == null) {
			return;
		}
		Athlete athlete = event.getAthlete() != null ? event.getAthlete() : fop.getCurAthlete();
		String platform = fop.getName();
		String detail = detail(fop, event);
		if (refused) {
			action += ".refused";
			detail = join(detail, AuditFormat.kv("state", state));
		}
		AuditEntry.Builder builder = AuditEntry.builder(platform, action)
				.actor(actor(event))
				.athlete(athlete)
				.attempt(attempt(athlete))
				.cause(AuditContext.cause())
				.detail(detail);
		if (event instanceof FOPEvent.ForceTime forceTime) {
			builder.field("clock").oldValue(clockBefore).newValue(formatClock(forceTime.timeAllowed));
		}
		AuditLog.writeBatch(List.of(builder.build()), !refused && event instanceof FOPEvent.JuryDecision);
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
		if (event instanceof FOPEvent.JuryDecision) {
			return actor.atStation("JURY_CONSOLE", null, actor.inferred());
		}
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
			case "SummonReferee" -> "jury.summon";
			case "BarbellOrPlatesChanged" -> "tc.equipment";
			default -> null;
		};
	}

	private static String detail(FieldOfPlay fop, FOPEvent event) {
		StringJoiner detail = new StringJoiner(",");
		if (event instanceof FOPEvent.TimeStarted || event instanceof FOPEvent.TimeStopped
				|| event instanceof FOPEvent.TimeOver || event instanceof FOPEvent.StartLifting) {
			detail.add(AuditFormat.kv("clock", clock(fop)));
		}
		if (event instanceof FOPEvent.BreakStarted breakStarted) {
			detail.add(AuditFormat.kvs("type", breakStarted.getBreakType(), "countdown", breakStarted.getCountdownType(),
					"remaining", formatClock(breakStarted.getTimeRemaining()), "target", breakStarted.getTargetTime()));
		} else if (event instanceof FOPEvent.BreakPaused breakPaused) {
			detail.add(AuditFormat.kv("remaining", formatClock(breakPaused.getTimeRemaining())));
		} else if (event instanceof FOPEvent.BreakDone breakDone) {
			detail.add(AuditFormat.kv("type", breakDone.getBreakType()));
		} else if (event instanceof FOPEvent.DecisionUpdate decision) {
			detail.add(AuditFormat.kv("decision", lift(decision.isDecision())));
		} else if (event instanceof FOPEvent.DecisionFullUpdate decision) {
			detail.add(AuditFormat.kv("lights", decision.ref1 + "," + decision.ref2 + "," + decision.ref3));
		} else if (event instanceof FOPEvent.ExplicitDecision decision) {
			detail.add(AuditFormat.kvs("result", lift(Boolean.TRUE.equals(decision.success)),
					"lights", decision.ref1 + "," + decision.ref2 + "," + decision.ref3));
		} else if (event instanceof FOPEvent.JuryMemberDecisionUpdate decision) {
			detail.add(AuditFormat.kv("decision", lift(decision.decision)));
		} else if (event instanceof FOPEvent.JuryDecision decision) {
			detail.add(AuditFormat.kvs("decision", lift(Boolean.TRUE.equals(decision.success)),
					"reason", decision.getReasonCode()));
		} else if (event instanceof FOPEvent.SummonReferee summon) {
			detail.add(AuditFormat.kv("referee", summon.getRefNumber()));
		} else if (event instanceof FOPEvent.SwitchGroup switchGroup) {
			detail.add(AuditFormat.kv("session", switchGroup.getGroup() != null ? switchGroup.getGroup().getName() : null));
		} else if (event instanceof FOPEvent.CeremonyDone ceremony) {
			detail.add(AuditFormat.kv("type", ceremony.getCeremonyType()));
		}
		return detail.length() == 0 ? null : detail.toString();
	}

	public static String clock(FieldOfPlay fop) {
		if (fop.getAthleteTimer() == null) {
			return null;
		}
		return formatClock(fop.getAthleteTimer().liveTimeRemaining());
	}

	public static String formatClock(Integer millis) {
		return millis == null ? "indefinite" : formatClock(millis.intValue());
	}

	public static String formatClock(int millis) {
		int tenths = Math.max(0, millis) / 100;
		return String.format("%d:%02d.%d", tenths / 600, (tenths / 10) % 60, tenths % 10);
	}

	private static String lift(boolean good) {
		return good ? "GOOD LIFT" : "NO LIFT";
	}

	private static String attempt(Athlete athlete) {
		if (athlete == null) {
			return null;
		}
		int attempt = Math.min(athlete.getAttemptsDone() + 1, 6);
		return attempt <= 3 ? "SN" + attempt : "CJ" + (attempt - 3);
	}

	private static String join(String first, String second) {
		return first == null || first.isBlank() ? second : first + "," + second;
	}
}