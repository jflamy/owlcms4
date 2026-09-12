package app.owlcms.displays.attemptboard;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import elemental.json.JsonObject;

final class AttemptBoardRenderCheck {
	private static final String JAVASCRIPT_WHITESPACE = "[\\p{Zs}\\x{0009}-\\x{000D}\\x{2028}\\x{2029}\\x{FEFF}]";
	private static final Pattern EDGE_WHITESPACE = Pattern.compile(
	        "\\A" + JAVASCRIPT_WHITESPACE + "+|" + JAVASCRIPT_WHITESPACE + "+\\z");

	private record Expected(String weight, String renderedWeight, String startNumber, String mode, boolean visible) {}

	private final NavigableMap<Long, Expected> pending = new TreeMap<>();
	private long waitingSince;

	synchronized void published(AttemptBoardState state, String kgSymbol, long now) {
		if (this.pending.isEmpty()) {
			this.waitingSince = now;
		}
		JsonObject json = state.toJson();
		String mode = json.getString("mode");
		boolean visible = "CURRENT_ATHLETE".equals(mode) || "LIFT_COUNTDOWN".equals(mode)
		        || ("INTERRUPTION".equals(mode) && "TECHNICAL".equals(json.getString("breakType")));
		this.pending.put(state.getSequence(), new Expected(json.getString("weight"),
		        EDGE_WHITESPACE.matcher(json.getString("weight") + kgSymbol).replaceAll(""),
		        Integer.toString((int) json.getNumber("startNumber")), mode, visible));
		while (this.pending.size() > 128) {
			this.pending.pollFirstEntry();
		}
	}

	synchronized String rendered(String sequence, String weight, String startNumber, String renderedWeight,
	        String mode, boolean visible, long now) {
		long acknowledged;
		try {
			acknowledged = Long.parseLong(sequence);
		} catch (NumberFormatException exception) {
			return "invalid sequence=" + sequence;
		}
		Expected expected = this.pending.get(acknowledged);
		if (expected == null) {
			return null;
		}
		this.pending.headMap(acknowledged, true).clear();
		this.waitingSince = now;
		if (!expected.weight().equals(weight) || !expected.renderedWeight().equals(renderedWeight)
		        || !expected.startNumber().equals(startNumber) || !expected.mode().equals(mode)
		        || expected.visible() != visible) {
			return "seq=" + acknowledged + " expected=" + expected + " received=[weight=" + weight
			        + ", rendered=" + renderedWeight + ", startNumber=" + startNumber + ", mode=" + mode
			        + ", visible=" + visible + "]";
		}
		return null;
	}

	synchronized String timeout(long now) {
		if (this.pending.isEmpty() || now - this.waitingSince < 5000) {
			return null;
		}
		String failure = "no render acknowledgement progress for 5000ms; latest seq=" + this.pending.lastKey()
		        + " expected=" + this.pending.lastEntry().getValue();
		this.pending.clear();
		return failure;
	}

	synchronized void clear() {
		this.pending.clear();
	}
}