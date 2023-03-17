/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSRegistrationExport extends JXLSWorkbookStreamSource {

	private static final String TEMPLATES_REGISTRATION_REGISTRATION_EXPORT = "/templates/registration/RegistrationExport";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSRegistrationExport.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	public JXLSRegistrationExport(UI ui) {
		super();
		try {
			getLocalizedTemplate(TEMPLATES_REGISTRATION_REGISTRATION_EXPORT, ".xls", OwlcmsSession.getLocale());
		} catch (IOException e) {
		}
	}

	@Override
	public HashMap<String, Object> getReportingBeans() {
		HashMap<String, Object> beans = super.getReportingBeans();
		// the purpose of allocating groups, better to sort by platform first.
		beans.put("groups", GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			return compare;
		}).collect(Collectors.toList()));
		return beans;
	}

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		return getLocalizedTemplate(TEMPLATES_REGISTRATION_REGISTRATION_EXPORT, ".xls", locale);
	}

	@Override
	protected List<Athlete> getSortedAthletes() {
		List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null, null);
		return AthleteSorter
		        .registrationExportCopy(athletes);
	}

}
