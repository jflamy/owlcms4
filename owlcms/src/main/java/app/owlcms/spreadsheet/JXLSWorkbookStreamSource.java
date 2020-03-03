/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a
 * link. This class converts the output stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLSWorkbookStreamSource implements StreamResourceWriter {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWorkbookStreamSource.class);
    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    private HashMap<String, Object> reportingBeans;

    private boolean excludeNotWeighed;

    private Group group;
    private UI ui;

    public JXLSWorkbookStreamSource(UI ui) {
        this.ui = ui;
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
            Locale locale = session.getLocale();
            XLSTransformer transformer = new XLSTransformer();
            configureTransformer(transformer);
            Workbook workbook = null;
            try {
                setReportingInfo();
                HashMap<String, Object> reportingBeans2 = getReportingBeans();
                List<Athlete> athletes = (List<Athlete>) reportingBeans2.get("athletes");
                if (athletes != null && athletes.size() > 0) {
                    logger.warn("athletes.size {}", athletes.size());
                    workbook = transformer.transformXLS(getTemplate(locale), reportingBeans2);
                    if (workbook != null) {
                        postProcess(workbook);
                    }
                } else {
                    ui.access(() -> {
                        Notification notif = new Notification();
                        notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notif.setPosition(Position.TOP_STRETCH);
                        notif.setDuration(3000);
                        notif.setText("No Athletes");
                        notif.open();
                    });
                    workbook = new HSSFWorkbook();
                    workbook.createSheet().createRow(1).createCell(1).setCellValue("No Athletes");
                }
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
            if (workbook != null) {
                workbook.write(stream);
            }
        } catch (IOException e) {
            // ignore
        } catch (Throwable t) {
            logger.error(LoggerUtils.stackTrace(t));
        } finally {
            session.unlock();
        }
    }

    public Group getGroup() {
        return group;
    }

    public HashMap<String, Object> getReportingBeans() {
        return reportingBeans;
    }

    public List<String> getSuffixes(Locale locale) {
        List<String> tryList = new ArrayList<>();
        if (!locale.getVariant().isEmpty()) {
            tryList.add("_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant());
        }
        if (!locale.getCountry().isEmpty()) {
            tryList.add("_" + locale.getLanguage() + "_" + locale.getCountry());
        }
        if (!locale.getLanguage().isEmpty()) {
            tryList.add("_" + locale.getLanguage());
        }
        // always add English as last resort.
        if (!locale.getLanguage().equals("en")) {
            tryList.add("_" + "en");
        }
        if (!locale.getLanguage().equals("en")) {
            tryList.add("");
        }
        return tryList;
    }

    abstract public InputStream getTemplate(Locale locale) throws IOException;

    public boolean isExcludeNotWeighed() {
        return excludeNotWeighed;
    }

    public void setExcludeNotWeighed(boolean excludeNotWeighed) {
        this.excludeNotWeighed = excludeNotWeighed;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setReportingBeans(HashMap<String, Object> jXLSBeans) {
        this.reportingBeans = jXLSBeans;
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
        for (String suffix : tryList) {
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName + suffix + extension);
            if (resourceAsStream != null) {
                return resourceAsStream;
            }
        }
        throw new IOException("no template found for : " + templateName + extension + " tried with suffix " + tryList);
    }

    protected abstract List<Athlete> getSortedAthletes();

    protected void init() {
        setReportingBeans(new HashMap<String, Object>());
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
        getReportingBeans().put("competition", competition);
        getReportingBeans().put("session", getGroup()); // legacy
        getReportingBeans().put("group", getGroup());
        getReportingBeans().put("masters", Competition.getCurrent().isMasters());
    }
}
