package app.owlcms.data.athleteSort;

/**
 * The Enum Ranking.
 */
public enum Ranking {
    SNATCH, CLEANJERK, TOTAL,
    /** combined (men + women). */
    SNATCH_CJ_TOTAL,
    CAT_SINCLAIR, // legacy Quebec federation, Sinclair computed at category boundary
    BW_SINCLAIR, // normal sinclair
    SMM, // Sinclair Malone-Meltzer
    ROBI, // IWF ROBI
    CUSTOM // custom score (e.g. technical merit for kids competition)

}