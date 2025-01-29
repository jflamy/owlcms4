/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public interface IRegistrationFileProcessor {

	void adjustParticipations();

	String cleanMessage(String localizedMessage);

	// void appendErrors(Runnable displayUpdater, Consumer<String> errorAppender, XLSReadStatus status);

	int doProcessAthletes(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater, boolean eraseAthletes);

	int doProcessGroups(InputStream inputStream, boolean dryRun, Consumer<String> errorConsumer,
	        Runnable displayUpdater);

	void resetAthletes();

	void resetGroups();

	void updateAthletes(Consumer<String> errorConsumer, RCompetition c, List<RAthlete> athletes);

	void updatePlatformsAndGroups(List<RGroup> groups);

	void doProcessCompetitionHeader(InputStream inputStream, Consumer<String> errorConsumer, Runnable displayUpdater);
}