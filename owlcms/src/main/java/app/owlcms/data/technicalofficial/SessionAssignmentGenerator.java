/*******************************************************************************
 * @author Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.technicalofficial;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import ch.qos.logback.classic.Logger;

/**
 * Generates session assignments for technical officials based on the timetable.
 * 
 * Officials are grouped by {@link TeamRole} and team number.
 * The algorithm uses the TeamRole to determine which positions can be filled.
 * 
 * Implements rotation logic:
 * - Referees: center → reserve → right → left → center (skip reserve if team has 3 members)
 * - Jury: president stays fixed; jury members rotate according to jury size
 *   - 3-person jury: 2 active members (+ optional reserve if a 3rd member exists)
 *   - 5-person jury: 4 active members (+ optional reserve if a 5th member exists)
 * 
 * Assignments are stored directly in Group entity fields (jury1, jury2, referee1, etc.)
 */
public class SessionAssignmentGenerator {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(SessionAssignmentGenerator.class);

    /**
     * Maps timetable role categories to the TeamRole used for grouping officials.
     * Timetable uses OfficialRole categories (JURY, REFEREE, MARSHALL, etc.) 
     * which we map to TeamRole for lookup.
     */
    private static TeamRole mapTimetableCategoryToTeamRole(OfficialRole timetableCategory) {
        if (timetableCategory == null) {
            return null;
        }
        switch (timetableCategory) {
            case JURY:
            case JURY_MEMBER:
            case JURY_A:
            case JURY_B:
            case JURY_C:
            case JURY_D:
            case JURY_RESERVE:
                return TeamRole.JURY;
            case JURY_PRESIDENT:
                return TeamRole.JURY_PRESIDENT;
            case REFEREE:
            case CENTER_REFEREE:
            case LEFT_REFEREE:
            case RIGHT_REFEREE:
            case REFEREE_RESERVE:
                return TeamRole.REFEREE;
            case MARSHALL:
            case MARSHAL1:
            case MARSHAL2:
                return TeamRole.MARSHALL;
            case TIMEKEEPER:
                return TeamRole.TIMEKEEPER;
            case TECHNICAL_CONTROLLER:
            case TECHNICAL_CONTROLLER1:
            case TECHNICAL_CONTROLLER2:
                return TeamRole.TECHNICAL_CONTROLLER;
            case DOCTOR:
            case DOCTOR2:
            case DOCTOR3:
                return TeamRole.DOCTOR;
            case COMPETITION_SECRETARY:
            case COMPETITION_SECRETARY2:
                return TeamRole.COMPETITION_SECRETARY;
            case ANNOUNCER:
                return TeamRole.ANNOUNCER;
            case WEIGHIN1:
            case WEIGHIN2:
                return TeamRole.WEIGHIN;
            default:
                return null;
        }
    }

    /**
     * Creates a mapping from OfficialRole to the corresponding setter in the Group class.
     */
    private static Map<OfficialRole, BiConsumer<Group, String>> sessionRoleSetterMap() {
        Map<OfficialRole, BiConsumer<Group, String>> map = new EnumMap<>(OfficialRole.class);

        map.put(OfficialRole.CENTER_REFEREE, Group::setReferee2);
        map.put(OfficialRole.LEFT_REFEREE, Group::setReferee1);
        map.put(OfficialRole.RIGHT_REFEREE, Group::setReferee3);
        map.put(OfficialRole.TIMEKEEPER, Group::setTimeKeeper);
        map.put(OfficialRole.TECHNICAL_CONTROLLER1, Group::setTechnicalController);
        map.put(OfficialRole.TECHNICAL_CONTROLLER2, Group::setTechnicalController2);
        map.put(OfficialRole.MARSHAL1, Group::setMarshall);
        map.put(OfficialRole.MARSHAL2, Group::setMarshal2);
        map.put(OfficialRole.JURY_PRESIDENT, Group::setJury1);
        map.put(OfficialRole.JURY_A, Group::setJury2);
        map.put(OfficialRole.JURY_B, Group::setJury3);
        map.put(OfficialRole.JURY_C, Group::setJury4);
        map.put(OfficialRole.JURY_D, Group::setJury5);
        map.put(OfficialRole.ANNOUNCER, Group::setAnnouncer);
        map.put(OfficialRole.WEIGHIN1, Group::setWeighIn1);
        map.put(OfficialRole.WEIGHIN2, Group::setWeighIn2);
        map.put(OfficialRole.REFEREE_RESERVE, Group::setReserve);
        map.put(OfficialRole.JURY_RESERVE, Group::setReserveJury);
        map.put(OfficialRole.DOCTOR, Group::setDoctor);
        map.put(OfficialRole.DOCTOR2, Group::setDoctor2);
        map.put(OfficialRole.DOCTOR3, Group::setDoctor3);
        map.put(OfficialRole.COMPETITION_SECRETARY, Group::setCompetitionSecretary);
        map.put(OfficialRole.COMPETITION_SECRETARY2, Group::setCompetitionSecretary2);
        return map;
    }

