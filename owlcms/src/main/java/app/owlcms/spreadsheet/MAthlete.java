/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.Comparator;

import org.apache.commons.lang3.ObjectUtils;

import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RegistrationOrderComparator;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;

public class MAthlete extends PAthlete {

	public static class MedalComparator implements Comparator<MAthlete> {
		private final boolean mastersAgeGroupOrder;

		public MedalComparator() {
			this(null);
		}

		public MedalComparator(Group group) {
			this.mastersAgeGroupOrder = RegistrationOrderComparator.useMastersAgeGroupOrder(group);
		}

		@Override
		public int compare(MAthlete o1, MAthlete o2) {
			int compare;

			compare = RegistrationOrderComparator.compareMastersAgeGroupOrder(o1, o2, this.mastersAgeGroupOrder);
			if (compare != 0) {
				return compare;
			}

			compare = ObjectUtils.compare(o1.getCategory().getMedalingSortCode(), o2.getCategory().getMedalingSortCode(), false);
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
	private Double liftValue;
	private Ranking ranking;

	public MAthlete(PAthlete p, Ranking r, int rank, Double result) {
		super(p._getParticipation());
		this.setRanking(r);
		this.setLiftRank(rank);
		this.setLiftResult(result == null ? 0 : result);
	}

	public int getLiftRank() {
		return this.liftRank;
	}

	public String getLiftResult() {
		switch (this.ranking) {
			case CLEANJERK:
			case SNATCH:
			case TOTAL:
				int roundedValue = (int) Math.round(this.liftValue);
				return String.valueOf(roundedValue);
			default:
				return String.format("%.3f", this.liftValue);
		}
	}

	public double getLiftValue() {
		return this.liftValue;
	}

	public String getMedalingSortCode() {
		return getCategory().getMedalingSortCode();
	}

	public Ranking getRanking() {
		return this.ranking;
	}

	public String getRankingText() {
		switch (this.ranking) {
			case CLEANJERK:
				return Translator.translate("Clean_and_Jerk");
			case SNATCH:
				return Translator.translate("Snatch");
			case TOTAL:
				return Translator.translate("Total");
			default:
				return Ranking.getScoringTitle(this.ranking);
		}
	}

	public void setLiftResult() {
		// unused
	}

	public void setLiftResult(double d) {
		this.liftValue = d;
	}

	public void setMedalingSortCode(String unused) {
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
