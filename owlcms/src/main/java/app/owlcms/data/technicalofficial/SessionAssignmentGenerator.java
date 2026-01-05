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
 * Implements rotation logic:
 * - Referees: center → reserve → right → left → center (skip reserve if team has 3 members)
 * - Jury: if reserve exists: D→reserve, A→B, B→C, C→D, reserve→A; if no reserve, no rotation
 * 
 * Assignments are stored directly in Group entity fields (jury1, jury2, referee1, etc.)
 */
public class SessionAssignmentGenerator {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(SessionAssignmentGenerator.class);

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
        return map;
    }

    /**
     * Generate session assignments for all sessions based on the timetable.
     * 
     * @return Number of assignments generated
     */
    public static int generateSessionAssignments() {
        return JPAService.runInTransaction(em -> {
            // Get all timetable entries
            List<TechnicalOfficialsTimetable> timetableEntries = TechnicalOfficialsTimetableRepository.findAll(em);
            if (timetableEntries.isEmpty()) {
                logger.warn("No timetable entries found - cannot generate assignments");
                return 0;
            }

            // Get all sessions in chronological order
            List<Group> sessions = GroupRepository.findAll().stream()
                    .filter(g -> g.getWeighInTime() != null)
                    .sorted(Comparator.comparing(Group::getWeighInTime))
                    .collect(Collectors.toList());

            if (sessions.isEmpty()) {
                logger.warn("No sessions found - cannot generate assignments");
                return 0;
            }

            // Get all technical officials with their OfficialRole and team
            List<TechnicalOfficial> officials = TechnicalOfficialRepository.findAll();
            
            // Group officials by OfficialRole and team
            Map<OfficialRole, Map<Integer, List<TechnicalOfficial>>> officialsByRoleAndTeam = officials.stream()
                    .filter(o -> o.getOfficialRole() != null && o.getTechnicalOfficialTeam() != null)
                    .collect(Collectors.groupingBy(
                            TechnicalOfficial::getOfficialRole,
                            Collectors.groupingBy(TechnicalOfficial::getTechnicalOfficialTeam)));

            // Group timetable entries by role category and team, collecting sessions
            Map<OfficialRole, Map<Integer, Set<Group>>> sessionsByRoleAndTeam = new HashMap<>();
            for (TechnicalOfficialsTimetable entry : timetableEntries) {
                sessionsByRoleAndTeam
                        .computeIfAbsent(entry.getRoleCategory(), k -> new HashMap<>())
                        .computeIfAbsent(entry.getTeamNumber(), k -> new HashSet<>())
                        .add(entry.getGroup());
            }

            Map<OfficialRole, BiConsumer<Group, String>> setterMap = sessionRoleSetterMap();
            int assignmentCount = 0;

            // Process REFEREE teams with rotation
            assignmentCount += processRefereeTeams(em, sessions, officialsByRoleAndTeam, sessionsByRoleAndTeam, setterMap);

            // Process JURY teams with rotation
            assignmentCount += processJuryTeams(em, sessions, officialsByRoleAndTeam, sessionsByRoleAndTeam, setterMap);

            // Process non-rotating roles (MARSHAL, TC, TIMEKEEPER, ANNOUNCER, WEIGHIN)
            assignmentCount += processNonRotatingRoles(em, sessions, officialsByRoleAndTeam, sessionsByRoleAndTeam, setterMap);

            return assignmentCount;
        });
    }

    /**
     * Process referee teams with rotation: center → reserve → right → left → center
     * Skip reserve if team has only 3 members.
     */
    private static int processRefereeTeams(EntityManager em, List<Group> allSessions,
            Map<OfficialRole, Map<Integer, List<TechnicalOfficial>>> officialsByRoleAndTeam,
            Map<OfficialRole, Map<Integer, Set<Group>>> sessionsByRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;
        
        // Get referee officials (generic REFEREE role)
        Map<Integer, List<TechnicalOfficial>> refereesByTeam = officialsByRoleAndTeam.get(OfficialRole.REFEREE);
        if (refereesByTeam == null || refereesByTeam.isEmpty()) {
            logger.info("No referees with REFEREE role found");
            return 0;
        }

        // Get sessions assigned to referee teams
        Map<Integer, Set<Group>> refereeSessions = sessionsByRoleAndTeam.get(OfficialRole.REFEREE);
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
                        logger.debug("Assigned {} as {} for session {}", officialName, assignedRole, session.getName());
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
     * Process jury teams with rotation:
     * If reserve: D→reserve, A→B, B→C, C→D, reserve→A
     * If no reserve: no rotation
     */
    private static int processJuryTeams(EntityManager em, List<Group> allSessions,
            Map<OfficialRole, Map<Integer, List<TechnicalOfficial>>> officialsByRoleAndTeam,
            Map<OfficialRole, Map<Integer, Set<Group>>> sessionsByRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;

        // Get jury member officials (generic JURY_MEMBER role)
        Map<Integer, List<TechnicalOfficial>> juryMembersByTeam = officialsByRoleAndTeam.getOrDefault(OfficialRole.JURY_MEMBER, new HashMap<>());
        
        // Get jury president officials (JURY_PRESIDENT role)
        Map<Integer, List<TechnicalOfficial>> juryPresidentsByTeam = officialsByRoleAndTeam.getOrDefault(OfficialRole.JURY_PRESIDENT, new HashMap<>());

        // Get sessions assigned to jury teams
        Map<Integer, Set<Group>> jurySessions = sessionsByRoleAndTeam.getOrDefault(OfficialRole.JURY_MEMBER, new HashMap<>());
        
        // Merge with JURY_PRESIDENT sessions if different
        Map<Integer, Set<Group>> juryPresidentSessions = sessionsByRoleAndTeam.getOrDefault(OfficialRole.JURY_PRESIDENT, new HashMap<>());
        for (Map.Entry<Integer, Set<Group>> entry : juryPresidentSessions.entrySet()) {
            jurySessions.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }

        if (jurySessions.isEmpty()) {
            logger.info("No jury team assignments in timetable");
            return 0;
        }

        // Process each team
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
                        logger.debug("Assigned {} as JURY_PRESIDENT for session {}", presidentName, session.getName());
                    }
                }
            }

            int memberCount = members.size();
            boolean hasReserve = memberCount >= 5;

            if (hasReserve) {
                // Rotation: A, B, C, D, Reserve
                OfficialRole[] rotationCycle = {
                        OfficialRole.JURY_A, OfficialRole.JURY_B, OfficialRole.JURY_C,
                        OfficialRole.JURY_D, OfficialRole.JURY_RESERVE
                };

                for (int officialIndex = 0; officialIndex < Math.min(memberCount, rotationCycle.length); officialIndex++) {
                    TechnicalOfficial official = members.get(officialIndex);
                    String officialName = official.getFullName();

                    for (int sessionIndex = 0; sessionIndex < sortedTeamSessions.size(); sessionIndex++) {
                        Group session = sortedTeamSessions.get(sessionIndex);

                        // Calculate rotated position
                        int rotatedPosition = (officialIndex + sessionIndex) % rotationCycle.length;
                        OfficialRole assignedRole = rotationCycle[rotatedPosition];

                        BiConsumer<Group, String> setter = setterMap.get(assignedRole);
                        if (setter != null) {
                            setter.accept(session, officialName);
                            count++;
                            logger.debug("Assigned {} as {} for session {}", officialName, assignedRole, session.getName());
                        }
                    }
                }
            } else {
                // No reserve - assign static positions A, B, C, D
                OfficialRole[] staticRoles = {
                        OfficialRole.JURY_A, OfficialRole.JURY_B, OfficialRole.JURY_C, OfficialRole.JURY_D
                };

                for (int officialIndex = 0; officialIndex < Math.min(memberCount, staticRoles.length); officialIndex++) {
                    TechnicalOfficial official = members.get(officialIndex);
                    String officialName = official.getFullName();
                    OfficialRole staticRole = staticRoles[officialIndex];

                    BiConsumer<Group, String> setter = setterMap.get(staticRole);
                    for (Group session : sortedTeamSessions) {
                        if (setter != null) {
                            setter.accept(session, officialName);
                            count++;
                            logger.debug("Assigned {} as {} for session {}", officialName, staticRole, session.getName());
                        }
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
     * Process non-rotating roles: MARSHAL1, MARSHAL2, TECHNICAL_CONTROLLER1, TECHNICAL_CONTROLLER2,
     * TIMEKEEPER, ANNOUNCER, WEIGHIN1, WEIGHIN2
     */
    private static int processNonRotatingRoles(EntityManager em, List<Group> allSessions,
            Map<OfficialRole, Map<Integer, List<TechnicalOfficial>>> officialsByRoleAndTeam,
            Map<OfficialRole, Map<Integer, Set<Group>>> sessionsByRoleAndTeam,
            Map<OfficialRole, BiConsumer<Group, String>> setterMap) {
        
        int count = 0;

        // Non-rotating roles that map directly
        OfficialRole[] nonRotatingRoles = {
                OfficialRole.MARSHAL1, OfficialRole.MARSHAL2,
                OfficialRole.TECHNICAL_CONTROLLER1, OfficialRole.TECHNICAL_CONTROLLER2,
                OfficialRole.TIMEKEEPER, OfficialRole.ANNOUNCER,
                OfficialRole.WEIGHIN1, OfficialRole.WEIGHIN2
        };

        for (OfficialRole role : nonRotatingRoles) {
            Map<Integer, List<TechnicalOfficial>> officialsByTeam = officialsByRoleAndTeam.get(role);
            Map<Integer, Set<Group>> sessionsByTeam = sessionsByRoleAndTeam.get(role);

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

                // Use the first official for this role in this team
                TechnicalOfficial official = teamOfficials.get(0);
                String officialName = official.getFullName();

                BiConsumer<Group, String> setter = setterMap.get(role);
                if (setter == null) {
                    continue;
                }

                for (Group session : allSessions) {
                    if (teamSessions.contains(session)) {
                        setter.accept(session, officialName);
                        em.merge(session);
                        count++;
                        logger.debug("Assigned {} as {} for session {}", officialName, role, session.getName());
                    }
                }
            }
        }

        return count;
    }
}
