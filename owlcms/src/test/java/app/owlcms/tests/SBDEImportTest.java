/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import app.owlcms.Main;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.spreadsheet.JXLSSBDEExport;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;

public class SBDEImportTest {

    private static final Logger logger = LoggerFactory.getLogger(SBDEImportTest.class);
    private static final String FIXTURE = "/testDatabases/SBDETestDatabase_2025-08-24_18h11.json";
    private static final Set<String> PART_1_SESSIONS = Set.of("1", "2");
    private static final Set<String> PART_2_SESSIONS = Set.of("3", "4", "5");
    private static final String CHANGED_REFEREE = "Poulin, Manon";
    private static final String CHANGED_RECORD_ELIGIBILITIES = "QC,CA";

    @BeforeClass
    public static void setupTests() {
        System.setProperty("enableEmbeddedMqtt", "false");
        Main.injectSuppliers();
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    @Test
    public void importSplitSBDEFilesUpdatesOfficialsPreservesLiftsAndAddsAthletes() throws Exception {
        byte[] part1 = createSbdeForSessions(PART_1_SESSIONS);
        byte[] changedOfficialsPart1 = createSbdeForSessions(PART_1_SESSIONS, CHANGED_REFEREE);
        byte[] part2 = createSbdeForSessions(PART_2_SESSIONS);

        resetDatabase();
        importEmptyJsonFixture();
        assertTrue("initial stripped JSON import should not contain sessions", GroupRepository.findAll().isEmpty());
        assertTrue("initial stripped JSON import should not contain athletes", AthleteRepository.findAll().isEmpty());
        assertActiveCategoryAvailable("YTH F 53");
        assertActiveCategoryAvailable("AM45");

        ImportResult firstImport = loadSbdePartIntoEmptyDatabase(part1);
        assertEquals("part 1 sessions processed", 2, firstImport.sessionsProcessed);
        assertEquals("part 1 athletes processed", expectedAthleteCount(PART_1_SESSIONS), firstImport.athletesProcessed);
        assertSessionNames(PART_1_SESSIONS);
        assertEquals("part 1 athlete count", expectedAthleteCount(PART_1_SESSIONS), AthleteRepository.findAll().size());

        Group session1 = GroupRepository.findByName("1");
        assertNotNull("session 1 should exist after first SBDE import", session1);
        String originalReferee = session1.getReferee1();
        assertNotEquals("fixture should let the test change referee 1", CHANGED_REFEREE, originalReferee);

        ImportResult officialsImport = updateSbdeSessionOfficialsOnly(changedOfficialsPart1);
        assertEquals("officials-only import sessions processed", 2, officialsImport.sessionsProcessed);
        assertEquals("officials-only import should ignore athletes", 0, officialsImport.athletesProcessed);
        assertEquals("session 1 referee should be updated by officials-only import",
                CHANGED_REFEREE, GroupRepository.findByName("1").getReferee1());
        assertEquals("officials-only import should not add athletes",
                expectedAthleteCount(PART_1_SESSIONS), AthleteRepository.findAll().size());

        Athlete athlete = firstAthleteInSession("1");
        LiftSnapshot liftsBeforeReload = LiftSnapshot.from(athlete);

        ImportResult athleteReload = reloadSbdeAthletesWithoutSessionChanges(part1);
        assertEquals("part 1 reload sessions identified", 2, athleteReload.sessionsProcessed);
        assertEquals("part 1 reload athletes processed", expectedAthleteCount(PART_1_SESSIONS), athleteReload.athletesProcessed);
        assertEquals("part 1 reload should not add duplicate athletes",
            expectedAthleteCount(PART_1_SESSIONS), AthleteRepository.findAll().size());
        assertEquals("part 1 reload should preserve existing athlete lifts",
            liftsBeforeReload, LiftSnapshot.from(findAthleteById(athlete.getId())));

        String changedFirstName = athlete.getFirstName() + " Changed";
        assertNotEquals("fixture should let the test change record eligibilities",
            CHANGED_RECORD_ELIGIBILITIES, athlete.getFederationCodes());
        changeAthleteNameAndRecordEligibilities(athlete, changedFirstName, CHANGED_RECORD_ELIGIBILITIES);
        Athlete changedAthlete = findAthleteById(athlete.getId());
        assertEquals("change-athlete operation should update the athlete name",
            changedFirstName, changedAthlete.getFirstName());
        assertEquals("change-athlete operation should update record eligibilities",
            CHANGED_RECORD_ELIGIBILITIES, changedAthlete.getFederationCodes());
        assertEquals("change-athlete operation should preserve existing lifts",
            liftsBeforeReload, LiftSnapshot.from(changedAthlete));

        Map<Long, LiftSnapshot> part1LiftSnapshots = liftSnapshotsForSessions(PART_1_SESSIONS);

        ImportResult additionalAthletes = addSbdeAdditionalSessionsAndAthletes(part2);
        assertEquals("part 2 sessions processed", 3, additionalAthletes.sessionsProcessed);
        assertEquals("part 2 athletes processed", expectedAthleteCount(PART_2_SESSIONS), additionalAthletes.athletesProcessed);
        assertSessionNames(Set.of("1", "2", "3", "4", "5"));
        assertEquals("all athletes from both SBDE parts should be present",
                expectedAthleteCount(PART_1_SESSIONS) + expectedAthleteCount(PART_2_SESSIONS),
                AthleteRepository.findAll().size());
        assertLiftSnapshotsForSessions(PART_1_SESSIONS, part1LiftSnapshots);
        Athlete changedAthleteAfterAdditionalImport = findAthleteById(athlete.getId());
        assertEquals("changed athlete first name should stay modified after adding part 2",
            changedFirstName, changedAthleteAfterAdditionalImport.getFirstName());
        assertEquals("changed athlete record eligibilities should stay modified after adding part 2",
            CHANGED_RECORD_ELIGIBILITIES, changedAthleteAfterAdditionalImport.getFederationCodes());
        assertEquals("changed athlete lifts should stay modified after adding part 2",
            liftsBeforeReload, LiftSnapshot.from(changedAthleteAfterAdditionalImport));
        assertEquals("session 1 referee should stay modified after adding part 2",
            CHANGED_REFEREE, GroupRepository.findByName("1").getReferee1());
        assertSessionsPresent(PART_2_SESSIONS);
    }

    private byte[] createSbdeForSessions(Set<String> sessionNames) throws Exception {
        return createSbdeForSessions(sessionNames, null);
    }

    private byte[] createSbdeForSessions(Set<String> sessionNames, String session1Referee) throws Exception {
        resetDatabase();
        importJsonPart(sessionNames);
        if (session1Referee != null) {
            Group session1 = GroupRepository.findByName("1");
            assertNotNull("session 1 should exist before changing referee", session1);
            session1.setReferee1(session1Referee);
            GroupRepository.save(session1);
        }
        assertSessionNames(sessionNames);
        assertEquals("filtered JSON athlete count", expectedAthleteCount(sessionNames), AthleteRepository.findAll().size());
        return exportSbde();
    }

    private void importJsonPart(Set<String> sessionNames) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root;
        try (InputStream input = SBDEImportTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull("SBDE JSON fixture should exist", input);
            root = (ObjectNode) mapper.readTree(input);
        }

        Set<Long> groupIds = new LinkedHashSet<>();
        ArrayNode groups = mapper.createArrayNode();
        for (JsonNode group : root.withArray("groups")) {
            if (sessionNames.contains(group.path("name").asText())) {
                groupIds.add(group.path("id").asLong());
                groups.add(group.deepCopy());
            }
        }
        assertEquals("selected session count", sessionNames.size(), groups.size());
        root.set("groups", groups);

        ArrayNode athletes = mapper.createArrayNode();
        for (JsonNode athlete : root.withArray("athletes")) {
            if (groupIds.contains(athlete.path("group").asLong())) {
                athletes.add(athlete.deepCopy());
            }
        }
        assertFalse("selected sessions should have athletes", athletes.isEmpty());
        root.set("athletes", athletes);

        FormatDetector.importData(new ByteArrayInputStream(mapper.writeValueAsBytes(root)));
        CategoryRepository.resetCodeMap();
    }

