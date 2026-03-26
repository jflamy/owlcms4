/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
// using per-task Threads instead of a pooled ExecutorService avoids inheritable ThreadLocal
// leakage from pooled threads. A dedicated daemon Thread is started for each request.
import java.util.AbstractList;
import java.util.concurrent.atomic.AtomicReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jxls.builder.JxlsStreaming;
import org.jxls.transform.poi.JxlsPoi;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.OwlcmsSessionThreadLocal;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.LocalResource;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a
 * source of data when the user clicks on a link. This class converts the output
 * stream
 * to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLSWorkbookStreamSource implements StreamResourceWriter, InputStreamFactory {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWorkbookStreamSource.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	private static ThreadLocal<Ranking> bestLifterRankingSystem = InheritableThreadLocal.withInitial(() -> null);
	private static ThreadLocal<Boolean> noInterimScoresInResults = InheritableThreadLocal.withInitial(() -> null);

	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	// prepareWithoutTemplate() removed — UI should use lightweight checks via
	// defaultPreCheckFor(...) and
	// writers should perform their own prepare() which may include template
	// resolution as needed.

	// No shared executor here; each download starts a short-lived daemon Thread.

	public static Ranking getBestLifterRankingThreadLocal() {
		Ranking blss = bestLifterRankingSystem.get();
		// if (blss == null) {
		// blss = Competition.getCurrent().getScoringSystem();
		// }
		return blss;
	}

	public static void setBestLifterRankingThreadLocal(Ranking bestLifterRankingValue) {
		bestLifterRankingSystem.set(bestLifterRankingValue);
	}

	protected static void setNoInterimScoresInResults(boolean noInterimScoresInResultsP) {
		noInterimScoresInResults.set(noInterimScoresInResultsP);
	}

	public static boolean isNoInterimScoresInResults() {
		Boolean blss = noInterimScoresInResults.get();
		return blss != null && Boolean.TRUE.equals(blss);
	}

	private List<Athlete> sortedAthletes;
	private Championship championship;
	private String ageGroupPrefix;
	private Gender gender;
	private Category category;
	private boolean excludeNotWeighed;
	private Group group;
	protected InputStream inputStream;
	private HashMap<String, Object> reportingBeans;
	private String templateFileName;
	@SuppressWarnings("unused")
	private UI ui;
	private java.util.function.Consumer<Throwable> doneCallback;
	private String fileExtension;
	private boolean emptyOk = false;
	private Integer pageLength = null;
	private Ranking bestLifterScoringSystem;
	private Integer lastLine;
	private Integer firstMergeLine;
	private List<Integer> mergeColumnList;

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
			logger.debug("getting {}", getBestLifterScoringSystem());
			writeStream(stream);
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t);
			logger.error("writeStream failed: {}", LoggerUtils.stackTrace(t));
		} finally {
			session.unlock();
		}
	}

	@Override
	public InputStream createInputStream() {
		logger.debug("createInputStream called {}\n", LoggerUtils.stackTrace());
		// IMPORTANT: do NOT access VaadinSession or UI here. Pre-checks that require
		// UI/Session must be executed by the caller (for example
		// LazyDownloadButton.preCheck()).
		// Return the background-driven InputStream immediately so Vaadin can stream it.
		// Ensure reporting beans are available for the writer. Some callers may not
		// invoke
		// prepare() beforehand, so we keep this defensive initialization here as well.
		setReportingInfo();
		return doCreateStream();
	}

	protected InputStream doCreateStream() {
		final PipedInputStream in = new PipedInputStream();
		final PipedOutputStream out;
		try {
			out = new PipedOutputStream(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final AtomicReference<IOException> writerException = new AtomicReference<>();

		Thread writerThread = new Thread(() -> {
			try {
				// Propagate instance field values to ThreadLocal for cross-cutting concerns.
				// The ThreadLocal is read by classes (e.g., Athlete.getBestLifterScore()) that
				// don't have direct access to this JXLSWorkbookStreamSource instance.
				if (this.bestLifterScoringSystem != null) {
					setBestLifterRankingThreadLocal(this.bestLifterScoringSystem);
				}
				writeStream(out);
				// success: notify caller
				try {
					if (this.doneCallback != null)
						this.doneCallback.accept(null);
				} catch (Throwable cb) {
					/* swallow */ }
			} catch (Throwable t) {
				// notify doneCallback with a user-friendly message when available
				try {
					if (this.doneCallback != null) {
						try {
							this.doneCallback.accept(t);
						} catch (Throwable cb) {
							/* swallow */ }
					}
				} catch (Throwable ignore) {
				}

				if (t instanceof IOException) {
					writerException.set((IOException) t);
				} else if (t.getCause() instanceof IOException) {
					writerException.set((IOException) t.getCause());
				} else if (t instanceof StopProcessingException) {
					writerException.set(new IOException(t));
				} else {
					writerException.set(new IOException(t));
				}
			} finally {
				// Clear thread-local state to avoid leaking session/context if the Thread
				// object is retained for any reason. This is defensive: per-task threads
				// are normally reclaimed by the GC once terminated, but clearing is
				// low-cost and prevents surprises if code changes later.
				try {
					OwlcmsSessionThreadLocal.remove();
				} catch (Throwable ignore) {
				}
				try {
					bestLifterRankingSystem.remove();
				} catch (Throwable ignore) {
				}
				try {
					noInterimScoresInResults.remove();
				} catch (Throwable ignore) {
				}
				try {
					out.close();
				} catch (IOException e) {
					logger.error("Error closing piped output stream", e);
				}
			}
		}, "JXLSWorkbookStreamSource-writer");
		writerThread.setDaemon(true);
		writerThread.start();

		return new InputStreamWrapper(in, writerException);
	}

	public void extractVariables(String comment) {
		logger.debug("comment = {}", comment);
		comment = comment.replaceAll("[\\r\\n\\s]", "");

		// Pattern to match jx:area(lastCell="X1")
		Pattern pattern1 = Pattern.compile("jx:area\\(lastCell=\"([A-Za-z])(\\d+)\"\\)");
		Matcher matcher1 = pattern1.matcher(comment);
		if (matcher1.find()) {
			logger.debug("last line = {}", matcher1.group(2));
			setLastLine(Integer.parseInt(matcher1.group(2)));
		}

		// Pattern to match owlcms:fixMerges(4, [1, 2, 3]) with optional spaces
		Pattern pattern2 = Pattern.compile("owlcms:fixMerges\\((\\d+),\\[(.*?)\\]\\)");
		Matcher matcher2 = pattern2.matcher(comment);
		if (matcher2.find()) {
			logger.debug("firstMergeLine = {}", matcher2.group(1));
			setFirstMergeLine(Integer.parseInt(matcher2.group(1)));
			String columns = matcher2.group(2);

			// Convert columns to a list of integers
			String[] columnsArray = columns.split("\\s*,\\s*");
			setMergeColumnList(new ArrayList<>());
			for (String column : columnsArray) {
				logger.debug("column: {}", column.trim());
				getMergeColumnList().add(Integer.parseInt(column.trim()));
			}
		}
	}

	/**
	 * @return the ageGroupPrefix
	 */
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	public Ranking getBestLifterScoringSystem() {
		return this.bestLifterScoringSystem;
	}

	public Category getCategory() {
		return this.category;
	}

	/**
	 * @return the ageDivision
	 */
	public Championship getChampionship() {
		return this.championship;
	}

	public java.util.function.Consumer<Throwable> getDoneCallback() {
		return this.doneCallback;
	}

	public String getFileExtension() {
		return this.fileExtension;
	}

	public Integer getFirstMergeLine() {
		return this.firstMergeLine;
	}

	public Group getGroup() {
		if (this.group != null) {
			Group nGroup = GroupRepository.getById(this.group.getId());
			return nGroup;
		} else {
			return null;
		}
	}

	public Integer getLastLine() {
		return this.lastLine;
	}

	public List<Integer> getMergeColumnList() {
		return this.mergeColumnList;
	}

	// Missing helper accessors used by subclasses and internal logic
	public HashMap<String, Object> getReportingBeans() {
		return this.reportingBeans;
	}

	public void setReportingBeans(HashMap<String, Object> beans) {
		this.reportingBeans = beans;
	}

	public void setExcludeNotWeighed(boolean exclude) {
		this.excludeNotWeighed = exclude;
	}

	public boolean isEmptyOk() {
		return this.emptyOk;
	}

	public int getSizeLimit() {
		// default generous limit; subclasses may override
		return Integer.MAX_VALUE;
	}

	final public List<Athlete> getSortedAthletes() {
		return this.sortedAthletes;
	}

	public List<Athlete> computeSortedAthletes() {
		return this.getSortedAthletes();
	}

	public String getTemplateFileName() {
		return this.templateFileName;
	}

	public void setPageLength(Integer pageLength) {
		this.pageLength = pageLength;
	}

	public Integer getPageLength() {
		return this.pageLength;
	}

	public void setLastLine(Integer lastLine) {
		this.lastLine = lastLine;
	}

	public void setFirstMergeLine(Integer firstMergeLine) {
		this.firstMergeLine = firstMergeLine;
	}

	public void setMergeColumnList(List<Integer> list) {
		this.mergeColumnList = list;
	}

	public void setFileExtension(String ext) {
		this.fileExtension = ext;
	}

	// getTemplate(Locale) now has a default implementation lower in the class;
	// subclasses may override it.

	/**
	 * Default concrete writeStream that reads the template and delegates to the
	 * jxls transform helpers already defined in this class.
	 */
	protected void writeStream(OutputStream stream) throws IOException {
		logger.debug("writeStream {}", this.getClass().getName());
		File tempFile = null;
		InputStream template = null;
		try {
			// Use the provided template stream if one was explicitly set via
			// setInputStream().
			// Otherwise, fetch the default template. In either case, wrap in
			// BufferedInputStream
			// for efficiency and mark/reset support, then copy to a temp file immediately
			// so the
			// original stream is available for reuse on subsequent downloads.
			if (this.inputStream != null) {
				template = new BufferedInputStream(this.inputStream);
			} else {
				template = new BufferedInputStream(getTemplate(OwlcmsSession.getLocale()));
			}

			// Copy template to temp file so WorkbookFactory/JXLS can operate on it
			tempFile = File.createTempFile("jxlsTemplate", ".tmp");
			FileUtils.copyInputStreamToFile(template, tempFile);

			Workbook workbook = WorkbookFactory.create(new FileInputStream(tempFile));
			if (checkJxls3(workbook)) {
				jxls3Transform(stream, tempFile);
			} else {
				jxls1Transform(stream, workbook);
			}
		} catch (StopProcessingException e) {
			LoggerUtils.logError(logger, e);
			// rethrow StopProcessingException directly so caller can handle it
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t);
			throw new IOException(t);
		} finally {
			try {
				if (template != null) {
					template.close();
				}
			} catch (IOException ignore) {
			}
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}

	// Common setters used by UI and other callers
	public void setInputStream(InputStream template) {
		this.inputStream = template;
	}

	public void setDoneCallback(java.util.function.Consumer<Throwable> cb) {
		this.doneCallback = cb;
	}

	public void setSortedAthletes(List<Athlete> athletes) {
		logger.debug("setSortedAthletes called, {} athletes {}", athletes != null ? athletes.size() : 0,
				LoggerUtils.whereFrom());
		this.sortedAthletes = athletes;
	}

	public void setGroup(Group group) {
		logger.debug("setGroup called, group = {} {}", group, LoggerUtils.whereFrom());
		this.group = group;
	}

	public boolean isExcludeNotWeighed() {
		return this.excludeNotWeighed;
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

	protected void createStandardFooter(Workbook workbook) {
		// Get the current date and time
		LocalDateTime now = LocalDateTime.now();

		// Get the default locale
		Locale currentLocale = OwlcmsSession.getLocale();

		// Get a date formatter for the short date format in the current locale
		String shortDatePattern = DateTimeUtils.localizedShortDatePattern(currentLocale);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(shortDatePattern, currentLocale);

		// Get a time formatter for the short time format in the current locale
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
				.withLocale(currentLocale);

		// Format the current date and time
		String formattedDate = now.format(dateFormatter);
		String formattedTime = now.format(timeFormatter);

		Footer footer = workbook.getSheetAt(0).getFooter();

		footer.setLeft(Translator.translate("Results.producedBy", "owlcms", OwlcmsFactory.getVersion()));
		footer.setCenter(Translator.translate("Results.dateTime", formattedDate, formattedTime));
		footer.setRight(Translator.translate("Results.pageOf", HeaderFooter.page(), HeaderFooter.numPages()));
	}

	/**
	 * Try the possible variations of a template based on locale. For
	 * "/templates/start/startList", ".xls", and a locale of fr_CA, the following
	 * names will be
	 * tried /templates/start/startList_fr_CA.xls /templates/start/startList_fr.xls
	 * /templates/start/startList_en.xls
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

	private List<String> getSuffixes(Locale locale) {
		List<String> result = new ArrayList<>();
		if (locale == null) {
			result.add("");
			return result;
		}
		String language = locale.getLanguage();
		String country = locale.getCountry();
		if (language != null && !language.isEmpty()) {
			if (country != null && !country.isEmpty()) {
				result.add("_" + language + "_" + country);
			}
			result.add("_" + language);
		}
		result.add("");
		return result;
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
		List<Athlete> athletes = computeSortedAthletes();
		if (athletes != null) {
			getReportingBeans().put("athletes", athletes);
			getReportingBeans().put("lifters", athletes); // legacy
		}
		try {
			logger.debug("{} setReportingInfo called, group = {} category = {} championship = {} athletes.size {} {}",
					this.getClass().getSimpleName(), getGroup(), getCategory(), getChampionship(),
					athletes != null ? athletes.size() : "null",
					LoggerUtils.whereFrom());
			Competition competition = Competition.getCurrent();
			getReportingBeans().put("t", Translator.getMap());
			getReportingBeans().put("tf", new JXLSFormatter());
			getReportingBeans().put("competition", competition);
			getReportingBeans().put("session", getGroup());
			getReportingBeans().put("group", getGroup());// legacy
			getReportingBeans().put("platforms", PlatformRepository.findAll());
			getReportingBeans().put("coaches", CoachRepository.findAll());
			getReportingBeans().put("tos", TechnicalOfficialRepository.findActive());
			getReportingBeans().put("local", LocalResource.class);

			Object recordsBean = createRecordsBean();
			if (recordsBean != null) {
				getReportingBeans().put("records", recordsBean);
			}
		} catch (Exception e) {
			logger.error("Exception while fetching records for session {} category {}", getGroup(), getCategory(), e);
		}

		getReportingBeans().put("masters", Competition.getCurrent().isMasters());

		getReportingBeans().put("championship", getChampionship());
		getReportingBeans().put("ageGroupPrefix", getAgeGroupPrefix());
		getReportingBeans().put("gender", getGender());

		List<Group> sessions = GroupRepository.findAll().stream().sorted(Group.groupWeighinTimeComparator)
				.collect(Collectors.toList());

		// Ranking overallScoringSystem = this.getBestLifterScoringSystem();
		// overallScoringSystem = overallScoringSystem != null ? overallScoringSystem :
		// Competition.getCurrent().getScoringSystem();
		Ranking overallScoringSystem = getBestLifterRankingThreadLocal();

		// make available to the Athlete class in this Thread (and subThreads).
		this.reportingBeans.put("bestRankingTitle",
				overallScoringSystem != null ? Ranking.getScoringTitle(overallScoringSystem)
						: Translator.translate("BestAthlete"));

		getReportingBeans().put("groups", sessions);
		getReportingBeans().put("sessions", sessions);
	}

	protected Object createRecordsBean() {
		logger.debug("fetching records for session {} category {}", getGroup(), getCategory());
		JXLSExportRecords jxlsExportRecords = new JXLSExportRecords(null, false, false);
		jxlsExportRecords.setGroup(getGroup());
		jxlsExportRecords.computeSortedAthletes();
		List<RecordEvent> records = normalizeRecordEventsForTemplate(jxlsExportRecords.getRecords(getCategory()));
		logger.debug("{} records found", records != null ? records.size() : 0);
		return records;
	}

	protected List<RecordEvent> normalizeRecordEventsForTemplate(List<RecordEvent> records) {
		if (records == null) {
			return null;
		}
		for (RecordEvent e : records) {
			if (e.getBwCatUpper() > 250) {
				e.setBwCatString(">" + e.getBwCatLower());
			} else {
				e.setBwCatString(Integer.toString(e.getBwCatUpper()));
			}
		}
		return records;
	}

	protected static class LazyRecordEventList extends AbstractList<RecordEvent> {
		private java.util.function.Supplier<List<RecordEvent>> supplier;
		private List<RecordEvent> delegate;

		protected LazyRecordEventList(java.util.function.Supplier<List<RecordEvent>> supplier) {
			this.supplier = supplier;
		}

		@Override
		public RecordEvent get(int index) {
			return resolve().get(index);
		}

		@Override
		public int size() {
			return resolve().size();
		}

		private List<RecordEvent> resolve() {
			if (this.delegate == null) {
				List<RecordEvent> resolved = this.supplier.get();
				this.delegate = resolved != null ? resolved : List.of();
				this.supplier = null;
			}
			return this.delegate;
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
				if (comment != null && comment.getString().getString().contains("jx:area")) {
					jxls3 = true;

					extractVariables(comment.getString().getString());
					if (getLastLine() != null) {
						this.setPageLength(getLastLine());
					}
				}
			}
		}
		return jxls3;
	}

	private boolean isSizeOk(int size) {
		return size < getSizeLimit();
	}

	private void jxls1Transform(OutputStream stream, Workbook workbook) {
		XLSTransformer transformer = new XLSTransformer();
		configureTransformer(transformer);
		try {
			HashMap<String, Object> reportingInfo = getReportingBeans();

			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
			if (athletes != null && (athletes.size() == 0 ? isEmptyOk() : isSizeOk(athletes.size()))) {
				logger.info("{} before transformWorkbook", this.getTemplateFileName());
				long start = System.currentTimeMillis();
				transformer.transformWorkbook(workbook, reportingInfo);
				logger.info("{} after transformWorkbook ({} ms)", this.getTemplateFileName(),
						System.currentTimeMillis() - start);
				if (workbook != null) {
					postProcess(workbook);
				}
				logger.debug("after postprocess");
			} else {
				String localized = Translator.translate("NoAthletes");
				logger./**/warn("No athletes: empty report.");
				// treat as a validation failure -> stop processing and let caller handle the
				// error
				throw new StopProcessingException("NoAthletes", new RuntimeException(localized));
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
		logger.debug("jxls3Transform called class={} template={}\n{}", this.getClass().getName(), templateFile,
				LoggerUtils.stackTrace());
		Workbook workbook = null;
		File tempFile = null;
		try {
			HashMap<String, Object> reportingInfo = getReportingBeans();
			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
			int size = athletes != null ? athletes.size() : 0;
			logger.debug("reportingInfo sessions {} athletes: {}", reportingInfo.get("sessions"), size);
			if (size == 0 ? isEmptyOk() : isSizeOk(size)) {
				tempFile = File.createTempFile("jxlsOutput", ".xlsx");
				logger.info("starting jxls3 processing for {}", templateFile);
				long start = System.currentTimeMillis();
				JxlsPoi.fill(new FileInputStream(templateFile), JxlsStreaming.STREAMING_OFF, reportingInfo, tempFile);
				logger.info("processing done: {}ms", System.currentTimeMillis() - start);
				workbook = WorkbookFactory.create(tempFile);
				if (workbook != null) {
					start = System.currentTimeMillis();
					logger.info("postProcessing {} {}", templateFile, this.getClass().getName());
					postProcess(workbook);
					logger.info("postProcessing done: {}ms", System.currentTimeMillis() - start);
				}
			} else {
				if (athletes == null || athletes.size() == 0) {
					String localized = Translator.translate("NoAthletes");
					logger./**/warn("no athletes: empty report.");
					throw new StopProcessingException("NoAthletes", new RuntimeException(localized));
				} else {
					String localized = Translator.translate("TooManyAthletes", Integer.toString(getSizeLimit()));
					logger./**/warn("too many athletes : no report");
					// let caller handle the notification and error propagation
					throw new StopProcessingException("TooManyAthletes", new RuntimeException(localized));
				}
			}
		} catch (IOException e) {
			LoggerUtils.logError(logger, e);
			throw new RuntimeException(e);
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

	public void setUi(UI current) {
		this.ui = current;
	}

	public UI getUi() {
		return ui;
	}

	/**
	 * Default implementation of getTemplate. Subclasses may override to provide a
	 * custom template lookup.
	 */
	public InputStream getTemplate(Locale locale) throws IOException {
		if (this.templateFileName != null) {
			String name = this.templateFileName;
			String ext = "";
			int dot = name.lastIndexOf('.');
			if (dot >= 0) {
				ext = name.substring(dot);
				name = name.substring(0, dot);
			}
			if (ext == null || ext.isEmpty()) {
				ext = ".xls";
			}
			return getLocalizedTemplate(name, ext, locale);
		}
		throw new IOException("No templateFileName set for " + this.getClass().getName());
	}

	public void setTemplateFileName(String templateFileName) {
		this.templateFileName = templateFileName;
	}

	public void setChampionship(Championship championship) {
		this.championship = championship;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	public Gender getGender() {
		return this.gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public void setBestLifterScoringSystem(Ranking bestLifterScoringSystem) {
		this.bestLifterScoringSystem = bestLifterScoringSystem;
	}

	public void setEmptyOk(boolean emptyOk) {
		this.emptyOk = emptyOk;
	}

	/**
	 * Optional pre-check invoked before creating the input stream. Implementations
	 * should return an Optional
	 * containing an Exception when the download should be aborted early (for
	 * example when there's no data).
	 * The default implementation performs basic reporting-info validation used by
	 * many JXLS exporters.
	 */
	public Optional<Exception> prepare() {
		try {

			setReportingInfo();
			// Validate that the template exists and is readable on the UI thread.
			// If a caller provided a custom template via setInputStream(), trust that they
			// manage it properly.
			// Otherwise, validate that the default template exists.
			try {
				if (this.inputStream == null) {
					// No custom template set; validate the default template can be loaded
					InputStream testTemplate = getTemplate(OwlcmsSession.getLocale());
					if (testTemplate != null) {
						testTemplate.close();
					}
				}
				// If inputStream is set, don't touch it - caller is responsible for managing it
			} catch (IOException e) {
				return Optional.of(e);
			}
			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) getReportingBeans().get("athletes");
			int size = athletes != null ? athletes.size() : 0;
			if (!(size == 0 ? isEmptyOk() : isSizeOk(size))) {
				if (athletes == null || athletes.size() == 0) {
					String localized = Translator.translate("NoAthletes");
					return Optional.of(new StopProcessingException("NoAthletes", new RuntimeException(localized)));
				} else {
					String localized = Translator.translate("TooManyAthletes", Integer.toString(getSizeLimit()));
					return Optional.of(new StopProcessingException("TooManyAthletes", new RuntimeException(localized)));
				}
			}

			return Optional.empty();
		} catch (Exception e) {
			e.printStackTrace();

			return Optional.of(e);
		}
	}
}
