package app.owlcms.data.athlete;

/**
 * Indicates why an athlete is not eligible for individual ranking, or null if eligible.
 */
public enum EligibleForIndividualRankingStatus {
    OOC_INVITED(ParticipationStatus.OOC),
    OOC_DID_NOT_MAKE_WEIGHT(ParticipationStatus.OOC),
    OOC_OUT_OF_AGE_RANGE(ParticipationStatus.OOC),
    /**
     * Athlete finished below the qualifying total captured for the category. This denies the <b>total</b> medal only
     * (and total-derived score medals); the individual snatch and clean&amp;jerk lift medals are still awarded. Applies
     * to all federations that capture a qualifying total, independently of the IMWA (Masters) flag.
     */
    OOC_QUALIFICATION(ParticipationStatus.OOC),
    OOC_ADMINISTRATIVE(ParticipationStatus.OOC),
    DSQ_DOPING(ParticipationStatus.DSQ),
    DSQ_DISCIPLINARY(ParticipationStatus.DSQ),
    OOC_OTHER(ParticipationStatus.OOC),
    ELIGIBLE(ParticipationStatus.INCLUSION);

    public enum ParticipationStatus {
        OOC, // Out Of Competition / out-of-classification reasons
        DSQ, // Disqualification reasons
        INCLUSION // Explicit inclusion reasons (e.g., ELIGIBLE)
    }

    private final ParticipationStatus type;

    EligibleForIndividualRankingStatus(ParticipationStatus type) {
        this.type = type;
    }

    public ParticipationStatus getType() {
        return type;
    }

    public boolean isDisqualification() {
        return type == ParticipationStatus.DSQ;
    }

    public boolean isOutOfCompetition() {
        return type == ParticipationStatus.OOC;
    }

    public boolean isInclusion() {
        return type == ParticipationStatus.INCLUSION;
    }

    @Override
    public String toString() {
        return name();
    }
}
