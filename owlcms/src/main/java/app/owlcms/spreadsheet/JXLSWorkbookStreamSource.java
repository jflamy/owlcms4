/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jxls.builder.JxlsStreaming;
import org.jxls.transform.poi.JxlsPoi;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a
 * link. This class converts the output stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLSWorkbookStreamSource implements StreamResourceWriter, InputStreamFactory {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWorkbookStreamSource.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}
	protected List<Athlete> sortedAthletes;
	private Championship championship;
	private String ageGroupPrefix;
	private Category category;
	private boolean excludeNotWeighed;
	private Group group;
	private InputStream inputStream;
	private HashMap<String, Object> reportingBeans;
	private String templateFileName;
	private UI ui;
	private Consumer<String> doneCallback;
	private String fileExtension;
	private boolean emptyOk = false;

	public JXLSWorkbookStreamSource() {
		this.ui = UI.getCurrent();
		this.setExcludeNotWeighed(true);
		init();
	}

	/**
	 * Read the xls template and write the processed XLS file out.
	 *
	 * @see com.vaadin.flow.server.StreamResourceWriter#accept(java.io.OutputStream,
	 *      com.vaadin.flow.server.VaadinSession)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void accept(OutputStream stream, VaadinSession session) throws IOException {
		try {
			session.lock();
			writeStream(stream);
		} catch (Throwable t) {
			logger.error(LoggerUtils./**/stackTrace(t));
		} finally {
			session.unlock();
		}
	}

	@Override
	public InputStream createInputStream() {
		try {
			PipedInputStream in = new PipedInputStream();
			PipedOutputStream out = new PipedOutputStream(in);
			new Thread(
			        new Runnable() {
				        @Override
				        public void run() {
					        try {
						        writeStream(out);
						        out.close();
					        } catch (IOException e) {
						        throw new RuntimeException(e);
					        }
				        }
			        }).start();
			return in;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the ageDivision
	 */
	public Championship getChampionship() {
		return this.championship;
	}

	/**
	 * @return the ageGroupPrefix
	 */
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	public Category getCategory() {
		return this.category;
	}

	public Consumer<String> getDoneCallback() {
		return this.doneCallback;
	}

	public String getFileExtension() {
		return this.fileExtension;
	}

	public Group getGroup() {
		if (this.group != null) {
			Group nGroup = GroupRepository.getById(this.group.getId());
			return nGroup;
		} else {
			return null;
		}
	}

	public HashMap<String, Object> getReportingBeans() {
		return this.reportingBeans;
	}

	public List<Athlete> getSortedAthletes() {
		return this.sortedAthletes;
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
		return this.templateFileName;
	}

	public boolean isEmptyOk() {
		return this.emptyOk;
	}

	public boolean isExcludeNotWeighed() {
		return this.excludeNotWeighed;
	}

	public void setChampionship(Championship championship) {
		this.championship = championship;
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

	public void setDoneCallback(Consumer<String> action) {
		this.doneCallback = action;
	}

	public void setEmptyOk(boolean emptyOk) {
		this.emptyOk = emptyOk;
	}

	public void setExcludeNotWeighed(boolean excludeNotWeighed) {
		this.excludeNotWeighed = excludeNotWeighed;
	}

	public void setFileExtension(String extension) {
		// logger.debug("setting extension {} in {}",extension,this);
		this.fileExtension = extension;
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
	 * Try the possible variations of a template based on locale. For "/templates/start/startList", ".xls", and a locale
	 * of fr_CA, the following names will be tried /templates/start/startList_fr_CA.xls
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
		if (extension.equals(".xls")) {
			extensionList = Arrays.asList(".xlsx", ".xls");
		} else {
			extensionList = Arrays.asList(extension);
		}

		for (String ext : extensionList) {
			for (String suffix : tryList) {
				String name = templateName + suffix + ext;
				try {
					final InputStream resourceAsStream = ResourceWalker.getFileOrResource(name);
					// logger.debug("trying {} : {}", name, resourceAsStream);
					if (resourceAsStream != null) {
						this.setFileExtension(ext);
						return resourceAsStream;
					}
				} catch (FileNotFoundException e) {
					// ignore
				}
			}
		}
		throw new IOException("no template found for : " + templateName + extension + " tried with suffix " + tryList);
	}

	protected InputStream getTemplate(Locale locale) throws IOException, Exception {
		if (this.inputStream != null) {
			logger.debug("explicitly set template {}", this.inputStream);
			return new BufferedInputStream(this.inputStream);
		}
		String templateFileName2 = getTemplateFileName();
		InputStream resourceAsStream = ResourceWalker.getFileOrResource(templateFileName2);
		return new BufferedInputStream(resourceAsStream);
	}

	protected void init() {
		setReportingBeans(new HashMap<>());
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

		// reuse existing logic for processing records
		JXLSExportRecords jxlsExportRecords = new JXLSExportRecords(null, false);
		jxlsExportRecords.setGroup(getGroup());
		logger.debug("fetching records for session {} category {}", getGroup(), getCategory());
		try {
			jxlsExportRecords.getSortedAthletes();
			// Must be called immediately after getSortedAthletes
			List<RecordEvent> records = jxlsExportRecords.getRecords(getCategory());
			logger.debug("{} records found", records.size());
			getReportingBeans().put("records", records);
		} catch (Exception e) {
			// no records
		}

		getReportingBeans().put("masters", Competition.getCurrent().isMasters());
		List<Group> sessions = GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			if (compare != 0) {
				return compare;
			}
			return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
		}).collect(Collectors.toList());
		getReportingBeans().put("groups", sessions);
		getReportingBeans().put("sessions", sessions);
	}

	@SuppressWarnings("unchecked")
	protected void writeStream(OutputStream stream) throws IOException {
		File tempFile = null;
		try {
			InputStream template;
			Locale locale = OwlcmsSession.getLocale();
			template = getTemplate(locale);
			tempFile = File.createTempFile("jxlsTemplate", ".tmp");
			FileUtils.copyInputStreamToFile(template, tempFile);
			Workbook workbook = WorkbookFactory.create(tempFile);
			if (checkJxls3(workbook)) {
				jxls3Transform(stream, tempFile);
			} else {
				jxls1Transform(stream, workbook);
			}
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			return;
		} finally {
			if (tempFile != null) {
				tempFile.delete();
			}
		}

	}

	private boolean checkJxls3(Workbook tempWorkbook) throws IOException {
		boolean jxls3 = false;
		Sheet sheet = tempWorkbook.getSheetAt(0); // Get the first sheet
		Row row = sheet.getRow(0); // Get the first row (0-based)
		if (row != null) {
			Cell cell = row.getCell(0); // Get the first cell in the row (0-based)
			if (cell != null) {
				Comment comment = cell.getCellComment();
				jxls3 = (comment != null && comment.getString().getString().contains("jx:area"));
			}
		}
		return jxls3;
	}

	private void jxls1Transform(OutputStream stream, Workbook workbook) {
		XLSTransformer transformer = new XLSTransformer();
		configureTransformer(transformer);
		try {
			setReportingInfo();
			HashMap<String, Object> reportingInfo = getReportingBeans();
			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
			if (athletes != null && (athletes.size() > 0 || isEmptyOk())) {
				transformer.transformWorkbook(workbook, reportingInfo);
				logger.debug("after workbook");
				if (workbook != null) {
					postProcess(workbook);
				}
				logger.debug("after postprocess");
			} else {
				String noAthletes = Translator.translate("NoAthletes");
				logger./**/warn("no athletes: empty report.");
				this.ui.access(() -> {
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
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t);
		}
		if (workbook != null) {
			logger.debug("writing stream");
			try {
				workbook.write(stream);
				if (this.doneCallback != null) {
					this.doneCallback.accept(null);
				}
			} catch (Throwable e) {
				LoggerUtils.logError(logger, e);
			}
			logger.debug("wrote stream");
		}
	}

	private void jxls3Transform(OutputStream stream, File templateFile) {
		Workbook workbook = null;
		File tempFile = null;
		try {
			setReportingInfo();
			HashMap<String, Object> reportingInfo = getReportingBeans();
			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
			logger.debug("reportingInfo sessions {}", reportingInfo.get("sessions"));
			if (athletes != null && (athletes.size() > 0 || isEmptyOk())) {
				tempFile = File.createTempFile("jxlsOutput", ".xlsx");
				JxlsPoi.fill(new FileInputStream(templateFile), JxlsStreaming.STREAMING_OFF, reportingInfo, tempFile);
				workbook = WorkbookFactory.create(tempFile);
				logger.debug("after workbook3");
				if (workbook != null) {
					postProcess(workbook);
				}
				logger.debug("after postprocess3");
			} else {
				String noAthletes = Translator.translate("NoAthletes");
				logger./**/warn("no athletes: empty report.");
				this.ui.access(() -> {
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
		} finally {
			if (tempFile != null) {
				tempFile.delete();
			}
		}
		if (workbook != null) {
			logger.debug("writing stream");
			try {
				workbook.write(stream);
				if (this.doneCallback != null) {
					this.doneCallback.accept(null);
				}
			} catch (Throwable e) {
				LoggerUtils.logError(logger, e);
			}
			logger.debug("wrote stream3");
		}
	}

}