    /**
     * Generate session assignments for all sessions based on the timetable.
     * 
     * Officials are grouped by their TeamRole and team number.
     * 
     * @return Number of assignments generated
     */
    public static int generateSessionAssignments() {
        return JPAService.runInTransaction(em -> {
            // Get all timetable entries
            List<TechnicalOfficialsTimetable> timetableEntries = TechnicalOfficialsTimetableRepository.findAll(em);
            if (timetableEntries.isEmpty()) {
                logger./**/warn("No timetable entries found - cannot generate assignments");
                return 0;
            }

            // Clear all existing assignments for all sessions before generating new ones
            logger.info("Clearing all existing session assignments before generating new assignments");
            for (Group group : GroupRepository.doFindAll(em)) {
                group.clearAllAssignments();
                em.merge(group);
            }

            // Get all technical officials
            List<TechnicalOfficial> officials = TechnicalOfficialRepository.findAll();

            // Get all sessions in chronological order (by competition time if available, otherwise by name)
            List<Group> sessions = GroupRepository.doFindAll(em).stream()
                    .sorted((g1, g2) -> {
                        // Sort by competition time if both have it
                        if (g1.getCompetitionTime() != null && g2.getCompetitionTime() != null) {
                            return g1.getCompetitionTime().compareTo(g2.getCompetitionTime());
                        }
                        // Sort by weigh-in time if both have it
                        if (g1.getWeighInTime() != null && g2.getWeighInTime() != null) {
                            return g1.getWeighInTime().compareTo(g2.getWeighInTime());
                        }
                        // Put sessions with times first, then sort by name
                        boolean g1HasTime = g1.getCompetitionTime() != null || g1.getWeighInTime() != null;
                        boolean g2HasTime = g2.getCompetitionTime() != null || g2.getWeighInTime() != null;
                        if (g1HasTime != g2HasTime) {
                            return g1HasTime ? -1 : 1;
                        }
                        // Finally sort by name as fallback
                        String name1 = g1.getName() != null ? g1.getName() : "";
                        String name2 = g2.getName() != null ? g2.getName() : "";
                        return name1.compareTo(name2);
                    })
                    .collect(Collectors.toList());

            if (sessions.isEmpty()) {
                logger./**/warn("No sessions found - cannot generate assignments");
                return 0;
            }

            // Group officials by TeamRole and team number
            Map<TeamRole, Map<Integer, List<TechnicalOfficial>>> officialsByTeamRoleAndTeam = new HashMap<>();
            for (TechnicalOfficial official : officials) {
                if (official.getTechnicalOfficialTeam() == null || official.getTeamRole() == null) {
                    continue;
                }
                officialsByTeamRoleAndTeam
                    .computeIfAbsent(official.getTeamRole(), k -> new HashMap<>())
                    .computeIfAbsent(official.getTechnicalOfficialTeam(), k -> new ArrayList<>())
                    .add(official);
            }

            // Group timetable entries by TeamRole and team number, collecting sessions
            Map<TeamRole, Map<Integer, Set<Group>>> sessionsByTeamRoleAndTeam = new HashMap<>();
            for (TechnicalOfficialsTimetable entry : timetableEntries) {
                OfficialRole timetableRole = entry.getRoleCategory();
                // Map timetable OfficialRole category to TeamRole for lookup
                TeamRole teamRole = mapTimetableCategoryToTeamRole(timetableRole);
                if (teamRole != null) {
                    sessionsByTeamRoleAndTeam
                            .computeIfAbsent(teamRole, k -> new HashMap<>())
                            .computeIfAbsent(entry.getTeamNumber(), k -> new HashSet<>())
                            .add(entry.getGroup());
                }
            }

            Map<OfficialRole, BiConsumer<Group, String>> setterMap = sessionRoleSetterMap();
            int assignmentCount = 0;

            // Process REFEREE teams with rotation
            assignmentCount += processRefereeTeams(em, sessions, officialsByTeamRoleAndTeam, sessionsByTeamRoleAndTeam, setterMap);

            // Process JURY teams with rotation (JURY_PRESIDENT + JURY members)
            assignmentCount += processJuryTeams(em, sessions, officialsByTeamRoleAndTeam, sessionsByTeamRoleAndTeam, setterMap);

            // Process non-rotating roles (MARSHAL, TC, TIMEKEEPER, ANNOUNCER, WEIGHIN, DOCTOR, COMPETITION_SECRETARY)
            assignmentCount += processNonRotatingRoles(em, sessions, officialsByTeamRoleAndTeam, sessionsByTeamRoleAndTeam, setterMap);

            return assignmentCount;
        });
    }

