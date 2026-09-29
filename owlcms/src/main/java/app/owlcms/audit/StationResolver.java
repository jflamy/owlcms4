package app.owlcms.audit;

import java.lang.reflect.Method;
import java.util.Map;

public final class StationResolver {
	private static final Map<String, String> STATIONS = Map.ofEntries(
			Map.entry("AdminView", "ADMIN"),
			Map.entry("AnnouncerContent", "ANNOUNCER"),
			Map.entry("MarshallContent", "MARSHAL"),
			Map.entry("TimekeeperContent", "TIMEKEEPER"),
			Map.entry("WodkeeperContent", "TIMEKEEPER"),
			Map.entry("TCContent", "TC"),
			Map.entry("JuryContent", "JURY_CONSOLE"),
			Map.entry("JuryKeypadContent", "JURY_MEMBER"),
			Map.entry("JuryMobileContent", "JURY_MEMBER"),
			Map.entry("RefContent", "REFEREE"),
			Map.entry("MedalCeremonyContent", "MEDALS"),
			Map.entry("TestingContent", "TESTING"),
			Map.entry("WeighinContent", "WEIGHIN"),
			Map.entry("RegistrationContent", "REGISTRATION"),
			Map.entry("CoachContent", "REGISTRATION"),
			Map.entry("TeamSelectionContent", "REGISTRATION"),
			Map.entry("SessionResultsContent", "RESULTS"),
			Map.entry("TeamResultsContent", "RESULTS"),
			Map.entry("PackageContent", "RESULTS"),
			Map.entry("ProxyAthleteTimer", "SYSTEM"),
			Map.entry("ProxyBreakTimer", "SYSTEM"),
			Map.entry("FieldOfPlay", "SYSTEM"),
			Map.entry("FOPSimulator", "SIMULATION"),
			Map.entry("CompetitionSimulator", "SIMULATION"));

	private StationResolver() {
	}

	public static String resolve(Object origin) {
		if (origin == null) {
			return "SYSTEM";
		}
		Object resolved = unwrap(origin);
		String name = resolved.getClass().getSimpleName();
		String station = STATIONS.get(name);
		if (station != null) {
			return station;
		}
		return resolved.getClass().getPackageName().contains(".nui.preparation") ? "PREPARATION" : "UNKNOWN";
	}

	private static Object unwrap(Object origin) {
		Object current = origin;
		for (int depth = 0; depth < 5; depth++) {
			try {
				Method method = current.getClass().getMethod("getOrigin");
				Object next = method.invoke(current);
				if (next == null || next == current) {
					return current;
				}
				current = next;
			} catch (ReflectiveOperationException e) {
				return current;
			}
		}
		return current;
	}
}