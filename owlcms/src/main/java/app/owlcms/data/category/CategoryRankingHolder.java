/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
    protected int snatchRank = 0;
    protected int cleanJerkRank = 0;
    protected int totalRank = 0;
    protected int customRank = 0;
    protected int combinedRank = 0;

    public int getCleanJerkRank() {
        return cleanJerkRank;
    }

    public int getCombinedRank() {
        return combinedRank;
    }

    public int getCustomRank() {
        return customRank;
    }

    public int getSnatchRank() {
        return snatchRank;
    }

    public int getTotalRank() {
        return totalRank;
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
