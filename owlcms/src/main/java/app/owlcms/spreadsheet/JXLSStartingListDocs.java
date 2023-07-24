/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroupRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSStartingListDocs extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSStartingListDocs.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	private Consumer<Workbook> postProcessor;

	public JXLSStartingListDocs() {
		super();
		this.setExcludeNotWeighed(false);
	}

	@Override
	protected void postProcess(Workbook workbook) {
		if (postProcessor != null) {
			postProcessor.accept(workbook);
		}
	}

	public Consumer<Workbook> getPostProcessor() {
		return postProcessor;
	}

	public void setPostProcessor(Consumer<Workbook> postProcessor) {
		this.postProcessor = postProcessor;
	}

//	@Override
//	protected List<Athlete> getSortedAthletes() {
//		List<Athlete> registrationOrderCopy = AthleteSorter.registrationOrderCopy(sortedAthletes);
//		return registrationOrderCopy;
//	}

	public void createAgeGroupColumns(int listColumn, int catColumn) {
		setPostProcessor((w) -> {
			List<String> prefixes = AgeGroupRepository.findAgeGroupPrefixes(null);
			Sheet sheet = w.getSheetAt(0);
			int emptyCells = 0;

			Row headerRow = sheet.getRow(5);
			int categoryWidth = sheet.getColumnWidth(catColumn);
			CellStyle categoryStyle = headerRow.getCell(catColumn).getCellStyle();

			int offset = 0;
			for (String pr : prefixes) {
				sheet.setColumnWidth(listColumn + offset, categoryWidth);
				headerRow.createCell(listColumn + offset);
				headerRow.getCell(listColumn + offset).setCellValue(pr);
				headerRow.getCell(listColumn + offset).setCellStyle(categoryStyle);
				offset++;
			}

			int lastLine = 0;
			for (Row r : sheet) {
				if (r.getRowNum() < 7) {
					continue;
				}
				Cell cell = r.getCell(0);
				if (cell == null || cell.getCellType() == CellType.BLANK) {
					emptyCells++;
				} else {
					emptyCells = 0;
				}
				if (emptyCells == 2) {
					lastLine = r.getRowNum();
					break;
				}

				if (emptyCells == 0) {
					Cell eligibleCatsCell = r.getCell(listColumn - 1);
					String eligibleCatsString = eligibleCatsCell.getStringCellValue();
					if (eligibleCatsString != null && !eligibleCatsString.isBlank()) {
						String[] eligibleCats = eligibleCatsString.split(";");
						Cell cell2 = r.getCell(catColumn);
						CellStyle rstyle = cell2.getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							r.createCell(listColumn + prefixOffset);
							r.getCell(listColumn + prefixOffset).setCellStyle(rstyle);
							for (String catString : eligibleCats) {
								if (catString.startsWith(prefixes.get(prefixOffset))) {
									r.getCell(listColumn + prefixOffset).setCellValue(catString);
								}
							}

						}
					} else {
						CellStyle estyle = r.getCell(listColumn - 1).getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size()+1; prefixOffset++) {
							CellStyle tstyle = r.getCell(listColumn - 1 -  prefixes.size() + prefixOffset).getCellStyle();
							r.createCell(listColumn - 1 + prefixOffset);
							r.getCell(listColumn - 1 + prefixOffset).setCellStyle(tstyle);
						}
						r.createCell(listColumn - 1 + prefixes.size());
						r.getCell(listColumn - 1 + prefixes.size()).setCellStyle(estyle);
					}
				}
			}
			sheet.setColumnHidden(listColumn-1, true);
			
			sheet.addMergedRegion(new CellRangeAddress(4,4,0,listColumn-1+prefixes.size()));
			w.setPrintArea(0, 0, listColumn-1+prefixes.size(), 0, lastLine);
		});
	}

	public void createTeamColumns(int listColumn, int catColumn) {
		setPostProcessor((w) -> {
			List<String> prefixes = AgeGroupRepository.findAgeGroupPrefixes(null);
			Sheet sheet = w.getSheetAt(0);
			int emptyCells = 0;

			Row headerRow = sheet.getRow(5);
			int categoryWidth = sheet.getColumnWidth(catColumn);
			CellStyle style = headerRow.getCell(catColumn).getCellStyle();

			int offset = 0;
			for (String pr : prefixes) {
				sheet.setColumnWidth(listColumn + offset, categoryWidth);
				headerRow.createCell(listColumn + offset);
				headerRow.getCell(listColumn + offset).setCellValue(pr);
				headerRow.getCell(listColumn + offset).setCellStyle(style);
				offset++;
			}

			int lastLine = 0;
			for (Row r : sheet) {
				if (r.getRowNum() < 7) {
					continue;
				}
				Cell cell = r.getCell(1);
				if (cell == null || cell.getCellType() == CellType.BLANK) {
					emptyCells++;
				} if (cell == null || (cell.getCellType() == CellType.NUMERIC && cell.getNumericCellValue() == 0)) {
					emptyCells++;
				} else {
					emptyCells = 0;
				}
				if (emptyCells == 2) {
					lastLine = r.getRowNum();
					break;
				}

				if (emptyCells == 0) {
					Cell eligibleCatsCell = r.getCell(listColumn-1);
					String eligibleCatsString = eligibleCatsCell.getStringCellValue();
					if (eligibleCatsString != null && !eligibleCatsString.isBlank()) {
						String[] eligibleCats = eligibleCatsString.split(";");
						CellStyle rstyle = r.getCell(catColumn).getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							r.createCell(listColumn + prefixOffset);
							r.getCell(listColumn + prefixOffset).setCellStyle(rstyle);
							for (String catString : eligibleCats) {
								if (catString.startsWith(prefixes.get(prefixOffset))) {
									r.getCell(listColumn + prefixOffset).setCellValue(catString);
								}
							}

						}
					} else {
						CellStyle estyle = r.getCell(listColumn-1).getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size()+1; prefixOffset++) {
							CellStyle tstyle = r.getCell(listColumn - 1 -  prefixes.size() + prefixOffset).getCellStyle();
							r.createCell(listColumn - 1 + prefixOffset);
							r.getCell(listColumn - 1 + prefixOffset).setCellStyle(tstyle);
						}
						r.createCell(listColumn - 1 + prefixes.size());
						r.getCell(listColumn - 1 + prefixes.size()).setCellStyle(estyle);
					}
				}
			}
			sheet.setColumnHidden(listColumn-1, true);
			
			sheet.addMergedRegion(new CellRangeAddress(4,4,0,listColumn-1+prefixes.size()));
			w.setPrintArea(0, 0, listColumn-1+prefixes.size(), 0, lastLine);
		});
	}
}
