package app.owlcms.data.technicalofficial;

public enum OverallRole {
    COMPETITION_DIRECTOR("CompetitionDirector", "CompetitionDirector"),
    COMPETITION_SECRETARY("CompetitionSecretary", "CompetitionSecretary"),
    COMPETITION_SECRETARY2("CompetitionSecretary2", "CompetitionSecretary2"),
    TECHNICAL_DELEGATE("TechnicalDelegate", "TechnicalDelegate"),
    FEDERATION("Competition.federationTitle", "Competition.federationTitle"),
    VIP("VIP", "VIP"),
    VOLUNTEER("Volunteer", "Volunteer"),
    TECHNICAL_OFFICIAL("TechnicalOfficial", "TechnicalOfficial"),
    TECHNOLOGY_INFORMATION("Technology", "Technology");

    private final String assignmentKey;
    private final String introductionKey;

    // Default constructor: use the enum name as the translation key
    OverallRole() {
        this.assignmentKey = OverallRole.class.getSimpleName() + "." + name();
        this.introductionKey = this.assignmentKey;
    }

    // Optional constructor to allow using existing translation keys
    OverallRole(String assignmentKey, String introductionKey) {
        this.assignmentKey = assignmentKey;
        this.introductionKey = introductionKey;
    }

    public String getIntroductionKey() {
        return introductionKey;
    }

    public String getAssignmentKey() {
        return assignmentKey;
    }
}
