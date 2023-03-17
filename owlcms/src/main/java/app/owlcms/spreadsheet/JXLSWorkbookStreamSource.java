/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a
 * source of data when the user clicks on a link. This class converts the output
 * stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLSWorkbookStreamSource implements StreamResourceWriter {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWorkbookStreamSource.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	protected List<Athlete> sortedAthletes;

	private AgeDivision ageDivision;

	private String ageGroupPrefix;
	private Category category;
	private boolean excludeNotWeighed;
	private Group group;
	private InputStream inputStream;
	private HashMap<String, Object> reportingBeans;
	private String templateFileName;
	private UI ui;

	public JXLSWorkbookStreamSource() {
		this.ui = UI.getCurrent();
		this.setExcludeNotWeighed(true);
		init();
	}

	/**
	 * Read the Excel template and write the processed file out.
	 * The type of Excel file matches the template read (xls or xlsx)
	 *
	 * @see com.vaadin.flow.server.StreamResourceWriter#accept(java.io.OutputStream,
	 *      com.vaadin.flow.server.VaadinSession)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void accept(OutputStream stream, VaadinSession session) throws IOException {
		try {
			session.lock();
			Locale locale = OwlcmsSession.getLocale();
			XLSTransformer transformer = new XLSTransformer();
			configureTransformer(transformer);
			Workbook workbook = null;
			try {
				setReportingInfo();
				HashMap<String, Object> reportingInfo = getReportingBeans();
				List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
				if (athletes != null && (athletes.size() > 0 || isEmptyOk())) {
					workbook = transformer.transformXLS(getTemplate(locale), reportingInfo);
					if (workbook != null) {
						postProcess(workbook);
					}
				} else {
					String noAthletes = Translator.translate("NoAthletes");
					logger./**/warn("no athletes: empty report.");
					ui.access(() -> {
						Notification notif = new Notification();
						notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
						notif.setPosition(Position.TOP_STRETCH);
						notif.setDuration(3000);
						notif.setText(noAthletes);
						notif.open();
					});
					workbook = new HSSFWorkbook();
					workbook.createSheet().createRow(1).createCell(1).setCellValue(noAthletes);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			if (workbook != null) {
				//logger.debug("workbook type {}", workbook.getClass().getSimpleName());
				workbook.write(stream);
			}
		} catch (IOException e) {
			// ignore
		} catch (Throwable t) {
			logger.error(LoggerUtils./**/stackTrace(t));
		} finally {
			session.unlock();
		}
	}

	/**
	 * @return the ageDivision
	 */
	public AgeDivision getAgeDivision() {
		return ageDivision;
	}

	/**
	 * @return the ageGroupPrefix
	 */
	public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	public Category getCategory() {
		return category;
	}

	public Group getGroup() {
		return group;
	}

	public HashMap<String, Object> getReportingBeans() {
		return reportingBeans;
	}

	public List<String> getSuffixes(Locale locale) {
		List<String> tryList = new ArrayList<>();
		if (!locale.getVariant().isEmpty() && !locale.getCountry().isEmpty() && !locale.getLanguage().isEmpty()) {
			tryList.add("_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant());
		}
		if (!locale.getCountry().isEmpty() && !locale.getLanguage().isEmpty()) {
			tryList.add("_" + locale.getLanguage() + "_" + locale.getCountry());
		}
		if (!locale.getLanguage().isEmpty()) {
			tryList.add("_" + locale.getLanguage());
		}
		// try English explicitly for backward compatibility
		if (!locale.getLanguage().equals("en")) {
			tryList.add("_" + "en");
		}
		tryList.add("");
		return tryList;
	}

	public String getTemplateFileName() {
		return templateFileName;
	}

	public boolean isExcludeNotWeighed() {
		return excludeNotWeighed;
	}

	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	/**
	 * @param ageGroupPrefix the ageGroupPrefix to set
	 */
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public void setExcludeNotWeighed(boolean excludeNotWeighed) {
		this.excludeNotWeighed = excludeNotWeighed;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public void setInputStream(InputStream is) {
		this.inputStream = is;
	}

	public void setReportingBeans(HashMap<String, Object> jXLSBeans) {
		this.reportingBeans = jXLSBeans;
	}

	public void setSortedAthletes(List<Athlete> sortedAthletes) {
		this.sortedAthletes = sortedAthletes;
	}

	/**
	 * Attempt to erase a pair of adjoining cells.
	 *
	 * @param workbook
	 * @param rownum
	 * @param cellnum
	 */
	public void zapCellPair(Workbook workbook, int rownum, int cellnum) {
		Row row = workbook.getSheetAt(0).getRow(rownum);
		final Cell cellLeft = row.getCell(cellnum);
		if (cellLeft == null) {
			return;
		}

		cellLeft.setCellValue("");

		Cell cellRight = row.getCell(cellnum + 1);
		if (cellRight == null) {
			return;
		}

		cellRight.setCellValue("");

		CellStyle blank = workbook.createCellStyle();
		blank.setBorderBottom(BorderStyle.NONE);
		cellLeft.setCellStyle(blank);
		cellRight.setCellStyle(blank);
	}

	protected void configureTransformer(XLSTransformer transformer) {
		// do nothing, to be overridden as needed,
	}

	/**
	 * Try the possible variations of a template based on locale. For
	 * "/templates/start/startList", ".xls", and a locale of fr_CA, the following
	 * names will be tried /templates/start/startList_fr_CA.xls
	 * /templates/start/startList_fr.xls /templates/start/startList_en.xls
	 *
	 * @param templateName
	 * @param extension
	 * @param locale
	 * @return
	 * @throws IOException
	 */
	protected InputStream getLocalizedTemplate(String templateName, String extension, Locale locale)
	        throws IOException {
		List<String> tryList = getSuffixes(locale);
		List<String> extensionList;
		if (extension.startsWith(".xls")) {
			extensionList = Arrays.asList(
					".xlsx",
					".xls");
		} else {
			extensionList = Arrays.asList(extension);
		}

		for (String ext : extensionList) {
			for (String suffix : tryList) {
				String name = templateName + suffix + ext;
				try {
					final InputStream resourceAsStream = ResourceWalker.getFileOrResource(name);
					this.setTemplateFileName(name);
					if (resourceAsStream != null) {
						return resourceAsStream;
					}
				} catch (FileNotFoundException e) {
					// ignore
				}
			}
		}
		throw new IOException("no template found for : " + templateName + extension + " tried with suffix " + tryList);
	}

	protected List<Athlete> getSortedAthletes() {
		return sortedAthletes;
	}

	protected InputStream getTemplate(Locale locale) throws IOException, Exception {
		if (getInputStream() != null) {
			//logger.debug("explicitly set template {}", getInputStream());
			return getInputStream();
		}
		InputStream resourceAsStream = ResourceWalker.getFileOrResource(getTemplateFileName());
		return resourceAsStream;
	}

	protected void init() {
		setReportingBeans(new HashMap<String, Object>());
	}

	protected boolean isEmptyOk() {
		return false;
	}

	protected void postProcess(Workbook workbook) {
		// do nothing, to be overridden as needed,
	}

	/**
	 * Return athletes as required by the template.
	 */
	protected void setReportingInfo() {
		List<Athlete> athletes = getSortedAthletes();
		if (athletes != null) {
			getReportingBeans().put("athletes", athletes);
			getReportingBeans().put("lifters", athletes); // legacy
		}
		Competition competition = Competition.getCurrent();
		getReportingBeans().put("t", Translator.getMap());
		getReportingBeans().put("competition", competition);
		getReportingBeans().put("session", getGroup()); // legacy
		getReportingBeans().put("group", getGroup());
		getReportingBeans().put("masters", Competition.getCurrent().isMasters());
		getReportingBeans().put("groups", GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			if (compare != 0) {
				return compare;
			}
			return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
		}).collect(Collectors.toList()));
	}

	public void setTemplateFileName(String templateFileName) {
		//logger.debug("setting template file name {}",templateFileName);
		this.templateFileName = templateFileName;
	}

	private InputStream getInputStream() {
		return inputStream;
	}
}
