package app.owlcms.data.athleteSort;

import java.util.Arrays;
import java.util.List;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;

/**
 * The Enum Ranking.
 */
public enum Ranking {
	SNATCH, CLEANJERK, TOTAL,
	SNATCH_CJ_TOTAL, // sum of all three point scores
	CAT_SINCLAIR, // legacy Quebec federation, Sinclair computed at category boundary
	BW_SINCLAIR, // normal sinclair
	SMM, // Sinclair Malone-Meltzer
	ROBI, // IWF ROBI
	CUSTOM, // custom score (e.g. technical merit for kids competition)
	QPOINTS,  // Huebner QPoints.
	HSR; // Huebner Scaled Results
	
	
	public static List<Ranking> scoringSystems() {	
		return Arrays.asList(BW_SINCLAIR, SMM, ROBI, QPOINTS, CAT_SINCLAIR, CUSTOM);
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
		switch (rankingType) {
			case SNATCH:
				return curLifter.getBestSnatch();
			case CLEANJERK:
				return curLifter.getBestCleanJerk();
			case TOTAL:
				return curLifter.getTotal();
			case ROBI:
				return curLifter.getRobi();
			case CUSTOM:
				return curLifter.getCustomScoreComputed();
			case SNATCH_CJ_TOTAL:
				return 0D; // no such thing
			case BW_SINCLAIR:
				return curLifter.getSinclairForDelta();
			case CAT_SINCLAIR:
				return curLifter.getCategorySinclair();
			case SMM:
				return curLifter.getSmfForDelta();
			case HSR:
				return curLifter.getHSR();
			case QPOINTS:
				return curLifter.getQPoints();
		}
		return 0D;
	}

	public static int getRanking(Athlete curLifter, Ranking rankingType) {
		Integer value = null;
		if (rankingType == null) {
			return 0;
		}
		switch (rankingType) {
			case SNATCH:
				value = curLifter.getSnatchRank();
			case CLEANJERK:
				value = curLifter.getCleanJerkRank();
			case TOTAL:
				value = curLifter.getTotalRank();
			case ROBI:
				value = curLifter.getRobiRank();
			case CUSTOM:
				value = curLifter.getCustomRank();
			case SNATCH_CJ_TOTAL:
				value = 0; // no such thing
			case BW_SINCLAIR:
				value = curLifter.getSinclairRank();
			case CAT_SINCLAIR:
				value = curLifter.getCatSinclairRank();
			case SMM:
				value = curLifter.getSmmRank();
			case HSR:
				value = curLifter.getHSRRank();
			case QPOINTS:
				value = curLifter.getqPointsRank();
		}
		return value == null ? 0 : value;
	}
	
	public static String getScoringTitle(Ranking rankingType) {
		if (rankingType == null) {
			return Translator.translate("Ranking.SINCLAIR");
		}
		switch (rankingType) {
			case ROBI:
			case CUSTOM:
			case BW_SINCLAIR:
			case CAT_SINCLAIR:
			case SMM:
			case HSR:
			case QPOINTS:
				return Translator.translate("Ranking."+rankingType);
			default:
				throw new UnsupportedOperationException("not a score ranking "+rankingType);
		}
	}

}