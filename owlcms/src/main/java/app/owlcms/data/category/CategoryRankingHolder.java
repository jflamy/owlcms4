package app.owlcms.data.category;

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
