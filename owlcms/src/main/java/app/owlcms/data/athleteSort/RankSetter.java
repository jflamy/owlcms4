package app.owlcms.data.athleteSort;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;

public class RankSetter {

    public int rank = 0;
    public int jrRank = 0;
    public int ythRank = 0;
    public int srRank = 0;

    public void increment(Athlete a, Ranking r, boolean eligible, boolean zero) {
        int age;
        try {
            age = a.getAge();
        } catch (Exception e) {
            // if no age, rank as senior.
            // defensive, should not happen.
            age = 21;
        }

        switch (r) {
        case SNATCH:
            if (age <= 17) {
                a.setSnatchRankYth(eligible ? (zero ? 0 : ++ythRank) : -1);
            }
            if (age >= 15 && age <= 20) {
                a.setSnatchRankJr(eligible ? (zero ? 0 : ++jrRank) : -1);
            }
            if (age >= 15) {
                a.setSnatchRankSr(eligible ? (zero ? 0 : ++srRank) : -1);
            }
            a.setSnatchRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CLEANJERK:
            if (age <= 17) {
                a.setCleanJerkRankYth(eligible ? (zero ? 0 : ++ythRank) : -1);
            }
            if (age >= 15 && age <= 20) {
                a.setCleanJerkRankJr(eligible ? (zero ? 0 : ++jrRank) : -1);
            }
            if (age >= 15) {
                a.setCleanJerkRankSr(eligible ? (zero ? 0 : ++srRank) : -1);
            }
            a.setCleanJerkRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case TOTAL:
            if (age <= 17) {
                a.setTotalRankYth(eligible ? (zero ? 0 : ++ythRank) : -1);
            }
            if (age >= 15 && age <= 20) {
                a.setTotalRankJr(eligible ? (zero ? 0 : ++jrRank) : -1);
            }
            if (age >= 15) {
                a.setTotalRankSr(eligible ? (zero ? 0 : ++srRank) : -1);
            }
            a.setTotalRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case BW_SINCLAIR:
            a.setSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CAT_SINCLAIR:
            a.setCatSinclairRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;

        case COMBINED:
            a.setCombinedRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case CUSTOM:
            a.setCustomRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case ROBI:
            a.setRobiRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;
        case SMM:
            a.setSmmRank(eligible ? (zero ? 0 : ++rank) : -1);
            break;

        }
    }

}
