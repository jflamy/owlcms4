/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

/**
 * Used to compute category-specific rankings.
 *
 * @author Jean-François Lamy
 *
 */
public class CategoryRankingHolder implements IRankHolder {
	protected int cleanJerkRank = 0;
	protected int combinedRank = 0;
	protected int customRank = 0;
	protected int snatchRank = 0;
	protected int totalRank = 0;
	protected int categoryScoreRank = 0;
	protected double lastCategoryScore = 0.0D;

	public int getCategoryScoreRank() {
		return this.categoryScoreRank;
	}

	public double getLastCategoryScore() {
		return this.lastCategoryScore;
	}

	public int getCleanJerkRank() {
		return this.cleanJerkRank;
	}

	public int getCombinedRank() {
		return this.combinedRank;
	}

	public int getCustomRank() {
		return this.customRank;
	}

	public int getSnatchRank() {
		return this.snatchRank;
	}

	public int getTotalRank() {
		return this.totalRank;
	}

	public void setCategoryScoreRank(int scoreRank) {
		this.categoryScoreRank = scoreRank;
	}

	public void setLastCategoryScore(double lastCategoryScore) {
		this.lastCategoryScore = lastCategoryScore;
	}

	public void setCleanJerkRank(int cleanJerkRank) {
		this.cleanJerkRank = cleanJerkRank;
	}

	public void setCombinedRank(int combinedRank) {
		this.combinedRank = combinedRank;
	}

	public void setCustomRank(int customRank) {
		this.customRank = customRank;
	}

	public void setSnatchRank(int snatchRank) {
		this.snatchRank = snatchRank;
	}

	public void setTotalRank(int totalRank) {
		this.totalRank = totalRank;
	}
}