    /**
     * Process referee teams with rotation: center → reserve → right → left → center
     * Skip reserve if team has only 3 members.
     */
    private static int processRefereeTeams(EntityManager em, List<Group> allSessions,
            Map<TeamRole, Map<Integer, List<TechnicalOfficial>>> officialsByTeamRoleAndTeam,
            Map<TeamRole, Map<Integer, Set<Group>>> sessionsByTeamRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;
        
        // Get referee officials (TeamRole.REFEREE)
        Map<Integer, List<TechnicalOfficial>> refereesByTeam = officialsByTeamRoleAndTeam.get(TeamRole.REFEREE);
        if (refereesByTeam == null || refereesByTeam.isEmpty()) {
            logger.info("No referees with REFEREE TeamRole found");
            return 0;
        }

        // Get sessions assigned to referee teams
        Map<Integer, Set<Group>> refereeSessions = sessionsByTeamRoleAndTeam.get(TeamRole.REFEREE);
        if (refereeSessions == null || refereeSessions.isEmpty()) {
            logger.info("No referee team assignments in timetable");
            return 0;
        }

        for (Map.Entry<Integer, List<TechnicalOfficial>> teamEntry : refereesByTeam.entrySet()) {
            int teamNumber = teamEntry.getKey();
            List<TechnicalOfficial> teamOfficials = teamEntry.getValue();
            Set<Group> teamSessions = refereeSessions.get(teamNumber);

            if (teamOfficials.isEmpty() || teamSessions == null || teamSessions.isEmpty()) {
                continue;
            }

            // Sort officials by ID for consistent ordering
            teamOfficials.sort(Comparator.comparing(TechnicalOfficial::getId));

            int teamSize = teamOfficials.size();
            boolean hasReserve = teamSize >= 4;

            // Define rotation cycle
            OfficialRole[] rotationCycle = hasReserve
                    ? new OfficialRole[]{OfficialRole.CENTER_REFEREE, OfficialRole.REFEREE_RESERVE,
                            OfficialRole.RIGHT_REFEREE, OfficialRole.LEFT_REFEREE}
                    : new OfficialRole[]{OfficialRole.CENTER_REFEREE, OfficialRole.RIGHT_REFEREE,
                            OfficialRole.LEFT_REFEREE};

            // Sort team sessions chronologically
            List<Group> sortedTeamSessions = allSessions.stream()
                    .filter(teamSessions::contains)
                    .collect(Collectors.toList());

            // Assign each official to sessions with rotation
            for (int officialIndex = 0; officialIndex < Math.min(teamSize, rotationCycle.length); officialIndex++) {
                TechnicalOfficial official = teamOfficials.get(officialIndex);
                String officialName = official.getFullName();

                for (int sessionIndex = 0; sessionIndex < sortedTeamSessions.size(); sessionIndex++) {
                    Group session = sortedTeamSessions.get(sessionIndex);

                    // Calculate rotated position
                    int rotatedPosition = (officialIndex + sessionIndex) % rotationCycle.length;
                    OfficialRole assignedRole = rotationCycle[rotatedPosition];

                    // Set the assignment on the Group
                    BiConsumer<Group, String> setter = setterMap.get(assignedRole);
                    if (setter != null) {
                        setter.accept(session, officialName);
                        count++;
                    }
                }
            }

            // Merge sessions
            for (Group session : sortedTeamSessions) {
                em.merge(session);
            }
        }

        return count;
    }

