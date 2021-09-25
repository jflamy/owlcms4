/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

/**
 * Used to tally category-specific rankings.
 * 
 * @author Jean-François Lamy
 *
 */
public class CategoryRankingHolder {
    public int getSnatchRank() {
        return snatchRank;
    }
    public void setSnatchRank(int snatchRank) {
        this.snatchRank = snatchRank;
    }
    public int getCleanJerkRank() {
        return cleanJerkRank;
    }
    public void setCleanJerkRank(int cleanJerkRank) {
        this.cleanJerkRank = cleanJerkRank;
    }
    public int getTotalRank() {
        return totalRank;
    }
    public void setTotalRank(int totalRank) {
        this.totalRank = totalRank;
    }
    private int snatchRank = 0;
    private int cleanJerkRank = 0;
    private int totalRank = 0;

}
