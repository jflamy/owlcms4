/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

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
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a link. This class
 * converts the output stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLSWorkbookStreamSource implements StreamResourceWriter {

	private final static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWorkbookStreamSource.class);
    static {logger.setLevel(Level.INFO);}

    private HashMap<String, Object> reportingBeans;

    private boolean excludeNotWeighed;

	private Group group;

    public JXLSWorkbookStreamSource() {
        this.setExcludeNotWeighed(true);
        init();
    }

    protected void init() {
        setReportingBeans(new HashMap<String, Object>());
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

    protected abstract List<Athlete> getSortedAthletes();

	/**
     * Read the xls template and write the processed XLS file out. 
     * 
	 * @see com.vaadin.flow.server.StreamResourceWriter#accept(java.io.OutputStream, com.vaadin.flow.server.VaadinSession)
	 */
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
                workbook = transformer.transformXLS(getTemplate(locale), reportingBeans2);
            } catch (Exception e) {
            	logger.error(LoggerUtils.stackTrace(e));
            }
            if (workbook != null) {
                postProcess(workbook);
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
	
    abstract public InputStream getTemplate(Locale locale) throws IOException;
    
    public void setReportingBeans(HashMap<String, Object> jXLSBeans) {
        this.reportingBeans = jXLSBeans;
    }

    public HashMap<String, Object> getReportingBeans() {
        return reportingBeans;
    }

    public void setExcludeNotWeighed(boolean excludeNotWeighed) {
        this.excludeNotWeighed = excludeNotWeighed;
    }

    public boolean isExcludeNotWeighed() {
        return excludeNotWeighed;
    }

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
	
    protected void configureTransformer(XLSTransformer transformer) {
        // do nothing, to be overridden as needed,
    }

    protected void postProcess(Workbook workbook) {
        // do nothing, to be overridden as needed,
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
        if (cellLeft == null) return;

        cellLeft.setCellValue("");

        Cell cellRight = row.getCell(cellnum + 1);
        if (cellRight == null) return;

        cellRight.setCellValue("");

        CellStyle blank = workbook.createCellStyle();
        blank.setBorderBottom(BorderStyle.NONE);
        cellLeft.setCellStyle(blank);
        cellRight.setCellStyle(blank);
    }
}
