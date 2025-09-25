package app.owlcms.data.technicalofficial;

public enum OfficialRole {
    ANNOUNCER("Announcer", "Announcer"),
    TIMEKEEPER("Timekeeper", "Timekeeper"),
    CENTER_REFEREE("Referee2", "CenterReferee"),
    LEFT_REFEREE("Referee1", "SideReferee"),
    RIGHT_REFEREE("Referee3", "SideReferee"),
    MARSHAL1("Marshal1", "ChiefMarshal"),
    MARSHAL2("Marshal2", "AssistantMarshal"),
    TECHNICAL_CONTROLLER1("TechnicalController1", "TechnicalController1"),
    TECHNICAL_CONTROLLER2("TechnicalController2", "TechnicalController2"),
    JURY_PRESIDENT("JuryPresident", "JuryPresident"),
    JURY_A("Jury2", "JuryMember"),
    JURY_B("Jury3", "JuryMember"),
    JURY_C("Jury4", "JuryMember"),
    JURY_D("Jury5", "JuryMember"),
    REFEREE_RESERVE("ReserveReferee", "ReserveReferee"),
    JURY_RESERVE("ReserveJury", "ReserveJury"),
    WEIGHIN1("Weighin1", "Weighin1"),
    WEIGHIN2("Weighin2", "Weighin2");

    private final String assignmentKey;
     private final String introductionKey;

    // Default constructor: use the enum name as the translation key
    OfficialRole() {
        this.assignmentKey = OfficialRole.class.getSimpleName() + "." + name();
        this.introductionKey = this.assignmentKey;
    }

    // Optional constructor to allow using existing translation keys
    OfficialRole(String assignmentKey, String introductionKey) {
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

