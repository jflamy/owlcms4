/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import app.owlcms.Main;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.websocket.WebSocketEventSender;
import app.owlcms.simulation.ProtocolScenario;

/**
 * End-to-end check of the WebSocket protocol: runs {@link ProtocolScenario} against a real tracker started in
 * development mode from the peer {@code tracker} checkout (the way it runs locally, with the linked
 * {@code tracker-core}), with learning mode recording every frame. Checks the recording and leaves it in
 * {@code target/protocol-recording/latest.jsonl} for protocol assertions and inspection.
 *
 * <p>
 * Needs Node and the tracker checkout next to this repository ({@code -DtrackerDir=...} or {@code TRACKER_DIR} to
 * override, {@code -Dnode=...} for the Node executable); skipped otherwise. Run it on its own:
 * {@code mvn -pl owlcms -am -Dtest=ProtocolRecordingTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test}
 * </p>
 *
 * <p>
 * With {@code -Dprotocol.watch=true}, the tracker attempt board and lifting order scoreboard open in the default
 * browser and the lifts run slowly enough to follow. In both modes the opening uses a 10 s countdown, 5 s athlete
 * introduction, then a 20 s snatch countdown with 5 s of officials' introduction starting after 5 s. The tracker stays
 * up 15 s after the session ends in watch mode. The tracker always uses port 51793, so open pages and bookmarks stay
 * valid between runs (each run starts a fresh tracker).
 * </p>
 */
public class ProtocolRecordingTest {

	private static final String UPDATE_KEY = "protocol-test-key";
	private static final int TRACKER_PORT = 51793;
	private static final long SETTLE_MILLIS = 300;
	private static final long WATCH_SETTLE_MILLIS = 2500;
	private static final long WATCH_PAGE_LOAD_MILLIS = 5000;
	private static final long WATCH_LINGER_MILLIS = 15_000;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static Process tracker;
	private static Path outputDir;

	private record RecordedMessage(Instant receivedAt, JsonNode message) {
	}

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		// done at server startup: category names use the translated gender code ("M 75")
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDownTests() {
		WebSocketEventSender.closeAll();
		stopTracker();
		System.clearProperty("videodata");
		System.clearProperty("videoDataKey");
		System.clearProperty("enableEmbeddedMqtt");
		OwlcmsFactory.resetFOPByName();
		JPAService.close();
	}

