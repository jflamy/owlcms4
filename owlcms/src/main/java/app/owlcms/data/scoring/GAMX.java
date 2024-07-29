/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 *
 * Compute Q Points according to https://osf.io/8x3nb/ (formulas in https://osf.io/download/r2gxa/ and
 * https://osf.io/download/bmctw/)
 *
 * This class keeps the code for applying an age factor like SMF/SMHF even though this has not been discussed.
 */
public class GAMX {

	private static final int STARTING_TOTAL = 40;
	private static final int STARTING_BW = 35;
	private static final int NB_TOT = 461;
	private static final int NB_BW = 156;
	public static final int M_CONSTANT = 350;
	public static final int SD_CONSTANT = 50;
	private final static float[][][] z = new float[2][NB_BW][NB_TOT];
	private final static float[][][] gamx = new float[2][NB_BW][NB_TOT];
	private static boolean loaded = false;
	static Logger logger = (Logger) LoggerFactory.getLogger(GAMX.class);

	public static float doGetGamx(Gender gender, Double dBW, Integer liftedWeight) {
		if (gender == null || dBW == null) {
			return 0.0F;
		}

		dBW = Math.max(STARTING_BW, Math.min(190.0D, dBW));
		float floorScore = M_CONSTANT + zCoefficient(gender, (int) Math.floor(dBW), liftedWeight) * SD_CONSTANT;
		float ceilingScore = M_CONSTANT + zCoefficient(gender, (int) Math.ceil(dBW), liftedWeight) * SD_CONSTANT;
		double interpolated = floorScore + ((dBW - Math.floor(dBW)) * (ceilingScore - floorScore));

//		logger.debug("doGetGamx gender={} bw={} total={} floor={} ceil={} score={}", gender, dBW, liftedWeight,
//		        floorScore,
//		        ceilingScore, interpolated);

		return (float) interpolated;
	}

	public static float getGamx(Athlete a, Integer liftedWeight) {
		if (liftedWeight == null) {
			return 0.0F;
		}
		Gender gender = a.getGender();
		Double dBW = a.getBodyWeight();
		return doGetGamx(gender, dBW, liftedWeight);
	}

	public static int kgTarget(Gender gender, double targetScore, double bw) {
		int floorBW = (int) Math.floor(bw) - STARTING_BW;

		// search the target score in the weight row of the chasing athlete
		int floorWeightIndex = Arrays.binarySearch(gamx[gender.ordinal()][floorBW], (float) targetScore);
		if (floorWeightIndex < 0) {
			// no exact match, negative value, one too far for our purpose
			floorWeightIndex = -floorWeightIndex - 1;
		} else {
			// floorfloorWeightIndex is exact hit (unlikely given float).
		}

		try {
			// logger.debug("{} binary {} {}<{}<{}", floorBW + STARTING_BW, floorWeightIndex + STARTING_TOTAL,
			// gamx[gender.ordinal()][floorBW][floorWeightIndex - 1],
			// targetScore, gamx[gender.ordinal()][floorBW][floorWeightIndex]);

			int returnValue = floorWeightIndex + STARTING_TOTAL;
			var score = doGetGamx(gender, bw, returnValue);
			// logger.debug("bw={} total={} score={} targetScore={}", bw, returnValue, score, targetScore);
			while (score < targetScore) {
				returnValue++;
				score = doGetGamx(gender, bw, returnValue);
				// logger.debug("bw={} total={} score={} targetScore={}", bw, returnValue, score, targetScore);
			}
			return returnValue;
		} catch (Exception e) {
			logger.warn/**/("kgTarget IMPOSSIBLE {} {} {}", gender, targetScore, bw);
			return 0;
		}

	}

	public static float zCoefficient(Gender gender, int bw, Integer liftedWeight) {
		loadCoefficients();
		float zCoeff = z[gender.ordinal()][bw - STARTING_BW][liftedWeight - STARTING_TOTAL];
		return zCoeff;
	}

	/**
	 *
	 */
	private static void loadCoefficients() {
		if (loaded) {
			return;
		}
		String name = "/gamx/gamx.xlsx";
		try {
			InputStream stream = ResourceWalker.getResourceAsStream(name);
			try (Workbook workbook = new XSSFWorkbook(stream)) {

				for (int sheetindex = 1; sheetindex <= 2; sheetindex++) {

					Sheet sheet = workbook.getSheetAt(sheetindex);
					for (Row row : sheet) {
						int rowNum = row.getRowNum();
						if (rowNum == 0) {
							// skip header
							continue;
						}

						Gender gender = null;
						for (Cell cell : row) {
							int cellNum = cell.getColumnIndex();
							if (cellNum == 0) {
								gender = Gender.valueOf(cell.getStringCellValue().toUpperCase());
							}
							if (cellNum <= 1) {
								continue;
							}

							try {
								float coeff = (float) cell.getNumericCellValue();
								float gamxEquiv;
								if (Math.abs(coeff) <= 0.001) {
									gamxEquiv = 1000.0F; // any large value
								} else {
									gamxEquiv = (M_CONSTANT + SD_CONSTANT * coeff);
								}

								z[gender.ordinal()][rowNum - 1][cellNum - 2] = coeff;
								gamx[gender.ordinal()][rowNum - 1][cellNum - 2] = gamxEquiv;

								// logger.trace("z[{}][{}][{}] = {} => {}", gender.ordinal(), rowNum - 1, cellNum - 2,
								// coeff, equivKg);
							} catch (Exception e) {
								logger.error("{}[{}] {}", sheet.getSheetName(), cell.getAddress(), e);
							}
						}
					}
				}
			}
			loaded = true;
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
	}

	public GAMX() {
	}

	public float doGetZCoefficient(Gender gender, Double dBW, Float score) {
		if (gender == null || dBW == null) {
			return 0.0F;
		}
		float coefficient = (score - M_CONSTANT) / SD_CONSTANT;
		return coefficient;
	}

}
