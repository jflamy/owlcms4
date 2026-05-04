package app.owlcms.data.technicalofficial;

public enum OfficialRole {

    // technical officials, in introduction order
    // generic roles are included for convenience
    REFEREE("Referee", "Referee"),
    CENTER_REFEREE("Referee2", "CenterReferee"),
    LEFT_REFEREE("Referee1", "SideReferee"),
    RIGHT_REFEREE("Referee3", "SideReferee"),
    REFEREE_RESERVE("ReserveReferee", "ReserveReferee"),
    MARSHALL("Marshall", "Marshall"),
    MARSHAL1("Marshal1", "ChiefMarshal"),
    MARSHAL2("Marshal2", "AssistantMarshal"),
    TIMEKEEPER("Timekeeper", "Timekeeper"),
    TECHNICAL_CONTROLLER("TechnicalController", "TechnicalController"),
    TECHNICAL_CONTROLLER1("TechnicalController1", "TechnicalController"),
    TECHNICAL_CONTROLLER2("TechnicalController2", "TechnicalController"),
    DOCTOR("Doctor1", "Doctor"),
    DOCTOR2("Doctor2", "Doctor"),
    DOCTOR3("Doctor3", "Doctor"),

    // introduction not mandatory in TCRR
    COMPETITION_SECRETARY("CompetitionSecretary1", "CompetitionSecretary"),
    COMPETITION_SECRETARY2("CompetitionSecretary2", "CompetitionSecretary"),
    ANNOUNCER("Announcer", "Announcer"),

    // For federations that do not use the referees as weigh-in staff
    WEIGHIN("Weighin", "Weighin"),
    WEIGHIN1("Weighin1", "Weighin"),
    WEIGHIN2("Weighin2", "Weighin"),


    // Jury, in introduction order
    JURY("Jury", "Jury"),
    JURY_PRESIDENT("JuryPresident", "JuryPresident"),
    JURY_MEMBER("JuryMember", "JuryMember"),
    JURY_A("Jury2", "JuryMember"),
    JURY_B("Jury3", "JuryMember"),
    JURY_C("Jury4", "JuryMember"),
    JURY_D("Jury5", "JuryMember"),
    JURY_RESERVE("ReserveJury", "ReserveJury"),

    ;

    // Generic roles for team assignment (used with timetable)



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

    /**
     * Check if this role is a generic team assignment role (used in timetable)
     */
    public boolean isGenericTeamRole() {
        return this == JURY || this == REFEREE || this == MARSHALL || 
               this == TIMEKEEPER || this == TECHNICAL_CONTROLLER || 
               this == DOCTOR || this == COMPETITION_SECRETARY ||
               this == ANNOUNCER || this == WEIGHIN;
    }

    /**
     * Get specific session positions for a generic role
     */
    public java.util.List<OfficialRole> getSpecificPositions() {
        switch (this) {
            case JURY:
                return java.util.List.of(JURY_PRESIDENT, JURY_A, JURY_B, JURY_C, JURY_D, JURY_RESERVE);
            case REFEREE:
                return java.util.List.of(CENTER_REFEREE, LEFT_REFEREE, RIGHT_REFEREE, REFEREE_RESERVE);
            case MARSHALL:
                return java.util.List.of(MARSHAL1, MARSHAL2);
            case TECHNICAL_CONTROLLER:
                return java.util.List.of(TECHNICAL_CONTROLLER1, TECHNICAL_CONTROLLER2);
            case DOCTOR:
                return java.util.List.of(DOCTOR, DOCTOR2, DOCTOR3);
            case COMPETITION_SECRETARY:
                return java.util.List.of(COMPETITION_SECRETARY, COMPETITION_SECRETARY2);
            case TIMEKEEPER:
                return java.util.List.of(TIMEKEEPER);
            case WEIGHIN:
                return java.util.List.of(WEIGHIN1, WEIGHIN2);
            default:
                return java.util.List.of(this);
        }
    }
}
