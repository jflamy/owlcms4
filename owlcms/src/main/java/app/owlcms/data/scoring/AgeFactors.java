/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.io.InputStream;

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
public class AgeFactors {

	private static final int STARTING_AGE = 8;
	private static final int STARTING_BW = 30;
	private static final int NB_AGE = 20-STARTING_AGE+1;
	private static final int NB_BW = 115-STARTING_BW+1;
	final static float[][][] z = new float[2][NB_BW][NB_AGE];
	private static boolean loaded = false;
	static Logger logger = (Logger) LoggerFactory.getLogger(AgeFactors.class);

	public static float doGetScore(Gender gender, Double dBW, Integer age, Integer liftedWeight) {
		if (gender == null || dBW == null || age == null) {
			return 0.0F;
		}

		dBW = Math.max(STARTING_BW, Math.min(190.0D, dBW));
		float floorScore = zCoefficient(gender, (int) Math.floor(dBW), age) * liftedWeight;
		float ceilingScore = zCoefficient(gender, (int) Math.ceil(dBW), age)  * liftedWeight;
		double interpolated = floorScore + ((dBW - Math.floor(dBW)) * (ceilingScore - floorScore));

//		logger.debug("doGetScore gender={} bw={} total={} floor={} ceil={} score={}", gender, dBW, liftedWeight,
//		        floorScore,
//		        ceilingScore, interpolated);

		return (float) interpolated;
	}

	public static float getAgeAdjustedTotal(Athlete a, Integer liftedWeight) {
		if (liftedWeight == null || a == null || a.getAge() == null) {
			return 0.0F;
		}
		
		Gender gender = a.getGender();
		Double dBW = a.getBodyWeight();
		Integer age = a.getAge();
		if (age < 8) {
			age = 8;
		}
		if (dBW < 30.0) {
			dBW = 30.0;
		}
		return doGetScore(gender, dBW, age, liftedWeight);
	}

	public static int kgTarget(Gender gender, double targetScore, double bw, int age) {
		var coef = zCoefficient(gender, (int)bw, age);
		return (int) Math.ceil(targetScore / coef);
	}

	public static float zCoefficient(Gender gender, int bw, Integer age) {
		loadCoefficients();
		int row = bw - STARTING_BW;
		int column = age - STARTING_AGE;
		float zCoeff;
		try {
			zCoeff = z[gender.ordinal()][row][column];
		} catch (IndexOutOfBoundsException e) {
			zCoeff = 1.0F;
		}
		return zCoeff;
	}

	/**
	 *
	 */
	private static void loadCoefficients() {
		if (loaded) {
			return;
		}
		String name = "/ageFactors/ageFactors.xlsx";
		try {
			InputStream stream = ResourceWalker.getResourceAsStream(name);
			try (Workbook workbook = new XSSFWorkbook(stream)) {

				for (int sheetindex = 0; sheetindex <= 1; sheetindex++) {

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
								z[gender.ordinal()][rowNum - 1][cellNum - 2] = coeff;

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

	public AgeFactors() {
	}

}
