/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
		this.setExcludeNotWeighed(false);
	}

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
							r.createCell(listColumn - 1 + prefixOffset);
							int cellnum = listColumn - 1 - prefixes.size() + prefixOffset;
							if (cellnum >= 0) {
								CellStyle tstyle = r.getCell(cellnum).getCellStyle();
								r.getCell(listColumn - 1 + prefixOffset).setCellStyle(tstyle);
							}
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
			boolean vfeTemplate = false;

			List<String> prefixes = AgeGroupRepository.findAgeGroupPrefixes(null);
			Sheet sheet = w.getSheetAt(0);

			Row headerRow = sheet.getRow(5);
			int categoryWidth = sheet.getColumnWidth(catColumn + 1);
			CellStyle style = headerRow.getCell(catColumn + 1).getCellStyle();

			// check if this is a VFE sheet.
			Row templateMarkerRow = sheet.getRow(4);
			Cell templateMarkerCell = templateMarkerRow != null ? templateMarkerRow.getCell(0) : null;
			if (templateMarkerCell != null && templateMarkerCell.getCellType() == CellType.STRING
			        ) {
				String templateMarker = templateMarkerCell.getStringCellValue();
				if ("VFE".equals(templateMarker)) {
					vfeTemplate = true;
				}
				String translatedVfe = Translator.translateOrElseNull("VFE");
				if (translatedVfe != null && translatedVfe.equals(templateMarker)) {
					vfeTemplate = true;
				}
			}

			if (vfeTemplate) {
				String teamMembershipTitle = Translator.translateOrElseNull("TeamMembership.Title");
				int sourceColumnFromHeader = findColumn(headerRow, teamMembershipTitle);
				if (sourceColumnFromHeader >= 0) {
					listColumnVFEOffset = sourceColumnFromHeader - (listColumn - 1);
				} else {
					// Current VFE template keeps the split source list in column J.
					listColumnVFEOffset = 1;
				}
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
			if (vfeTemplate) {
				copySourceCellAcrossGeneratedColumns(sheet.getRow(6), sourceCol, listColumn + listColumnVFEOffset,
				        prefixes.size());
			}
			for (Row r : sheet) {
				if (r.getRowNum() < 7) {
					continue;
				}
				if (vfeTemplate && r.getRowNum() == 7) {
					copySourceCellAsMergedHeader(sheet, r, sourceCol, listColumn + listColumnVFEOffset,
					        prefixes.size());
					nonContentCounter = 0;
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
					CellStyle categoryStyle = catCell.getCellStyle();
					if (eligibleCatsString != null && !eligibleCatsString.isBlank()) {
						String[] eligibleCats = eligibleCatsString.split(";");
						
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
					} else {
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							int targetCol = listColumn + prefixOffset + listColumnVFEOffset;
							r.createCell(targetCol);
							r.getCell(targetCol).setCellStyle(categoryStyle);
						}
					}
					nonContentCounter = 0;
				} else {
					Cell endCell = r.getCell(listColumn - 1);
					Cell sourceCell = r.getCell(sourceCol);
					if (endCell != null && catCell != null) {
						CellStyle endCellStyle = endCell.getCellStyle();
						for (int prefixOffset = 0; prefixOffset < prefixes.size(); prefixOffset++) {
							int targetCol = listColumn + prefixOffset + listColumnVFEOffset;
							Cell targetCell = r.createCell(targetCol);
							if (vfeTemplate && sourceCell != null) {
								copyCellValueAndStyle(sourceCell, targetCell);
							} else {
								CellStyle categoryStyle = catCell.getCellStyle();
								targetCell.setCellStyle(categoryStyle);
							}
						}
						r.createCell(listColumn - 1 + prefixes.size() + listColumnVFEOffset);
						r.getCell(listColumn - 1 + prefixes.size() + listColumnVFEOffset).setCellStyle(endCellStyle);
					}
					nonContentCounter++;
				}
				if (nonContentCounter > 5) {
					break;
				}
			}
			sheet.setColumnHidden(sourceCol, true);

			int lastColumn = listColumn - 1 + prefixes.size() + listColumnVFEOffset;
			sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, lastColumn));
			w.setPrintArea(0, 0, lastColumn, 0, lastLine);
		});
	}

	private void copyCellValueAndStyle(Cell sourceCell, Cell targetCell) {
		targetCell.setCellStyle(sourceCell.getCellStyle());
		switch (sourceCell.getCellType()) {
		case STRING:
			targetCell.setCellValue(sourceCell.getStringCellValue());
			break;
		case NUMERIC:
			targetCell.setCellValue(sourceCell.getNumericCellValue());
			break;
		case BOOLEAN:
			targetCell.setCellValue(sourceCell.getBooleanCellValue());
			break;
		case FORMULA:
			targetCell.setCellFormula(sourceCell.getCellFormula());
			break;
		case ERROR:
			targetCell.setCellErrorValue(sourceCell.getErrorCellValue());
			break;
		case BLANK:
		case _NONE:
		default:
			break;
		}
	}

	private void copySourceCellAcrossGeneratedColumns(Row row, int sourceCol, int firstTargetCol, int columnCount) {
		if (row == null) {
			return;
		}
		Cell sourceCell = row.getCell(sourceCol);
		if (sourceCell == null) {
			return;
		}
		for (int offset = 0; offset < columnCount; offset++) {
			Cell targetCell = row.createCell(firstTargetCol + offset);
			copyCellValueAndStyle(sourceCell, targetCell);
		}
	}

	private void copySourceCellAsMergedHeader(Sheet sheet, Row row, int sourceCol, int firstTargetCol, int columnCount) {
		if (row == null) {
			return;
		}
		Cell sourceCell = row.getCell(sourceCol);
		if (sourceCell == null) {
			return;
		}
		for (int offset = 0; offset < columnCount; offset++) {
			Cell targetCell = row.createCell(firstTargetCol + offset);
			targetCell.setCellStyle(sourceCell.getCellStyle());
		}
		copyCellValueAndStyle(sourceCell, row.getCell(firstTargetCol));
		if (columnCount > 1) {
			sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), firstTargetCol,
			        firstTargetCol + columnCount - 1));
		}
	}

	private int findColumn(Row row, String expectedValue) {
		if (row == null || expectedValue == null) {
			return -1;
		}
		for (int column = row.getFirstCellNum(); column < row.getLastCellNum(); column++) {
			Cell cell = row.getCell(column);
			if (cell != null && cell.getCellType() == CellType.STRING
			        && expectedValue.equals(cell.getStringCellValue())) {
				return column;
			}
		}
		return -1;
	}

	public Consumer<Workbook> getPostProcessor() {
		return this.postProcessor;
	}

	@Override
	public boolean isEmptyOk() {
		return true;
	}

	// @Override
	// protected List<Athlete> getSortedAthletes() {
	// List<Athlete> registrationOrderCopy = AthleteSorter.registrationOrderCopy(sortedAthletes);
	// return registrationOrderCopy;
	// }

	public void setPostProcessor(Consumer<Workbook> postProcessor) {
		this.postProcessor = postProcessor;
	}

	@Override
	protected void postProcess(Workbook workbook) {
		createStandardFooter(workbook);
		if (this.postProcessor != null) {
			this.postProcessor.accept(workbook);
		}
		// fixMergeError(workbook);
	}
}
