/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSWeighInSheet extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWeighInSheet.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	public JXLSWeighInSheet() {
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		final Group currentGroup = getGroup();
		String computedStartingWeightsSheetTemplateFileName = Competition.getCurrent()
		        .getWeighInFormTemplateFileName();
		if (computedStartingWeightsSheetTemplateFileName.contains("Weigh")) {
			List<Athlete> collect = AthleteSorter
			        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup, null)).stream()
			        .map(a -> {
				        if (a.getTeam() == null) {
					        a.setTeam("");
				        }
				        return a;
			        }).collect(Collectors.toList());
			// logger.debug("sorted by category {}", collect);
			return collect;
		}
		if (currentGroup != null) {
			return AthleteSorter
			        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup, isExcludeNotWeighed()));
		} else {
			return AthleteSorter
			        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed()));
		}

	}

}