	@Test
	public void scenarioRecording() throws Exception {
		File trackerDir = trackerDir();
		String node = System.getProperty("node", "node");
		assumeTrue("tracker checkout with node_modules not found at " + trackerDir.getAbsolutePath(),
		        new File(trackerDir, "node_modules/vite/bin/vite.js").isFile());
		assumeTrue("node not available (" + node + ")", nodeAvailable(node));

		outputDir = Path.of("target", "protocol-recording").toAbsolutePath();
		deleteRecursively(outputDir);
		Files.createDirectories(outputDir);
		int port = TRACKER_PORT;
		startTracker(node, trackerDir, port);

		// competition: SINGLE_ATHLETE_GROUPS with M2 in M1, seven-person Adjudication Panel (saved in the database)
		DemoData.insertInitialData(1, null);
		Group session = ProtocolScenario.prepareSession();

		// production field of play, as created at startup: server timers, real display delays, and the WebSocket
		// forwarder for the configured destination (the scenario's session switch loads the athletes)
		System.setProperty("videodata", "ws://localhost:" + port + "/ws");
		System.setProperty("videoDataKey", UPDATE_KEY);
		System.setProperty("enableEmbeddedMqtt", "false");
		OwlcmsFactory.initFOPByName();
		FieldOfPlay fop = OwlcmsFactory.getFOPByName("A");
		assertTrue("seven-person Adjudication Panel", fop.isPanelProcedure());
		OwlcmsSession.setFop(fop);

		boolean watch = Boolean.getBoolean("protocol.watch");
		boolean includeIntroduction = Boolean.parseBoolean(System.getProperty("protocol.intro", "true"));
		ProtocolScenario scenario = new ProtocolScenario(fop, Thread::sleep,
		        watch ? WATCH_SETTLE_MILLIS : SETTLE_MILLIS, includeIntroduction);
		if (watch) {
			// send the database first so the board has data when it opens
			fop.fopEventPost(new FOPEvent.SwitchGroup(session, this));
			assertNotNull("tracker received the database", awaitRecording(text -> text.contains("database_zip"), 60));
			for (String board : List.of("attempt-board", "lifting-order")) {
				openBrowser("http://localhost:" + port + "/" + board + "?fop=" + fop.getName());
			}
			Thread.sleep(WATCH_PAGE_LOAD_MILLIS);
		}
		// the database is sent when the connection opens; sending a first update opens it
		new Thread(() -> {
			try {
				scenario.run(session);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, "protocol-scenario").start();

		Path recording = awaitRecording(text -> text.contains("database_zip"), 60);
		assertNotNull("tracker received the connection and the database", recording);
		assertNotNull("session done reached the tracker",
		        awaitRecording(text -> text.contains("GroupDone"), watch ? 600 : 300));
		checkPages(port);
		Thread.sleep(watch ? WATCH_LINGER_MILLIS : 1000);
		WebSocketEventSender.closeAll();
		stopTracker();

		Path latest = outputDir.resolve("latest.jsonl");
		Files.copy(recording, latest, StandardCopyOption.REPLACE_EXISTING);
		checkRecording(latest, includeIntroduction);
	}

	// ---------------------------------------------------------------- checks

	private void checkRecording(Path recording, boolean includeIntroduction) throws IOException {
		String raw = Files.readString(recording);
		assertFalse("the update key is never recorded", raw.contains(UPDATE_KEY));
		assertFalse("category names are complete (no null gender or age group)",
		        Pattern.compile("[cC]ategory(Name)?\\\\*\":\\\\*\"[^\"\\\\]*\\bnull\\b").matcher(raw).find());

		List<JsonNode> frames = new ArrayList<>();
		for (String line : Files.readAllLines(recording, StandardCharsets.UTF_8)) {
			if (!line.isBlank()) {
				frames.add(MAPPER.readTree(line));
			}
		}
		List<String> binaryTypes = new ArrayList<>();
		List<RecordedMessage> received = new ArrayList<>();
		for (JsonNode frame : frames) {
			if (!"in".equals(frame.path("dir").asString())) {
				JsonNode reply = MAPPER.readTree(frame.path("text").asString());
				assertFalse("no frame refused: " + reply, reply.path("status").asInt() == 401);
				continue;
			}
			if ("binary".equals(frame.path("frame").asString())) {
				binaryTypes.add(frame.path("type").asString());
			} else {
				received.add(new RecordedMessage(Instant.parse(frame.path("at").asString()),
				        MAPPER.readTree(frame.path("text").asString())));
			}
		}
		List<JsonNode> messages = received.stream().map(RecordedMessage::message).toList();
		if (includeIntroduction) {
			checkOpeningSequence(received);
		}

		assertTrue("database sent as a ZIP", binaryTypes.contains("database_zip"));
		assertTrue("translations sent", binaryTypes.contains("translations_zip"));
		for (String resource : List.of("database_zip", "translations_zip", "flags_zip", "logos_zip")) {
			assertTrue(resource + " sent at most once (start latch)", Collections.frequency(binaryTypes, resource) <= 1);
		}
		assertTrue("database checksum announced", messages.stream().anyMatch(m -> type(m).equals("database_metadata")));
		assertTrue("every message has the protocol version",
		        messages.stream().allMatch(m -> m.path("version").asString().equals(WebSocketEventSender.PROTOCOL_VERSION)));

		// updates and timers
		assertTrue(any(messages, m -> type(m).equals("update") && field(m, "uiEvent").equals("SwitchGroup")));
		List<String> expectedBreaks = includeIntroduction
		        ? List.of("BEFORE_INTRODUCTION", "FIRST_SNATCH", "FIRST_CJ", "CHALLENGE")
		        : List.of("FIRST_CJ", "CHALLENGE");
		for (String breakType : expectedBreaks) {
			assertTrue("break " + breakType, any(messages,
			        m -> type(m).equals("update") && field(m, "breakType").equals(breakType)));
		}
		assertTrue("break clock", any(messages,
		        m -> type(m).equals("timer") && field(m, "breakTimerEventType").equals("BreakStarted")));
		if (includeIntroduction) {
			assertTrue("introduction of athletes", any(messages,
			        m -> type(m).equals("update") && field(m, "ceremonyType").equals("INTRODUCTION")));
			assertTrue("introduction of officials during the countdown to the snatch", any(messages,
			        m -> type(m).equals("update") && field(m, "ceremonyType").equals("OFFICIALS_INTRODUCTION")
			                && field(m, "breakType").equals("FIRST_SNATCH")));
		}
		assertTrue("athlete clock", any(messages,
		        m -> type(m).equals("timer") && field(m, "athleteTimerEventType").equals("StartTime")));
		assertTrue("every update carries the decision state", messages.stream()
		        .filter(m -> type(m).equals("update"))
		        .allMatch(m -> m.path("payload").has("decisionState")));
		assertTrue("jury row enabled by the scenario's display default", any(messages,
		        m -> type(m).equals("update") && field(m, "juryDecisionsOnAttemptBoard").equals("true")));

		// decisions, in order
		List<String> decisionTypes = messages.stream()
		        .filter(m -> type(m).equals("decision"))
		        .map(m -> field(m, "decisionEventType"))
		        .toList();
		assertInOrder(decisionTypes, "FULL_DECISION", "JURY_UPDATE", "START_DELIBERATION", "PANEL_VOTE_CALLED",
		        "PANEL_VOTE", "JURY_DECISION", "CHALLENGE", "PANEL_VOTE_CALLED", "PANEL_VOTE", "JURY_DECISION");
		assertTrue("every decision message carries the decision state", messages.stream()
		        .filter(m -> type(m).equals("decision"))
		        .allMatch(m -> m.path("payload").has("decisionState")));

		JsonNode review = first(messages, m -> field(m, "decisionEventType").equals("START_DELIBERATION"))
		        .orElseThrow().path("payload").path("decisionState");
		assertEquals("deliberation", review.path("review").asString());
		assertTrue(review.path("panelReview").asBoolean());
		assertEquals("three referee cells", 3, review.path("refs").size());
		assertEquals("four jury cells", 4, review.path("jury").size());

		List<JsonNode> verdicts = messages.stream()
		        .filter(m -> field(m, "decisionEventType").equals("JURY_DECISION"))
		        .map(m -> m.path("payload").path("adjudicationDecision"))
		        .toList();
		assertEquals("two jury decisions", 2, verdicts.size());

		JsonNode snatch = verdicts.get(0);
		assertFalse("snatch: No Lift", snatch.path("decision").asBoolean());
		assertTrue(snatch.path("reversal").asBoolean());
		assertEquals(ProtocolScenario.OVERTURN_RULE, snatch.path("ruleCode").asInt());
		assertEquals("Snatch", snatch.path("liftTypeKey").asString());
		assertEquals("[true,true,true]", snatch.path("originalRefereeDecisions").toString());
		assertEquals("[false,false,false]", snatch.path("refereeVotes").toString());
		assertEquals("[false,false,false,false]", snatch.path("juryVotes").toString());

		JsonNode cj = verdicts.get(1);
		assertTrue("clean & jerk: Good Lift after the challenge", cj.path("decision").asBoolean());
		assertTrue(cj.path("reversal").asBoolean());
		assertEquals("Clean_and_Jerk", cj.path("liftTypeKey").asString());
		assertEquals("[false,false,true]", cj.path("originalRefereeDecisions").toString());
		assertEquals("[true,true,true,true]", cj.path("juryVotes").toString());

		assertTrue("session done", any(messages,
		        m -> type(m).equals("update") && field(m, "uiEvent").equals("GroupDone")));
	}

	private static void assertInOrder(List<String> actual, String... expected) {
		int index = 0;
		for (String type : actual) {
			if (index < expected.length && type.equals(expected[index])) {
				index++;
			}
		}
		assertEquals("decision types in order " + List.of(expected) + " within " + actual, expected.length, index);
	}

	private static void checkOpeningSequence(List<RecordedMessage> received) {
		RecordedMessage introductionTimer = firstRecorded(received,
		        m -> type(m).equals("timer") && field(m, "breakTimerEventType").equals("BreakStarted")
		                && field(m, "breakType").equals("BEFORE_INTRODUCTION"));
		assertEquals("10 s before introduction", "10000",
		        field(introductionTimer.message(), "breakMillisRemaining"));
		assertEquals("before introduction is timed", "false",
		        field(introductionTimer.message(), "indefiniteBreak"));

		RecordedMessage athletes = firstRecorded(received,
		        m -> field(m, "uiEvent").equals("CeremonyStarted") && field(m, "ceremonyType").equals("INTRODUCTION"));
		assertTrue("no timer during athlete introduction",
		        athletes.message().path("payload").path("indefiniteBreak").asBoolean());
		assertElapsed("before introduction countdown", introductionTimer, athletes, 10_000);

		RecordedMessage snatchTimer = firstRecorded(received,
		        m -> type(m).equals("timer") && field(m, "breakTimerEventType").equals("BreakStarted")
		                && field(m, "breakType").equals("FIRST_SNATCH"));
		assertEquals("20 s before snatch", "20000", field(snatchTimer.message(), "breakMillisRemaining"));
		assertElapsed("athlete introduction", athletes, snatchTimer, 5000);

		RecordedMessage officials = firstRecorded(received,
		        m -> field(m, "uiEvent").equals("CeremonyStarted")
		                && field(m, "ceremonyType").equals("OFFICIALS_INTRODUCTION"));
		assertElapsed("before officials introduction", snatchTimer, officials, 5000);
		RecordedMessage officialsDone = received.stream()
		        .filter(r -> r.receivedAt().isAfter(officials.receivedAt())
		                && field(r.message(), "uiEvent").equals("CeremonyDone"))
		        .findFirst().orElseThrow(() -> new AssertionError("officials introduction did not end"));
		assertElapsed("officials introduction", officials, officialsDone, 5000);

		String snatchStart = field(snatchTimer.message(), "breakStartTimeMillis");
		for (RecordedMessage event : List.of(officials, officialsDone)) {
			assertEquals("ceremony does not restart the countdown", snatchStart,
			        field(event.message(), "breakStartTimeMillis"));
			assertEquals("ceremony keeps the snatch break", "FIRST_SNATCH", field(event.message(), "breakType"));
			assertEquals("countdown still running", "BreakStarted", field(event.message(), "breakTimerEventType"));
			assertFalse("countdown is not indefinite", event.message().path("payload").path("indefiniteBreak").asBoolean());
		}
		assertEquals("only one snatch countdown start", 1L, received.stream()
		        .filter(r -> type(r.message()).equals("timer")
		                && field(r.message(), "breakTimerEventType").equals("BreakStarted")
		                && field(r.message(), "breakType").equals("FIRST_SNATCH"))
		        .count());
		assertEquals("about 15 s left when officials start", 15_000,
		        officials.message().path("payload").path("breakMillisRemaining").asInt(), 1000);
		assertEquals("about 10 s left when officials finish", 10_000,
		        officialsDone.message().path("payload").path("breakMillisRemaining").asInt(), 1000);

		RecordedMessage snatchDone = received.stream()
		        .filter(r -> r.receivedAt().isAfter(officialsDone.receivedAt())
		                && field(r.message(), "uiEvent").equals("BreakDone")
		                && field(r.message(), "breakType").equals("FIRST_SNATCH"))
		        .findFirst().orElseThrow(() -> new AssertionError("snatch countdown did not end"));
		assertElapsed("uninterrupted snatch countdown", snatchTimer, snatchDone, 20_000);
	}

	private static RecordedMessage firstRecorded(List<RecordedMessage> received, Predicate<JsonNode> condition) {
		return received.stream().filter(r -> condition.test(r.message())).findFirst()
		        .orElseThrow(() -> new AssertionError("expected opening event missing from recording"));
	}

	private static void assertElapsed(String label, RecordedMessage from, RecordedMessage to, long expectedMillis) {
		long elapsed = to.receivedAt().toEpochMilli() - from.receivedAt().toEpochMilli();
		assertEquals(label + " (milliseconds)", expectedMillis, elapsed, 1000);
	}

	private static boolean any(List<JsonNode> messages, Predicate<JsonNode> condition) {
		return messages.stream().anyMatch(condition);
	}

	private static Optional<JsonNode> first(List<JsonNode> messages, Predicate<JsonNode> condition) {
		return messages.stream().filter(condition).findFirst();
	}

	private static String type(JsonNode message) {
		return message.path("type").asString("");
	}

	private static String field(JsonNode message, String name) {
		return message.path("payload").path(name).asString("");
	}

	// ---------------------------------------------------------------- tracker process

	private static File trackerDir() {
		String configured = System.getProperty("trackerDir", System.getenv("TRACKER_DIR"));
		if (configured != null && !configured.isBlank()) {
			return new File(configured);
		}
		// surefire runs in the owlcms module: the tracker checkout is a peer of the repository
		return new File("../../tracker").getAbsoluteFile().toPath().normalize().toFile();
	}

	/** Opens the page in the default browser (macOS, Windows or Linux desktop). */
	private static void openBrowser(String url) throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		List<String> command = os.contains("mac") ? List.of("open", url)
		        : os.contains("win") ? List.of("rundll32", "url.dll,FileProtocolHandler", url)
		        : List.of("xdg-open", url);
		new ProcessBuilder(command).inheritIO().start();
	}

	private static boolean nodeAvailable(String node) {
		try {
			Process p = new ProcessBuilder(node, "--version").redirectErrorStream(true).start();
			return p.waitFor(20, TimeUnit.SECONDS) && p.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	/** With the database, translations and updates received, the pages show translated labels and the jury row. */
	private static void checkPages(int port) throws Exception {
		// HTTP/1.1: the default HTTP/2 attempt sends "Upgrade: h2c", which Node routes to the WebSocket upgrade handlers
		try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
			JsonNode scoreboard = readTrackerJson(client, port, "/api/scoreboard?type=lifting-order&fop=A&lang=en");
			assertTrue("scoreboard API succeeded", scoreboard.path("success").asBoolean());
			assertEquals("translated waiting message", "Waiting for next session.",
			        scoreboard.path("data").path("headers").path("waitingNextSession").asString());
			JsonNode attemptBoard = readTrackerJson(client, port, "/api/scoreboard?type=attempt-board&fop=A&lang=en");
			assertTrue("attempt board API succeeded", attemptBoard.path("success").asBoolean());
			assertTrue("attempt board follows the enabled jury display setting",
			        attemptBoard.path("data").path("options").path("showJuryVotes").asBoolean());
		}
	}

	private static JsonNode readTrackerJson(HttpClient client, int port, String route)
	        throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + route))
		        // the dev server compiles a route on its first request
		        .timeout(Duration.ofSeconds(60)).GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals("tracker response for " + route, 200, response.statusCode());
		return MAPPER.readTree(response.body());
	}

	private static void startTracker(String node, File trackerDir, int port) throws Exception {
		ProcessBuilder builder = new ProcessBuilder(node, "node_modules/vite/bin/vite.js", "dev", "--port",
		        Integer.toString(port), "--strictPort")
		        .directory(trackerDir)
		        .redirectErrorStream(true);
		builder.environment().put("LEARNING_MODE", "true");
		builder.environment().put("LEARNING_MODE_DIR", outputDir.toString());
		builder.environment().put("OWLCMS_UPDATEKEY", UPDATE_KEY);
		tracker = builder.start();

		Object ready = new Object();
		boolean[] initialized = { false };
		PrintWriter log = new PrintWriter(Files.newBufferedWriter(outputDir.resolve("tracker.log")), true);
		Thread reader = new Thread(() -> {
			try (BufferedReader in = new BufferedReader(
			        new InputStreamReader(tracker.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = in.readLine()) != null) {
					log.println(line);
					if (line.contains("[WebSocket] Server initialized")) {
						synchronized (ready) {
							initialized[0] = true;
							ready.notifyAll();
						}
					}
				}
			} catch (IOException e) {
				// process ended
			} finally {
				log.close();
			}
		}, "tracker-output");
		reader.setDaemon(true);
		reader.start();

		long deadline = System.currentTimeMillis() + 120_000;
		synchronized (ready) {
			while (!initialized[0] && System.currentTimeMillis() < deadline && tracker.isAlive()) {
				ready.wait(500);
			}
		}
		assertTrue("tracker started (see " + outputDir.resolve("tracker.log") + ")", initialized[0]);
	}

	private static void stopTracker() {
		if (tracker == null) {
			return;
		}
		tracker.descendants().forEach(ProcessHandle::destroy);
		tracker.destroy();
		try {
			if (!tracker.waitFor(10, TimeUnit.SECONDS)) {
				tracker.descendants().forEach(ProcessHandle::destroyForcibly);
				tracker.destroyForcibly();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		tracker = null;
	}

	/** @return the recording once its content satisfies the condition, null on timeout */
	private static Path awaitRecording(Predicate<String> condition, int seconds) throws Exception {
		Path dir = outputDir.resolve("recordings");
		long deadline = System.currentTimeMillis() + seconds * 1000L;
		while (System.currentTimeMillis() < deadline) {
			if (Files.isDirectory(dir)) {
				try (Stream<Path> files = Files.list(dir)) {
					for (Path file : files.filter(f -> f.toString().endsWith(".jsonl")).toList()) {
						if (condition.test(Files.readString(file))) {
							return file;
						}
					}
				}
			}
			Thread.sleep(250);
		}
		return null;
	}

	private static void deleteRecursively(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path p : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
				Files.deleteIfExists(p);
			}
		}
	}
}