    /**
     * Process jury teams with rotation.
     * A jury team = one non-rotating president + rotating jury members.
     *
     * Jury-member counts determine the jury size:
     * - 2 or 3 members -> 3-person jury (A, B, optional reserve)
     * - 4 or 5 members -> 5-person jury (A, B, C, D, optional reserve)
     */
    private static int processJuryTeams(EntityManager em, List<Group> allSessions,
            Map<TeamRole, Map<Integer, List<TechnicalOfficial>>> officialsByTeamRoleAndTeam,
            Map<TeamRole, Map<Integer, Set<Group>>> sessionsByTeamRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;

        // Get jury member officials (TeamRole.JURY)
        Map<Integer, List<TechnicalOfficial>> juryMembersByTeam = officialsByTeamRoleAndTeam.getOrDefault(TeamRole.JURY, new HashMap<>());
        
        // Get jury president officials (TeamRole.JURY_PRESIDENT)
        Map<Integer, List<TechnicalOfficial>> juryPresidentsByTeam = officialsByTeamRoleAndTeam.getOrDefault(TeamRole.JURY_PRESIDENT, new HashMap<>());

        // Get sessions assigned to jury teams (from timetable)
        Map<Integer, Set<Group>> jurySessions = new HashMap<>(sessionsByTeamRoleAndTeam.getOrDefault(TeamRole.JURY, new HashMap<>()));
        
        // Merge with JURY_PRESIDENT sessions if different
        Map<Integer, Set<Group>> juryPresidentSessions = sessionsByTeamRoleAndTeam.getOrDefault(TeamRole.JURY_PRESIDENT, new HashMap<>());
        for (Map.Entry<Integer, Set<Group>> entry : juryPresidentSessions.entrySet()) {
            jurySessions.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }

        if (jurySessions.isEmpty()) {
            logger.info("No jury team assignments in timetable");
            return 0;
        }

        // Process each team (union of all team numbers that have president or members)
        Set<Integer> allTeams = new HashSet<>();
        allTeams.addAll(juryMembersByTeam.keySet());
        allTeams.addAll(juryPresidentsByTeam.keySet());

        for (Integer teamNumber : allTeams) {
            List<TechnicalOfficial> members = juryMembersByTeam.getOrDefault(teamNumber, new ArrayList<>());
            List<TechnicalOfficial> presidents = juryPresidentsByTeam.getOrDefault(teamNumber, new ArrayList<>());
            Set<Group> teamSessions = jurySessions.get(teamNumber);

            if (teamSessions == null || teamSessions.isEmpty()) {
                continue;
            }

            // Sort members by ID for consistent ordering
            members.sort(Comparator.comparing(TechnicalOfficial::getId));

            // Sort team sessions chronologically
            List<Group> sortedTeamSessions = allSessions.stream()
                    .filter(teamSessions::contains)
                    .collect(Collectors.toList());

            // Assign president (no rotation)
            if (!presidents.isEmpty()) {
                TechnicalOfficial president = presidents.get(0);
                String presidentName = president.getFullName();
                BiConsumer<Group, String> setter = setterMap.get(OfficialRole.JURY_PRESIDENT);
                
                for (Group session : sortedTeamSessions) {
                    if (setter != null) {
                        setter.accept(session, presidentName);
                        count++;
                    }
                }
            }

            int memberCount = members.size();
            OfficialRole[] rotationCycle = getJuryRotationCycle(memberCount);

            for (int officialIndex = 0; officialIndex < Math.min(memberCount, rotationCycle.length); officialIndex++) {
                TechnicalOfficial official = members.get(officialIndex);
                String officialName = official.getFullName();

                for (int sessionIndex = 0; sessionIndex < sortedTeamSessions.size(); sessionIndex++) {
                    Group session = sortedTeamSessions.get(sessionIndex);

                    int rotatedPosition = (officialIndex + sessionIndex) % rotationCycle.length;
                    OfficialRole assignedRole = rotationCycle[rotatedPosition];

                    BiConsumer<Group, String> setter = setterMap.get(assignedRole);
                    if (setter != null) {
                        setter.accept(session, officialName);
                        count++;
                    }
                }
            }

            // Merge sessions
            for (Group session : sortedTeamSessions) {
                em.merge(session);
            }
        }

        return count;
    }

