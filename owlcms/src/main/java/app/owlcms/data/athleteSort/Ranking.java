/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athleteSort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import ch.qos.logback.classic.Logger;

/**
 * The Enum Ranking.
 */
public enum Ranking {
    // category values
	SNATCH("Sn", false),
	CLEANJERK("CJ", false),
	TOTAL("Tot", false),
	CUSTOM("Cus", false), // modified total / custom score (e.g. technical merit for kids competition)
	SNATCH_CJ_TOTAL("Combined", false), // sum of all three point scores
	CATEGORY_SCORE("SCORE", false), // copy of TOTAL, CUSTOM or any of the global scoring systems if used to award category medals

    // global scoring systems
	BW_SINCLAIR("Sinclair", true), // normal Sinclair
	SMM("Smm", true), // Legacy name, kept for import/export backward compatibility Sinclair Meltzer Huebner Faber
	CAT_SINCLAIR("CatSinclair", true), // legacy Quebec federation, Sinclair computed at category boundary

	QPOINTS("QPoints", true), // Huebner QPoints.
	QAGE("QMasters", true), // QPoints * SMHF age factors
	AGEFACTORS("QYouth", true),
	CAT_QPOINTS("CatQPoints", true), // QPoints computed at category boundary

	GAMX("GAMX", true), // GAMX 2.0 scoring
	GAMX_M("GAMX-M", true), // GAMX, age-adjusted for Masters
	GAMX_MS("GAMX-MS", true), // Placeholder variation of GAMX-M
	GAMX_MC("GAMX-MC", true), // Placeholder variation of GAMX-M
	GAMX_U("GAMX-U", true), // GAMX, age-adjusted for 7-17 years old
	GAMX_A("GAMX-A", true),// GAMX, age-adjusted for 13-40
	GAMX_S("GAMX-S", true), // Placeholder variation of GAMX (age ignored)
	GAMX_C("GAMX-C", true), // Placeholder variation of GAMX (age ignored)
	CAT_GAMX("CatGAMX", true), // GAMX computed at category boundary
	
	ROBI("Robi", true), // IWF ROBI
;

	public static Map<String, Ranking> rankingByReportingName = new HashMap<>();
	static {
		for (Ranking r : Ranking.values()) {
			rankingByReportingName.put(r.reportingName.toLowerCase(), r);
			rankingByReportingName.put(r.name().toLowerCase(), r);

		}
		rankingByReportingName.put("smhf", SMM);
	}
	static Logger logger = (Logger) LoggerFactory.getLogger(Ranking.class);

