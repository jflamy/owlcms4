/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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
public class CategoryRankingHolder {
    protected int snatchRank = 0;
    protected int cleanJerkRank = 0;
    protected int totalRank = 0;
    protected int customRank = 0;

    public int getCustomRank() {
        return customRank;
    }

    public void setCustomRank(int customRank) {
        this.customRank = customRank;
    }

    public int getCleanJerkRank() {
        return cleanJerkRank;
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

    public void setSnatchRank(int snatchRank) {
        this.snatchRank = snatchRank;
    }

    public void setTotalRank(int totalRank) {
        this.totalRank = totalRank;
    }

}
