package app.owlcms.data.athlete;

/**
 * Indicates why an athlete is not eligible for individual ranking, or null if eligible.
 */
public enum EligibleForIndividualRankingStatus {
    OOC_INVITED(ParticipationStatus.OOC),
    OOC_ADMINISTRATIVE(ParticipationStatus.OOC),
    OOC_QUALIFICATION(ParticipationStatus.OOC),
    OOC_OUT_OF_AGE_RANGE(ParticipationStatus.OOC),
    OOC_OTHER(ParticipationStatus.OOC),
    OOC_DID_NOT_MAKE_WEIGHT(ParticipationStatus.OOC),
    DSQ_DOPING(ParticipationStatus.DSQ),
    DSQ_DISCIPLINARY(ParticipationStatus.DSQ),
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