    private static OfficialRole[] getJuryRotationCycle(int memberCount) {
        if (memberCount <= 0) {
            return new OfficialRole[0];
        }

        if (memberCount <= 3) {
            return memberCount == 3
                    ? new OfficialRole[] { OfficialRole.JURY_A, OfficialRole.JURY_B, OfficialRole.JURY_RESERVE }
                    : memberCount == 2
                            ? new OfficialRole[] { OfficialRole.JURY_A, OfficialRole.JURY_B }
                            : new OfficialRole[] { OfficialRole.JURY_A };
        }

        return memberCount >= 5
                ? new OfficialRole[] {
                        OfficialRole.JURY_A,
                        OfficialRole.JURY_B,
                        OfficialRole.JURY_C,
                        OfficialRole.JURY_D,
                        OfficialRole.JURY_RESERVE }
                : new OfficialRole[] {
                        OfficialRole.JURY_A,
                        OfficialRole.JURY_B,
                        OfficialRole.JURY_C,
                        OfficialRole.JURY_D };
    }

    /**
     * Process non-rotating roles: MARSHAL, TECHNICAL_CONTROLLER, TIMEKEEPER, 
     * ANNOUNCER, WEIGHIN, DOCTOR, COMPETITION_SECRETARY
     * 
     * For roles with multiple positions (e.g., MARSHAL1, MARSHAL2), officials are assigned 
     * in order to available positions.
     */
    private static int processNonRotatingRoles(EntityManager em, List<Group> allSessions,
            Map<TeamRole, Map<Integer, List<TechnicalOfficial>>> officialsByTeamRoleAndTeam,
            Map<TeamRole, Map<Integer, Set<Group>>> sessionsByTeamRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;

        // Define TeamRole to OfficialRole[] mappings for non-rotating roles
        // Each TeamRole can have multiple positions to fill
        Map<TeamRole, OfficialRole[]> teamRoleToPositions = new EnumMap<>(TeamRole.class);
        teamRoleToPositions.put(TeamRole.MARSHALL, new OfficialRole[]{OfficialRole.MARSHAL1, OfficialRole.MARSHAL2});
        teamRoleToPositions.put(TeamRole.TECHNICAL_CONTROLLER, new OfficialRole[]{OfficialRole.TECHNICAL_CONTROLLER1, OfficialRole.TECHNICAL_CONTROLLER2});
        teamRoleToPositions.put(TeamRole.TIMEKEEPER, new OfficialRole[]{OfficialRole.TIMEKEEPER});
        teamRoleToPositions.put(TeamRole.ANNOUNCER, new OfficialRole[]{OfficialRole.ANNOUNCER});
        teamRoleToPositions.put(TeamRole.WEIGHIN, new OfficialRole[]{OfficialRole.WEIGHIN1, OfficialRole.WEIGHIN2});
        teamRoleToPositions.put(TeamRole.DOCTOR, new OfficialRole[]{OfficialRole.DOCTOR, OfficialRole.DOCTOR2, OfficialRole.DOCTOR3});
        teamRoleToPositions.put(TeamRole.COMPETITION_SECRETARY, new OfficialRole[]{OfficialRole.COMPETITION_SECRETARY, OfficialRole.COMPETITION_SECRETARY2});

        for (Map.Entry<TeamRole, OfficialRole[]> entry : teamRoleToPositions.entrySet()) {
            TeamRole teamRole = entry.getKey();
            OfficialRole[] positions = entry.getValue();

            Map<Integer, List<TechnicalOfficial>> officialsByTeam = officialsByTeamRoleAndTeam.get(teamRole);
            Map<Integer, Set<Group>> sessionsByTeam = sessionsByTeamRoleAndTeam.get(teamRole);

            if (officialsByTeam == null || sessionsByTeam == null) {
                continue;
            }

            for (Map.Entry<Integer, List<TechnicalOfficial>> teamEntry : officialsByTeam.entrySet()) {
                int teamNumber = teamEntry.getKey();
                List<TechnicalOfficial> teamOfficials = teamEntry.getValue();
                Set<Group> teamSessions = sessionsByTeam.get(teamNumber);

                if (teamOfficials.isEmpty() || teamSessions == null || teamSessions.isEmpty()) {
                    continue;
                }

                // Sort officials by ID for consistent ordering
                teamOfficials.sort(Comparator.comparing(TechnicalOfficial::getId));

                // Assign officials to positions in order
                for (int i = 0; i < Math.min(teamOfficials.size(), positions.length); i++) {
                    TechnicalOfficial official = teamOfficials.get(i);
                    String officialName = official.getFullName();
                    OfficialRole position = positions[i];

                    BiConsumer<Group, String> setter = setterMap.get(position);
                    if (setter == null) {
                        continue;
                    }

                    for (Group session : allSessions) {
                        if (teamSessions.contains(session)) {
                            setter.accept(session, officialName);
                            em.merge(session);
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }
}
