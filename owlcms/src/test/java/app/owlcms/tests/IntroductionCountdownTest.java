package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.common.eventbus.Subscribe;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.DemoData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.BreakController;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.monitors.websocket.WebSocketEventSender;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Production timers and WebSocket forwarding, without constructing Vaadin objects. */
@Category(SlowCategory.class)
public class IntroductionCountdownTest implements BreakController {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private final List<Map<String, Object>> recording = new CopyOnWriteArrayList<>();
	private final AtomicReference<Exception> receiverError = new AtomicReference<>();
	private final List<UIEvent.BreakStarted> displayStarts = new CopyOnWriteArrayList<>();
	private FieldOfPlay fop;

	@Test
	public void freshSnatchCountdownReachesDisplaysAndTrackerAndEndsAutomatically() throws Exception {
		CountDownLatch listening = new CountDownLatch(1);
		WebSocketServer receiver = new WebSocketServer(new InetSocketAddress("127.0.0.1", 0)) {
			@Override
			public void onStart() {
				listening.countDown();
			}

			@Override
			public void onOpen(WebSocket connection, ClientHandshake handshake) {
			}

			@Override
			public void onClose(WebSocket connection, int code, String reason, boolean remote) {
			}

			@Override
			public void onError(WebSocket connection, Exception error) {
				receiverError.compareAndSet(null, error);
			}

			@Override
			public void onMessage(WebSocket connection, String text) {
				try {
					JsonNode message = MAPPER.readTree(text);
					if (List.of("update", "timer", "decision").contains(message.path("type").asString())) {
						if (message.path("payload") instanceof ObjectNode payload) {
							payload.remove("updateKey");
						}
						recording.add(Map.of("at", System.currentTimeMillis(), "message", message));
					}
				} catch (Exception error) {
					receiverError.compareAndSet(null, error);
				}
			}
		};
		receiver.start();
		try {
			assertTrue("receiver starts", listening.await(5, TimeUnit.SECONDS));
			Main.injectSuppliers();
			JPAService.init(true, true);
			Config.initConfig();
			DemoData.insertInitialData(1, null);
			String url = "ws://127.0.0.1:" + receiver.getPort() + "/ws";
			System.setProperty("videodata", url);
			System.setProperty("videoDataKey", "introduction-test");
			System.setProperty("enableEmbeddedMqtt", "false");
			CountDownLatch connected = new CountDownLatch(1);
			// Capture timer traffic without the application-server startup resources.
			WebSocketEventSender.getOrCreate(url, () -> url, connected::countDown, "introduction-test");
			assertTrue("capture connection opens", connected.await(5, TimeUnit.SECONDS));
			OwlcmsFactory.initFOPByName();
			this.fop = OwlcmsFactory.getFOPByName("A");
			this.fop.getUiEventBus().register(this);
			this.fop.fopEventPost(new FOPEvent.SwitchGroup(GroupRepository.findByName("M1"), this));

			this.fop.startCountdown(BreakType.BEFORE_INTRODUCTION, 2000, this);
			assertTrue("pre-introduction timer runs", this.fop.getBreakTimer().isRunning());
			await("introduction ceremony starts", () -> this.fop.getCeremonyType() == CeremonyType.INTRODUCTION, 7000);
			assertFalse("no countdown during athlete introductions", this.fop.getBreakTimer().isRunning());
			Thread.sleep(1000);
			this.fop.fopEventPost(new FOPEvent.CeremonyDone(CeremonyType.INTRODUCTION, this));

			long requestedAt = System.currentTimeMillis();
			int previousDisplays = this.displayStarts.size();
			this.fop.startCountdown(BreakType.FIRST_SNATCH, 4000, this);
			assertTrue("End Introduction starts the server timer", this.fop.getBreakTimer().isRunning());
			assertEquals(BreakType.FIRST_SNATCH, this.fop.getBreakType());
			await("new countdown reaches both buses", () -> this.displayStarts.size() > previousDisplays
			        && !snatchStartsSince(requestedAt).isEmpty(), 2000);
			assertEquals("one start notification reaches local displays", previousDisplays + 1, this.displayStarts.size());
			assertEquals("one start notification reaches tracker", 1, snatchStartsSince(requestedAt).size());
			JsonNode start = snatchStartsSince(requestedAt).get(0);
			long startTime = Long.parseLong(start.path("breakStartTimeMillis").asString());
			long deadline = startTime + start.path("breakMillisRemaining").asInt();
			assertTrue("new start is not the pre-introduction anchor", startTime >= requestedAt);
			assertEquals("full duration is forwarded", 4000, start.path("breakMillisRemaining").asInt());
			await("initial snatch snapshot forwarded", () -> this.recording.stream()
			        .filter(entry -> ((Long) entry.get("at")) >= requestedAt)
			        .anyMatch(entry -> entry.get("message") instanceof JsonNode message
			                && "update".equals(message.path("type").asString())
			                && "BreakStarted".equals(message.path("payload").path("uiEvent").asString())
			                && "FIRST_SNATCH".equals(message.path("payload").path("breakType").asString())), 2000);
			for (Map<String, Object> entry : this.recording) {
				JsonNode message = (JsonNode) entry.get("message");
				JsonNode payload = message.path("payload");
				if (((Long) entry.get("at")) >= requestedAt
				        && "update".equals(message.path("type").asString())
				        && "BreakStarted".equals(payload.path("uiEvent").asString())) {
					assertEquals("initial snapshot uses the new start", startTime,
					        payload.path("breakStartTimeMillis").asLong());
				}
			}
			Thread.sleep(1000);
			assertDeadline(snapshot(), deadline);

			int previousFrames = this.recording.size();
			this.fop.pushOutUIEvent(new UIEvent.SetTime(60000, this, "replace last timer map", this.fop));
			await("athlete timer replaces the forwarding cache", () -> this.recording.stream().skip(previousFrames)
			        .anyMatch(entry -> entry.get("message") instanceof JsonNode message
			                && "timer".equals(message.path("type").asString())
			                && "SetTime".equals(message.path("payload").path("athleteTimerEventType").asString())), 2000);
			assertDeadline(snapshot(), deadline);
			await("countdown expiry starts lifting", () -> this.fop.getState() != FOPState.BREAK, 6000);
			assertFalse(this.fop.getBreakTimer().isRunning());

			// A fresh countdown must also replace a still-running clock, not subtract its elapsed time.
			this.fop.startCountdown(BreakType.BEFORE_INTRODUCTION, 10000, this);
			Thread.sleep(300);
			long replacementRequestedAt = System.currentTimeMillis();
			this.fop.startCountdown(BreakType.FIRST_SNATCH, 2000, this);
			assertEquals("reset duration, not remaining time from old clock", 2000,
			        this.fop.getBreakTimer().getTimeRemaining());
			assertTrue("replacement timer runs", this.fop.getBreakTimer().isRunning());
			await("replacement start forwarded", () -> !snatchStartsSince(replacementRequestedAt).isEmpty(), 2000);
			assertDeadline(snapshot(), replacementRequestedAt + 2000);
			await("replacement deadline, not the original ten seconds", () -> this.fop.getState() != FOPState.BREAK, 5000);
			await("final lifting update forwarded before teardown", () -> this.recording.stream()
			        .filter(entry -> ((Long) entry.get("at")) >= replacementRequestedAt)
			        .anyMatch(entry -> entry.get("message") instanceof JsonNode message
			                && "LiftingOrderUpdated".equals(message.path("payload").path("uiEvent").asString())
			                && "CURRENT_ATHLETE_DISPLAYED".equals(message.path("payload").path("fopState").asString())), 3000);
			assertEquals("receiver errors", null, this.receiverError.get());
		} finally {
			if (this.fop != null && this.fop.getBreakTimer().isRunning()) {
				this.fop.getBreakTimer().stop();
			}
			OwlcmsFactory.resetFOPByName();
			WebSocketEventSender.closeAll();
			receiver.stop(2000);
			System.clearProperty("videodata");
			System.clearProperty("videoDataKey");
			System.clearProperty("enableEmbeddedMqtt");
			JPAService.close();
			Files.writeString(Path.of("target", "introduction-countdown.json"),
			        MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this.recording));
		}
	}

	@Subscribe
	public void onDisplayStart(UIEvent.BreakStarted event) {
		this.displayStarts.add(event);
	}

	private List<JsonNode> snatchStartsSince(long requestedAt) {
		return this.recording.stream().filter(entry -> ((Long) entry.get("at")) >= requestedAt)
		        .map(entry -> (JsonNode) entry.get("message"))
		        .filter(message -> "timer".equals(message.path("type").asString()))
		        .map(message -> message.path("payload"))
		        .filter(payload -> "FIRST_SNATCH".equals(payload.path("breakType").asString())
		                && "BreakStarted".equals(payload.path("breakTimerEventType").asString()))
		        .toList();
	}

	private JsonNode snapshot() throws Exception {
		int previousFrames = this.recording.size();
		Method pushUpdate = WebSocketEventForwarder.class.getDeclaredMethod("pushUpdate", UIEvent.class);
		pushUpdate.setAccessible(true);
		pushUpdate.invoke(this.fop.getWebSocketEventForwarder(), new Object[] { null });
		await("snapshot reaches tracker", () -> this.recording.stream().skip(previousFrames)
		        .anyMatch(entry -> entry.get("message") instanceof JsonNode message
		                && "update".equals(message.path("type").asString())
		                && "InitialSync".equals(message.path("payload").path("uiEvent").asString())), 2000);
		return this.recording.stream().skip(previousFrames).map(entry -> (JsonNode) entry.get("message"))
		        .filter(message -> "update".equals(message.path("type").asString()))
		        .map(message -> message.path("payload"))
		        .filter(payload -> "InitialSync".equals(payload.path("uiEvent").asString()))
		        .findFirst().orElseThrow();
	}

	private void assertDeadline(JsonNode snapshot, long deadline) {
		assertEquals("running state reconstructed even after an athlete timer", "BreakStarted",
		        snapshot.path("breakTimerEventType").asString());
		int remaining = snapshot.path("breakMillisRemaining").asInt();
		assertTrue("remaining time is live", remaining > 0 && remaining < 4000);
		assertEquals("snapshot still targets the new deadline", deadline,
		        System.currentTimeMillis() + remaining, 300);
	}

	private void await(String description, BooleanSupplier ready, long timeout) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeout;
		while (!ready.getAsBoolean() && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		assertTrue(description, ready.getAsBoolean());
	}
}
