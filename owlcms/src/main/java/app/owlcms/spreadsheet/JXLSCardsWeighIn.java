/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;

@SuppressWarnings("serial")
public class JXLSCardsWeighIn extends JXLSCardsDocs {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(JXLSCardsWeighIn.class);

	public JXLSCardsWeighIn() {
		super();
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		if (getGroup() != null) {
			List<Athlete> registrationOrderCopy = AthleteSorter
					.registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(getGroup(), null));
			return registrationOrderCopy;
		} else {
			List<Athlete> registrationOrderCopy = AthleteSorter
					.registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, null));
			return registrationOrderCopy;
		}
	}


}
