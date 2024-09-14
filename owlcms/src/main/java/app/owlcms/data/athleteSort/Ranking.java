package app.owlcms.data.athleteSort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * The Enum Ranking.
 */
public enum Ranking {	
	SNATCH("Sn"),
	CLEANJERK("CJ"),
	TOTAL("Tot"),
	CUSTOM("Cus"), // modified total / custom score (e.g. technical merit for kids competition)
	SNATCH_CJ_TOTAL("Combined"), // sum of all three point scores

	BW_SINCLAIR("Sinclair"), // normal Sinclair
	CAT_SINCLAIR("CatSinclair"), // legacy Quebec federation, Sinclair computed at category boundary
	SMM("Smm"), // Legacy name, kept for import/export backward compatibility Sinclair Meltzer Huebner Faber
	ROBI("Robi"), // IWF ROBI
	QPOINTS("QPoints"), // Huebner QPoints.
	GAMX("GAMX"), // Global Adjusted Mixed (Huebner)
	AGEFACTORS("QYouth"),
	QAGE("QAge") // QPoints * SMHF age factors
	;
	
	static Logger logger = (Logger) LoggerFactory.getLogger(Ranking.class);

	public static int getRanking(Athlete curLifter, Ranking rankingType) {
		Integer value = null;
		if (rankingType == null) {
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
			case SMM:
				value = curLifter.getSmhfRank();
				break;
			case GAMX:
				value = curLifter.getGamxRank();
				break;
			case QPOINTS:
				value = curLifter.getqPointsRank();
				break;
			case QAGE:
				value = curLifter.getqAgeRank();
				break;
			case AGEFACTORS:
				value = curLifter.getAgeAdjustedTotalRank();
				break;
		}
		//logger.debug("{} ranking value: {}", curLifter.getShortName(), value);
		return value == null ? 0 : value;
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
				return curLifter.getSmhfForDelta();
			case GAMX:
				return curLifter.getGamx();
			case AGEFACTORS:
				return curLifter.getAgeAdjustedTotal();
			case QPOINTS:
				return curLifter.getQPoints();
			case QAGE:
				return curLifter.getQAge();
		}
		return 0D;
	}

	public static String getScoringTitle(Ranking rankingType) {
		if (rankingType == null) {
			return Translator.translate("Score");
		}
		switch (rankingType) {
			case ROBI:
			case CUSTOM:
			case BW_SINCLAIR:
			case CAT_SINCLAIR:
			case SMM:
			case GAMX:
			case QPOINTS:
			case AGEFACTORS:
			case QAGE:
				return Translator.translate("Ranking." + rankingType);
			default:
				throw new UnsupportedOperationException("not a score ranking " + rankingType);
		}
	}
	
	public static String getScoringExplanation(Ranking rankingType) {
		if (rankingType == null) {
			return Translator.translate("Score");
		}
		switch (rankingType) {
			case ROBI:
			case CUSTOM:
			case BW_SINCLAIR:
			case CAT_SINCLAIR:
			case SMM:
			case GAMX:
			case QPOINTS:
			case AGEFACTORS:
			case QAGE:
				return Translator.translate("RankingExplanation." + rankingType);
			default:
				throw new UnsupportedOperationException("not a score ranking " + rankingType);
		}
	}

	public static List<Ranking> scoringSystems() {
		List<Ranking> systems = new ArrayList<>(Arrays.asList(BW_SINCLAIR, SMM, ROBI, AGEFACTORS, QPOINTS, QAGE, GAMX, CAT_SINCLAIR));
		return systems;
	}

	private String reportingName;

	/**
	 * @param reportingInfoName the name of the beans used for Excel reporting
	 */
	Ranking(String reportingName) {
		this.reportingName = reportingName;
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

}