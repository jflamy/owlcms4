package app.owlcms.spreadsheet;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.i18n.Translator;

public class MAthlete extends PAthlete {

	public static class MedalComparator implements Comparator<MAthlete> {

		@Override
		public int compare(MAthlete o1, MAthlete o2) {
			int compare;

			compare = ObjectUtils.compare(o1.getCategory(), o2.getCategory(), false);
			if (compare != 0) {
				return compare;
			}

			compare = ObjectUtils.compare(o1.getRanking(), o2.getRanking(), false);
			if (compare != 0) {
				return compare;
			}

			// bronze first
			compare = ObjectUtils.compare(o1.getLiftRank(), o2.getLiftRank(), false);
			if (compare != 0) {
				return -compare;
			}

			return 0;
		}

	}

	static boolean winsMedal(PAthlete p, Ranking r) {
		Integer rank = AthleteSorter.getRank(p, r);
		return rank >= 1 && rank <= 3;
	}

	private int liftRank;

	private int liftResult;

	private Ranking ranking;

	public MAthlete(PAthlete p, Ranking r, int rank, Integer result) {
		super(p._getParticipation());
		this.setRanking(r);
		this.setLiftRank(rank);
		this.setLiftResult(result == null ? 0 : result);
	}

	public int getLiftRank() {
		return liftRank;
	}

	public int getLiftResult() {
		return liftResult;
	}

	public Ranking getRanking() {
		return ranking;
	}

	public String getRankingText() {
		switch (ranking) {
		case CLEANJERK:
			return Translator.translate("Clean_and_Jerk");
		case SNATCH:
			return Translator.translate("Snatch");
		case TOTAL:
			return Translator.translate("Total");
		default:
			return ranking.name();
		}
	}

	public void setLiftResult(int liftResult) {
		this.liftResult = liftResult;
	}

	public void setRanking(Ranking ranking) {
		this.ranking = ranking;
	}

	public void setRankingText() {
	}

	private void setLiftRank(int catMedalRank) {
		this.liftRank = catMedalRank;
	}

}
