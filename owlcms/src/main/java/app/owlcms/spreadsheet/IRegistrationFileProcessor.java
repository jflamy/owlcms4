package app.owlcms.spreadsheet;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public interface IRegistrationFileProcessor {

	void adjustParticipations();

	String cleanMessage(String localizedMessage);

	// void appendErrors(Runnable displayUpdater, Consumer<String> errorAppender, XLSReadStatus status);

	int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater);

	int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater);

	void resetAthletes();

	void resetGroups();

	void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> athletes);

	void updatePlatformsAndGroups(List<RGroup> groups);

}