    private void importEmptyJsonFixture() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root;
        try (InputStream input = SBDEImportTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull("SBDE JSON fixture should exist", input);
            root = (ObjectNode) mapper.readTree(input);
        }
        root.set("groups", mapper.createArrayNode());
        root.set("athletes", mapper.createArrayNode());

        FormatDetector.importData(new ByteArrayInputStream(mapper.writeValueAsBytes(root)));
        CategoryRepository.resetCodeMap();
    }

    private byte[] exportSbde() throws Exception {
        JXLSSBDEExport export = new JXLSSBDEExport(null);
        byte[] bytes;
        try (InputStream input = export.createInputStream()) {
            bytes = input.readAllBytes();
        }
        assertTrue("SBDE workbook should not be empty", bytes.length > 0);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertTrue("SBDE workbook should contain at least one sheet", workbook.getNumberOfSheets() > 0);
        }
        return bytes;
    }

    private ImportResult loadSbdePartIntoEmptyDatabase(byte[] bytes) {
        return importSbde(bytes,
                NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS,
                NRegistrationFileProcessor.AthleteOptions.ADD_ATHLETES);
    }

    private ImportResult updateSbdeSessionOfficialsOnly(byte[] bytes) {
        return importSbde(bytes,
                NRegistrationFileProcessor.SessionOptions.UPDATE_OFFICIALS_AND_DESCRIPTION,
                NRegistrationFileProcessor.AthleteOptions.IGNORE_ATHLETES);
    }

    private ImportResult reloadSbdeAthletesWithoutSessionChanges(byte[] bytes) {
        return importSbde(bytes,
                NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS,
                NRegistrationFileProcessor.AthleteOptions.UPDATE_ADD_ATHLETES);
    }

    private ImportResult addSbdeAdditionalSessionsAndAthletes(byte[] bytes) {
        return importSbde(bytes,
                NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS,
                NRegistrationFileProcessor.AthleteOptions.ADD_ATHLETES);
    }

    private ImportResult importSbde(byte[] bytes,
            NRegistrationFileProcessor.SessionOptions sessionOptions,
            NRegistrationFileProcessor.AthleteOptions athleteOptions) {
        NRegistrationFileProcessor processor = new NRegistrationFileProcessor(true, Locale.ENGLISH);
        processor.setSessionOptions(sessionOptions);
        processor.setAthleteOptions(athleteOptions);

        processor.doProcessCompetitionHeader(stream(bytes), this::logImportMessage, () -> { });
        boolean identifySessionsOnly = sessionOptions == NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS;
        int sessionsProcessed = processor.doProcessGroups(stream(bytes), identifySessionsOnly, this::logImportMessage, () -> { });
        int athletesProcessed = processor.doProcessAthletes(stream(bytes), false, this::logImportMessage, () -> { });
        return new ImportResult(sessionsProcessed, athletesProcessed);
    }

    private ByteArrayInputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private void logImportMessage(String message) {
        logger.info("SBDE import: {}", message);
    }

    private void resetDatabase() {
        JPAService.close();
        JPAService.init(true, true);
        Config.initConfig();
        Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
        ProdData.insertInitialData(0);
        OwlcmsFactory.initDefaultFOP();

        NRegistrationFileProcessor processor = new NRegistrationFileProcessor(true, Locale.ENGLISH);
        processor.resetAthletes();
        processor.resetSessions();
        CategoryRepository.resetCodeMap();
    }

        private void assertActiveCategoryAvailable(String categoryName) {
        Category category = CategoryRepository.findActive().stream()
            .filter(c -> categoryName.equals(c.getDisplayName())
                || categoryName.equals(c.getNameWithAgeGroup())
                || categoryName.equals(Category.canonicalName(c.getDisplayName()))
                || categoryName.equals(Category.canonicalName(c.getNameWithAgeGroup())))
            .findFirst()
            .orElse(null);
        assertNotNull("active category should be present in stripped baseline: " + categoryName, category);
        assertNotNull("active category should be present in category cache: " + categoryName,
            Category.codeFromName(categoryName));
        }

    private int expectedAthleteCount(Set<String> sessionNames) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = SBDEImportTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull("SBDE JSON fixture should exist", input);
            JsonNode root = mapper.readTree(input);
            Set<Long> groupIds = new LinkedHashSet<>();
            for (JsonNode group : root.withArray("groups")) {
                if (sessionNames.contains(group.path("name").asText())) {
                    groupIds.add(group.path("id").asLong());
                }
            }
            int count = 0;
            for (JsonNode athlete : root.withArray("athletes")) {
                if (groupIds.contains(athlete.path("group").asLong())) {
                    count++;
                }
            }
            return count;
        }
    }

    private void assertSessionNames(Set<String> expected) {
        List<String> names = GroupRepository.findAll().stream()
                .map(Group::getName)
                .toList();
        assertEquals("session names", expected, new LinkedHashSet<>(names));
    }

    private void assertSessionsPresent(Set<String> expectedSessionNames) {
        for (String sessionName : expectedSessionNames) {
            assertNotNull("session should be present: " + sessionName, GroupRepository.findByName(sessionName));
        }
    }

    private Map<Long, LiftSnapshot> liftSnapshotsForSessions(Set<String> sessionNames) {
        Map<Long, LiftSnapshot> snapshots = new LinkedHashMap<>();
        for (String sessionName : sessionNames) {
            Group session = GroupRepository.findByName(sessionName);
            assertNotNull("session should exist: " + sessionName, session);
                List<Athlete> athletes = AthleteRepository.findAll().stream()
                    .filter(athlete -> athlete.getGroup() != null && sessionName.equals(athlete.getGroup().getName()))
                    .toList();
            assertFalse("session should have athletes: " + sessionName, athletes.isEmpty());
            for (Athlete athlete : athletes) {
                snapshots.put(athlete.getId(), LiftSnapshot.from(athlete));
            }
        }
        return snapshots;
    }

    private void assertLiftSnapshotsForSessions(Set<String> sessionNames, Map<Long, LiftSnapshot> expectedSnapshots) {
        Map<Long, LiftSnapshot> actualSnapshots = liftSnapshotsForSessions(sessionNames);
        assertEquals("session 1 and 2 athletes should still have the same lifts",
                expectedSnapshots, actualSnapshots);
    }

    private Athlete firstAthleteInSession(String sessionName) {
        Group session = GroupRepository.findByName(sessionName);
        assertNotNull("session should exist: " + sessionName, session);
        List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(session, null);
        assertFalse("session should have athletes: " + sessionName, athletes.isEmpty());
        return athletes.get(0);
    }

    private Athlete findAthleteById(Long id) {
        return JPAService.runInTransaction(em -> AthleteRepository.getById(id, em));
    }

    private void changeAthleteNameAndRecordEligibilities(Athlete athlete, String newFirstName,
            String recordEligibilities) {
        JPAService.runInTransaction(em -> {
            Athlete managed = em.find(Athlete.class, athlete.getId());
            Athlete edited = new Athlete();
            Athlete.conditionalCopy(edited, managed, true, true, true);
            edited.setFirstName(newFirstName);
            edited.setFederationCodes(recordEligibilities);
            Athlete.conditionalCopy(managed, edited, true, true, true);
            em.merge(managed);
            return null;
        });
    }

    private record ImportResult(int sessionsProcessed, int athletesProcessed) {
    }

    private record LiftSnapshot(String[] lifts) {
        static LiftSnapshot from(Athlete athlete) {
            return new LiftSnapshot(new String[] {
                    athlete.getSnatch1ActualLift(),
                    athlete.getSnatch2ActualLift(),
                    athlete.getSnatch3ActualLift(),
                    athlete.getCleanJerk1ActualLift(),
                    athlete.getCleanJerk2ActualLift(),
                    athlete.getCleanJerk3ActualLift()
            });
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LiftSnapshot other)) {
                return false;
            }
            return Arrays.equals(this.lifts, other.lifts);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.lifts);
        }

        @Override
        public String toString() {
            return Arrays.toString(this.lifts);
        }
    }
}