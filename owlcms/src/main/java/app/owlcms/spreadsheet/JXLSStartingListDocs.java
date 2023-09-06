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
import app.owlcms.i18n.Translator;
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
						for (int prefixOffset = 0; prefixOffset < prefixes.size() + 1; prefixOffset++) {
							CellStyle tstyle = r.getCell(listColumn - 1 - prefixes.size() + prefixOffset)
							        .getCellStyle();
							r.createCell(listColumn - 1 + prefixOffset);
							r.getCell(listColumn - 1 + prefixOffset).setCellStyle(tstyle);
						}
						r.createCell(listColumn - 1 + prefixes.size());
						r.getCell(listColumn - 1 + prefixes.size()).setCellStyle(estyle);
					}
				}
			}
			sheet.setColumnHidden(listColumn - 1, true);

			sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, listColumn - 1 + prefixes.size()));
			w.setPrintArea(0, 0, listColumn - 1 + prefixes.size(), 0, lastLine);
		});
	}

	public void createTeamColumns(int listColumn, int catColumn) {
		setPostProcessor((w) -> {
			int listColumnVFEOffset = 0;

			List<String> prefixes = AgeGroupRepository.findAgeGroupPrefixes(null);
			Sheet sheet = w.getSheetAt(0);

			Row headerRow = sheet.getRow(5);
			int categoryWidth = sheet.getColumnWidth(catColumn + 1);
			CellStyle style = headerRow.getCell(catColumn + 1).getCellStyle();

			// check if this is a VFE sheet.
			String stringCellValue = headerRow.getCell(catColumn + 1 + 1).getStringCellValue();
			if (stringCellValue.contentEquals(Translator.translate("Change"))) {
				// the team membership column remains, hidden.
				// cat change + entry change + team membership = 3
				listColumnVFEOffset = 3; // entry + entry change + cat change = 3
			}

			int offset = 0;
			for (String pr : prefixes) {
				sheet.setColumnWidth(listColumn + offset + listColumnVFEOffset, categoryWidth);
				headerRow.createCell(listColumn + offset + listColumnVFEOffset);
				headerRow.getCell(listColumn + offset + listColumnVFEOffset).setCellValue(pr);
				headerRow.getCell(listColumn + offset + listColumnVFEOffset).setCellStyle(style);
				offset++;
			}

			int lastLine = 0;
			int sourceCol = listColumn - 1 + listColumnVFEOffset;
			int nonContentCounter = 0;
			for (Row r : sheet) {
				if (r.getRowNum() < 7) {
					continue;
				}

				Cell firstCell = r.getCell(0);
				Cell nameCell = r.getCell(3);
				boolean firstCellNonBlank = firstCell != null
				        && (firstCell.getCellType() == CellType.STRING && !firstCell.getStringCellValue().isBlank());
				boolean nameCellNonBlank = nameCell != null
				        && (nameCell.getCellType() == CellType.STRING && !nameCell.getStringCellValue().isBlank());
				boolean contentRow = firstCellNonBlank && nameCellNonBlank;

				Cell catCell = r.getCell(catColumn);
				if (contentRow) {
					// split the categories and create individual cells
					Cell eligibleCatsCell = r.getCell(sourceCol);
					String eligibleCatsString = eligibleCatsCell.getStringCellValue();
					if (eligibleCatsString != null && !eligibleCatsString.isBlank()) {
						String[] eligibleCats = eligibleCatsString.split(";");
						CellStyle categoryStyle = catCell.getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							int targetCol = listColumn + prefixOffset + listColumnVFEOffset;
							r.createCell(targetCol);
							r.getCell(targetCol).setCellStyle(categoryStyle);
							for (String catString : eligibleCats) {
								if (catString.startsWith(prefixes.get(prefixOffset))) {
									r.getCell(targetCol).setCellValue(catString);
								}
							}
						}
					}
					nonContentCounter = 0;
				} else {
					Cell endCell = r.getCell(listColumn - 1);
					if (endCell != null && catCell != null) {
						CellStyle endCellStyle = endCell.getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							CellStyle categoryStyle = catCell.getCellStyle();
							int targetCol = listColumn + prefixOffset + listColumnVFEOffset;
							r.createCell(targetCol);
							r.getCell(targetCol).setCellStyle(categoryStyle);
						}
						r.createCell(listColumn - 1 + prefixes.size() + listColumnVFEOffset);
						r.getCell(listColumn - 1 + prefixes.size() + listColumnVFEOffset).setCellStyle(endCellStyle);
					}
					nonContentCounter++;
				}
				if (nonContentCounter > 5)
					break;
			}
			sheet.setColumnHidden(sourceCol, true);

			sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, listColumn - 1 + prefixes.size()));
			w.setPrintArea(0, 0, listColumn - 1 + prefixes.size(), 0, lastLine);
		});
	}
}
