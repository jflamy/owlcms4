/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.StartNumberOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSCategoriesListDocs extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSCategoriesListDocs.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	public JXLSCategoriesListDocs() {
		this.setExcludeNotWeighed(false);
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		if (getSortedAthletes()!= null) {
			if (getGroup() != null) {
				logger.debug("*** prefetched athletes for group={}", getGroup());
				var athletes = getSortedAthletes();
				athletes.sort(new StartNumberOrderComparator());
				return athletes;
			} else {
				logger.debug("*** prefetched athletes no group");
				return getSortedAthletes();
			}
		}
		logger.debug("*** Computing sorted athletes for categories list sheet, group={}", getGroup());
		if (getGroup() != null) {
			List<Athlete> displayOrderCopy = AthleteSorter
			        .displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(getGroup(), null));
			return displayOrderCopy;
		} else {
			List<Athlete> registrationOrderCopy = AthleteSorter
			        .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, null));
			return registrationOrderCopy;
		}
	}
}