	public static String formatScoreboardRank(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited");
		} else {
			return total.toString();
		}
	}

	public static int getRanking(Athlete curLifter, Ranking rankingType) {
		Integer value = null;
		if (rankingType == null) {
			return 0;
		}
		if (!RankingConfig.shouldCompute(rankingType)) {
			return 0;
		}
		if (shouldHideIncompletePublishedScore(curLifter, rankingType)) {
			return 0;
		}
		switch (rankingType) {
			case SNATCH:
				value = curLifter.getSnatchRank();
				break;
			case CLEANJERK:
				value = curLifter.getCleanJerkRank();
				break;
			case TOTAL:
				value = curLifter.getTotalRank();
				break;
			case ROBI:
				value = curLifter.getRobiRank();
				break;
			case CUSTOM:
				value = curLifter.getCustomRank();
				break;
			case SNATCH_CJ_TOTAL:
				value = 0; // no such thing
				break;
			case BW_SINCLAIR:
				value = curLifter.getSinclairRank();
				break;
			case CAT_SINCLAIR:
				value = curLifter.getCatSinclairRank();
				break;
			case CAT_QPOINTS:
				value = curLifter.getCatQPointsRank();
				break;
			case CAT_GAMX:
				value = curLifter.getCatGAMXRank();
				break;
			case SMM:
				value = curLifter.getSmhfRank();
				break;
			case GAMX:
				value = curLifter.getGamxRank();
				break;
			case GAMX_M:
				value = curLifter.getGamxMRank();
				break;
			case GAMX_MS:
				value = curLifter.getGamxMSRank();
				break;
			case GAMX_MC:
				value = curLifter.getGamxMCRank();
				break;
			case GAMX_U:
				value = curLifter.getGamxURank();
				break;
			case GAMX_A:
				value = curLifter.getGamxARank();
				break;
			case GAMX_S:
				value = curLifter.getGamxSRank();
				break;
			case GAMX_C:
				value = curLifter.getGamxCRank();
				break;
			case QPOINTS:
				value = curLifter.getqPointsRank();
				break;
			case QAGE:
				value = curLifter.getQMastersRank();
				break;
			case AGEFACTORS:
				value = curLifter.getQYouthRank();
				break;
			case CATEGORY_SCORE:
				value = curLifter.getCategoryScoreRank();
				break;
		}
		return value == null ? 0 : value;
	}

	private static boolean shouldHideIncompletePublishedScore(Athlete curLifter, Ranking rankingType) {
		return curLifter != null
		        && JXLSWorkbookStreamSource.isNoInterimScoresInResults()
		        && !curLifter.isDone()
		        && switch (rankingType) {
		        case SNATCH, CLEANJERK, TOTAL, SNATCH_CJ_TOTAL -> false;
		        default -> true;
		        };
	}

	private static Ranking resolveRankingType(Athlete curLifter, Ranking rankingType) {
		if (rankingType == CATEGORY_SCORE) {
			return curLifter.getComputedScoringSystem();
		}
		return rankingType;
	}

	public static double getRankingValueForDelta(Athlete curLifter, Ranking rankingType) {
		if (rankingType == null) {
			return 0D;
		}
		if (!RankingConfig.shouldCompute(rankingType)) {
			return 0D;
		}
		Double d = 0D;
		Integer i = 0;
		rankingType = resolveRankingType(curLifter, rankingType);
		switch (rankingType) {
			case SNATCH:
				i = curLifter.getBestSnatch();
				d = i != null ? i.doubleValue() : null;
				break;
			case CLEANJERK:
				i = curLifter.getBestCleanJerk();
				d = i != null ? i.doubleValue() : null;
				break;
			case TOTAL:
				i = curLifter.getTotal();
				d = i != null ? i.doubleValue() : null;
				break;
			case ROBI:
				d = curLifter.getRobi();
				break;
			case CUSTOM:
				d = curLifter.getCustomScore();
				break;
			case SNATCH_CJ_TOTAL:
				d = 0D;
				break;
			case BW_SINCLAIR:
				d = curLifter.getSinclairForDelta();
				break;
			case CAT_SINCLAIR:
				d = curLifter.getCategorySinclairForDelta();
				break;
			case CAT_QPOINTS:
				d = curLifter.getCategoryQPointsForDelta();
				break;
			case SMM:
				d = curLifter.getSmhfForDelta();
				break;
			case GAMX:
				d = curLifter.getGamxForDelta();
				break;
			case GAMX_M:
				d = curLifter.getGamxMForDelta();
				break;
			case GAMX_MS:
				d = curLifter.getGamxMSForDelta();
				break;
			case GAMX_MC:
				d = curLifter.getGamxMCForDelta();
				break;
			case GAMX_U:
				d = curLifter.getGamxUForDelta();
				break;
			case GAMX_A:
				d = curLifter.getGamxAForDelta();
				break;
			case GAMX_S:
				d = curLifter.getGamxSForDelta();
				break;
			case GAMX_C:
				d = curLifter.getGamxCForDelta();
				break;
			case CAT_GAMX:
				d = curLifter.getCategoryGAMXForDelta();
				break;
			case AGEFACTORS:
				d = curLifter.getQYouthForDelta();
				break;
			case QPOINTS:
				d = curLifter.getQPointsForDelta();
				break;
			case QAGE:
				d = curLifter.getQMastersForDelta();
				break;
			case CATEGORY_SCORE:
				throw new RuntimeException("can't happen, CATEGORY_SCORE loop");
		}
		return d != null ? d : 0D;
	}

	/**
	 * @param curLifter
	 * @param rankingType
	 * @return
	 */
	public static double getRankingValue(Athlete curLifter, Ranking rankingType) {
		if (rankingType == null) {
			return 0D;
		}
		if (!RankingConfig.shouldCompute(rankingType)) {
			return 0D;
		}
		Ranking originalRankingType = rankingType;
		if (shouldHideIncompletePublishedScore(curLifter, originalRankingType)) {
			return 0D;
		}
		Double d = 0D;
		Integer i = 0;
		rankingType = resolveRankingType(curLifter, rankingType);
		switch (rankingType) {
			case SNATCH:
				i = curLifter.getBestSnatch();
				d = i != null ? i.doubleValue() : null;
				break;
			case CLEANJERK:
				i = curLifter.getBestCleanJerk();
				d = i != null ? i.doubleValue() : null;
				break;
			case TOTAL:
				i = curLifter.getTotal();
				d = i != null ? i.doubleValue() : null;
				break;
			case ROBI:
				d = curLifter.getRobi();
				break;
			case CUSTOM:
				d = curLifter.getCustomScore();
				break;
			case SNATCH_CJ_TOTAL:
				d = 0D; // no such thing
				break;
			case BW_SINCLAIR:
				d = curLifter.getSinclair();
				break;
			case CAT_SINCLAIR:
				d = curLifter.getCategorySinclair();
				break;
			case CAT_QPOINTS:
				d = curLifter.getCategoryQPoints();
				break;
			case SMM:
				d = curLifter.getSmhf();
				break;
			case GAMX:
				d = curLifter.getGamx();
				break;
			case GAMX_M:
				d = curLifter.getGamxM();
				break;
			case GAMX_MS:
				d = curLifter.getGamxMS();
				break;
			case GAMX_MC:
				d = curLifter.getGamxMC();
				break;
			case GAMX_U:
				d = curLifter.getGamxU();
				break;
			case GAMX_A:
				d = curLifter.getGamxA();
				break;
			case GAMX_S:
				d = curLifter.getGamxS();
				break;
			case GAMX_C:
				d = curLifter.getGamxC();
				break;
			case CAT_GAMX:
				d = curLifter.getCategoryGAMX();
				break;
			case AGEFACTORS:
				d = curLifter.getQYouth();
				break;
			case QPOINTS:
				d = curLifter.getQPoints();
				break;
			case QAGE:
				d = curLifter.getQMasters();
				break;
			case CATEGORY_SCORE:
				throw new RuntimeException("can't happen, CATEGORY_SCORE loop");
//				if (JXLSWorkbookStreamSource.isNoInterimScoresInResults()) {
//					d = curLifter.getCategoryScoreForDelta();
//				} else {
//					d = curLifter.getCategoryScore();
//				}
//				break;
		}
		return d != null ? d : 0D;
	}

	public static String getScoringExplanation(Ranking rankingType) {
		if (rankingType == null || rankingType == Ranking.CATEGORY_SCORE) {
			return Translator.translate("Score");
		}
		switch (rankingType) {
			case ROBI:
			case CUSTOM:
			case BW_SINCLAIR:
			case CAT_SINCLAIR:
			case CAT_QPOINTS:
			case CAT_GAMX:
			case SMM:
			case GAMX:
			case GAMX_M:
			case GAMX_MS:
			case GAMX_MC:
			case GAMX_U:
			case GAMX_A:
			case GAMX_S:
			case GAMX_C:
			case QPOINTS:
			case AGEFACTORS:
			case QAGE:
			case TOTAL:
				return switch (rankingType) {
					case GAMX_MS, GAMX_MC, GAMX_S, GAMX_C -> rankingType.getReportingName();
					default -> Translator.translate("RankingExplanation." + rankingType);
				};
			default:
				throw new UnsupportedOperationException("not a score ranking " + rankingType);
		}
	}

	public static String getScoringTitle(Ranking rankingType) {
		if (rankingType == null || rankingType == Ranking.CATEGORY_SCORE) {
			return Translator.translate("Score");
		}
		switch (rankingType) {
			case ROBI:
			case CUSTOM:
			case BW_SINCLAIR:
			case CAT_SINCLAIR:
			case CAT_QPOINTS:
			case CAT_GAMX:
			case SMM:
			case GAMX:
			case GAMX_M:
			case GAMX_MS:
			case GAMX_MC:
			case GAMX_U:
			case GAMX_A:
			case GAMX_S:
			case GAMX_C:
			case QPOINTS:
			case AGEFACTORS:
			case QAGE:
			case TOTAL:
				return switch (rankingType) {
					case GAMX_MS, GAMX_MC, GAMX_S, GAMX_C -> rankingType.getReportingName();
					default -> Translator.translate("Ranking." + rankingType);
				};
			default:
				throw new UnsupportedOperationException("not a score ranking " + rankingType);
		}
	}

	public static List<Ranking> scoringSystems() {
		List<Ranking> systems = new ArrayList<>(Arrays.asList(BW_SINCLAIR, SMM, ROBI, AGEFACTORS, QPOINTS, QAGE, GAMX, GAMX_M, GAMX_MS, GAMX_MC, GAMX_U, GAMX_A, GAMX_S, GAMX_C, CAT_QPOINTS, CAT_GAMX, CAT_SINCLAIR));
		systems.removeIf(ranking -> !RankingConfig.shouldCompute(ranking));
		return systems;
	}

	private String reportingName;
	private boolean medalScore;

	/**
	 * @param medalScore
	 * @param reportingInfoName the name of the beans used for Excel reporting
	 */
	Ranking(String reportingName, boolean medalScore) {
		this.reportingName = reportingName;
		this.medalScore = medalScore;
	}

	public String getMReportingName() {
		return "m" + this.reportingName;
	}

	public String getMWReportingName() {
		return "mw" + this.reportingName;
	}

	public String getWReportingName() {
		return "w" + this.reportingName;
	}

	public boolean isMedalScore() {
		return this.medalScore;
	}

	public void setMedalScore(boolean medalScore) {
		this.medalScore = medalScore;
	}

	public String getReportingName() {
		return reportingName;
	}

	public void setReportingName(String reportingName) {
		this.reportingName = reportingName;
	}

}