package playwright;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

/**
 * Visual check of a jury review scenario against a running owlcms. The settings are made through the screens; the
 * referees, the clock and the jury box are simulated with MQTT; the Jury Chair (Jury Control) and the Speaker are
 * browser clicks. Each step captures the Speaker, Jury Control, attempt board and warmup scoreboard pages and writes
 * an HTML report with the expected result and what was read from the pages.
 *
 * <p>
 * Scenarios ({@link #SCENARIOS}):
 * <ul>
 * <li>four-member jury with VPT (1-5). The review starts automatically (the jury votes against the referees) or with
 * the Challenge button of Jury Control (split jury); Summon All from the jury box calls the vote; the seven officials
 * vote.</li>
 * <li>jury vote, no seven-person panel: three-member jury without VPT (6-10), four-member jury without VPT (11-15),
 * a three-member jury with VPT behaves exactly as without VPT, so it has no scenarios of its own.
 * <li>16: four-member jury with VPT, a jury box that does not work: the vote never completes and the Chair gives the
 * decision with the No Lift button and the rule.
 * <li>17 (VPT) and 18 (3-member jury): jury box only, the Chair never confirms; the Speaker enters the decision. No automatic stop: the Jury Chair starts the review with Jury Deliberation
 * (unanimous jury against the referees) or Challenge (split jury). The vote is open at once and called verbally; the
 * three jurors punch in their votes; the referees do not vote; unanimity is needed to reverse.</li>
 * </ul>
 * In both cases the Chair applies the result (the rule for a No Lift, Confirm Good Lift); the Speaker closes the
 * dialog; lifting resumes; the Chair presses Resume. The settings are applied through the screens before each
 * scenario.
 * <p>
 * Each scenario has its own folder {@code <out>/<scenario>/}, cleaned and rewritten each time it runs, with
 * {@code timeline.html} (screens side by side) and {@code index.html} (details). After every run,
 * {@code <out>/summary.html} lists the last run of every scenario.
 *
 * <pre>
 * Options: --base=http://localhost:8080 --mqtt=tcp://localhost:1883 --fop=A --group=M1 --out=playwright/reports
 *          --scenario=1-vpt-reversal-nolift,3-vpt-challenge-success --headed
 * </pre>
 */
public class JuryScenario {

	private static final String ORANGE = "rgb(249, 115, 22)";

	record Options(String base, String mqttUri, String fop, String group, Path out, boolean headed,
	        List<String> scenarios) {

		static Options parse(String[] args) {
			Map<String, String> m = new LinkedHashMap<>();
			for (String a : args) {
				if (a.startsWith("--")) {
					int eq = a.indexOf('=');
					m.put(eq > 0 ? a.substring(2, eq) : a.substring(2), eq > 0 ? a.substring(eq + 1) : "true");
				}
			}
			return new Options(stripSlash(m.getOrDefault("base", "http://localhost:8080")),
			        m.getOrDefault("mqtt", "tcp://localhost:1883"), m.getOrDefault("fop", "A"), m.get("group"),
			        Path.of(m.getOrDefault("out", "playwright/reports")), m.containsKey("headed"),
			        List.of(m.getOrDefault("scenario", String.join(",", SCENARIOS.keySet())).split(",")));
		}

		private static String stripSlash(String s) {
			return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
		}
	}

	/** the jury in use, applied through the settings screens before the scenario */
	enum Setup {
		VPT4("four-member jury with VPT (seven-person Adjudication Panel)", true, 4),
		JURY3("three-member jury, no VPT", false, 3),
		JURY4("four-member jury, no VPT (unanimity, manual trigger)", false, 4);

		final String description;
		final boolean vpt;
		final int jurors;
		/** the seven-person Adjudication Panel procedure: four-member jury and VPT */
		final boolean panel;

		Setup(String description, boolean vpt, int jurors) {
			this.description = description;
			this.vpt = vpt;
			this.jurors = jurors;
			this.panel = jurors == 4 && vpt;
		}
	}

	/**
	 * A jury review.
	 *
	 * @param setup      the jury in use
	 * @param refs       the referees' decision on the lift
	 * @param liftJury   the jury votes on the lift
	 * @param challenge  true: the jury split leaves the referees' decision; the review is started with the Challenge
	 *                   button of Jury Control. false: with VPT the jury votes cause the automatic Adjudication Panel
	 *                   Review; without VPT the unanimous jury is against the referees and the Chair starts a
	 *                   deliberation
	 * @param reviewRefs the referees' votes in the review vote (null: the referees do not vote)
	 * @param reviewJury the jury votes in the review vote
	 * @param goodResult the result of the review vote
	 * @param brokenJuror a jury box that never sends a vote (1..4), 0 if none: the votes of that juror are ignored
	 * @param speakerDecides the jury uses only the jury box; the Chair never confirms the result nor gives a rule, the
	 *                       Speaker enters the decision in the "Waiting for decision" dialog
	 * @param startClockAnyway a jury device never votes; the Speaker abandons the incomplete vote and starts the next
	 *                         clock, leaving the referees' decision in place
	 */
	record Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
	        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror, boolean speakerDecides,
	        boolean startClockAnyway) {

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, 0, false, false);
		}

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, brokenJuror, false,
			        false);
		}

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror, boolean speakerDecides) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, brokenJuror,
			        speakerDecides, false);
		}
	}

	private static String[] votes(String... v) {
		return v;
	}

	static final Map<String, Scenario> SCENARIOS = new LinkedHashMap<>();
	static {
		add(new Scenario("1-vpt-reversal-nolift",
		        "1. VPT: Good Lift reversed to No Lift by the panel (automatic review), the Jury Chair gives the rule",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("2-vpt-reversal-goodlift",
		        "2. VPT: No Lift reversed to Good Lift by the panel (automatic review), the Jury Chair confirms",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        votes("bad", "bad", "good"), votes("good", "good", "good", "good"), true));
		add(new Scenario("3-vpt-challenge-success",
		        "3. VPT: successful challenge, split jury, No Lift reversed to Good Lift, the Jury Chair confirms",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        votes("good", "good", "good"), votes("good", "good", "good", "good"), true));
		add(new Scenario("4-vpt-challenge-fail",
		        "4. VPT: unsuccessful challenge, split jury, No Lift confirmed, the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        votes("bad", "bad", "good"), votes("bad", "bad", "good", "good"), false));
		add(new Scenario("5-vpt-confirmation-nolift",
		        "5. VPT: No Lift confirmed by the panel (automatic review), the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "good"), false));
		add(new Scenario("6-jury3-reversal-nolift",
		        "6. 3-member jury, no VPT: unanimous jury against a Good Lift, the Chair starts a deliberation, "
		                + "reversed to No Lift, the Jury Chair gives the rule",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad"), false));
		add(new Scenario("7-jury3-reversal-goodlift",
		        "7. 3-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, "
		                + "reversed to Good Lift, the Jury Chair confirms",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"), false,
		        null, votes("good", "good", "good"), true));
		add(new Scenario("8-jury3-challenge-success",
		        "8. 3-member jury, no VPT: successful challenge, split jury, unanimous review vote, No Lift reversed "
		                + "to Good Lift, the Jury Chair confirms",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "bad", "bad"), true,
		        null, votes("good", "good", "good"), true));
		add(new Scenario("9-jury3-challenge-fail",
		        "9. 3-member jury, no VPT: unsuccessful challenge, review vote not unanimous, No Lift stands, the Jury "
		                + "Chair gives the rule",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "bad", "bad"), true,
		        null, votes("good", "good", "bad"), false));
		add(new Scenario("10-jury3-confirmation-nolift",
		        "10. 3-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, No Lift "
		                + "confirmed by the unanimous review vote, the Jury Chair gives the rule",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"), false,
		        null, votes("bad", "bad", "bad"), false));
		add(new Scenario("11-jury4-reversal-nolift",
		        "11. 4-member jury, no VPT: unanimous jury against a Good Lift, the Chair starts a deliberation, "
		                + "reversed to No Lift, the Jury Chair gives the rule",
		        Setup.JURY4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("12-jury4-reversal-goodlift",
		        "12. 4-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, "
		                + "reversed to Good Lift, the Jury Chair confirms",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        null, votes("good", "good", "good", "good"), true));
		add(new Scenario("13-jury4-challenge-success",
		        "13. 4-member jury, no VPT: successful challenge, split jury, unanimous review vote, No Lift reversed "
		                + "to Good Lift, the Jury Chair confirms",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        null, votes("good", "good", "good", "good"), true));
		add(new Scenario("14-jury4-challenge-fail",
		        "14. 4-member jury, no VPT: unsuccessful challenge, review vote not unanimous (3 to 1), No Lift "
		                + "stands, the Jury Chair gives the rule",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        null, votes("good", "good", "good", "bad"), false));
		add(new Scenario("15-jury4-confirmation-nolift",
		        "15. 4-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, No Lift "
		                + "confirmed by the unanimous review vote, the Jury Chair gives the rule",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        null, votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("16-vpt-broken-juror-nolift",
		        "16. VPT: jury box 1 does not work, the Chair starts a deliberation and calls the vote, which never "
		                + "completes; the Chair gives No Lift with the button, then the rule",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "bad"), false, 1));
		add(new Scenario("17-vpt-speaker-enters-nolift",
		        "17. VPT, jury box only: automatic review, the jury box calls the vote, No Lift result; the Jury "
		                + "Chair never confirms nor gives the rule, the Speaker enters No Lift",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "bad"), false, 0, true));
		add(new Scenario("18-jury3-speaker-enters-nolift",
		        "18. 3-member jury, no VPT, jury box only: the jury box starts the deliberation, unanimous No Lift "
		                + "vote; the Jury Chair never confirms nor gives the rule, the Speaker enters No Lift",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad"), false, 0, true));
		add(new Scenario("19-vpt-broken-juror-start-anyway",
		        "19. VPT: jury box 1 does not work; the Speaker starts the next clock anyway, abandoning the "
		                + "incomplete jury vote and leaving the referees' Good Lift in place",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        null, null, true, 1, false, true));
	}

	private static void add(Scenario sc) {
		SCENARIOS.put(sc.id(), sc);
	}

	/** a check on one page: a JavaScript predicate waited for, with a description for the report */
	record Check(String page, String description, String predicate) {
	}

	record StepResult(int number, String title, List<String> actions, String expected, List<String> checks,
	        Map<String, String> shots, Map<String, String> observed) {
	}

	private final Options options;
	private final Map<String, Page> pages = new LinkedHashMap<>();
	private List<StepResult> results = new ArrayList<>();
	private Scenario scenario;
	private final List<String> deviceMessages = new ArrayList<>();
	private Path reportDir;
	private MqttClient mqtt;
	private List<String> pendingActions = new ArrayList<>();
	/** the settings read back after they were applied, shown in the Starting point step */
	private List<Check> setupChecks = List.of();
	private int failures;
	private int scenarioFailures;

	JuryScenario(Options options) {
		this.options = options;
	}

	public static void main(String[] args) throws Exception {
		Options options = Options.parse(args);
		JuryScenario scenario = new JuryScenario(options);
		int failures = scenario.run();
		System.exit(failures == 0 ? 0 : 1);
	}

	int run() throws Exception {
		Path out = this.options.out();
		Files.createDirectories(out);
		this.mqtt = connectMqtt();

		BrowserType.LaunchOptions launch = new BrowserType.LaunchOptions().setHeadless(!this.options.headed());
		launch.setChannel(UpdateCheck.browserChannel());
		try (Playwright playwright = Playwright.create(); Browser browser = playwright.chromium().launch(launch)) {
			Page settings = newPage(browser, 1920, 1080);

			String fop = "fop=" + this.options.fop();
			// the Speaker page loads the group given in its URL
			String group = this.options.group() != null
			        ? "&group=" + URLEncoder.encode(this.options.group(), StandardCharsets.UTF_8)
			        : "";
			this.pages.put("speaker", open(browser, "lifting/announcer?" + fop + group, 1920, 1080));
			this.pages.put("jury", open(browser, "jurykeypad?" + fop, 1280, 800));
			this.pages.put("attempt", open(browser, "displays/attemptBoard?" + fop, 1920, 1080));
			this.pages.put("scoreboard", open(browser, "displays/resultsLeaders?" + fop + "&decisionSection=true",
			        1920, 1080));

			for (String id : this.options.scenarios()) {
				Scenario sc = SCENARIOS.get(id.trim());
				if (sc == null) {
					throw new IllegalArgumentException("unknown scenario " + id + "; known: " + SCENARIOS.keySet());
				}
				this.scenario = sc;
				this.results = new ArrayList<>();
				this.scenarioFailures = 0;
				// one folder per scenario, cleaned and rewritten on each run of that scenario
				this.reportDir = out.resolve(sc.id());
				deleteRecursively(this.reportDir);
				Files.createDirectories(this.reportDir);
				System.out.println("== " + sc.id() + ": " + sc.title());
				try {
					configure(settings, sc.setup());
					String athlete = currentAthlete(this.pages.get("speaker"));
					if (athlete == null || athlete.isBlank()) {
						throw new IllegalStateException("no current athlete on platform " + this.options.fop()
						        + ": load a group and start lifting first");
					}
					runReview(athlete);
				} finally {
					writeReport();
					writeTimeline();
					writeStatus();
					System.out.println("timeline: " + this.reportDir.resolve("timeline.html").toAbsolutePath());
				}
			}
		} finally {
			writeSummary(out);
			this.pages.clear();
			this.mqtt.disconnect();
			this.mqtt.close();
		}
		System.out.println("summary: " + out.resolve("summary.html").toAbsolutePath());
		System.out.println(this.failures == 0 ? "all checks passed" : this.failures + " check(s) failed");
		return this.failures;
	}

	/** status.txt in the scenario folder: time of the run, number of failed checks, number of steps */
	private void writeStatus() throws IOException {
		String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
		Files.writeString(this.reportDir.resolve("status.txt"),
		        when + "\n" + this.scenarioFailures + "\n" + this.results.size() + "\n", StandardCharsets.UTF_8);
	}

	/** summary.html: the last run of every scenario, from the status.txt of each scenario folder */
	private void writeSummary(Path out) throws IOException {
		StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Jury scenarios"
		        + "</title><style>body{font-family:sans-serif;margin:1em}table{border-collapse:collapse}"
		        + "td,th{border:1px solid #ccc;padding:4px 8px;text-align:left}.pass{color:green}"
		        + ".fail{color:#c00;font-weight:bold}.none{color:#888}</style></head><body><h1>Jury scenarios</h1>"
		        + "<p>").append(esc(this.options.base())).append(", platform ").append(esc(this.options.fop()))
		        .append("</p><table><tr><th>Scenario</th><th>Last run</th><th>Result</th><th></th></tr>");
		for (Scenario sc : SCENARIOS.values()) {
			Path status = out.resolve(sc.id()).resolve("status.txt");
			sb.append("<tr><td>").append(esc(sc.title())).append("</td>");
			if (Files.exists(status)) {
				List<String> lines = Files.readAllLines(status, StandardCharsets.UTF_8);
				int failed = lines.size() > 1 ? Integer.parseInt(lines.get(1).trim()) : -1;
				String steps = lines.size() > 2 ? lines.get(2).trim() : "?";
				sb.append("<td>").append(esc(lines.get(0))).append("</td><td class='")
				        .append(failed == 0 ? "pass'>all checks passed" : "fail'>" + failed + " check(s) failed")
				        .append(" (").append(steps).append(" steps)</td><td><a href='").append(sc.id())
				        .append("/timeline.html'>timeline</a> · <a href='").append(sc.id())
				        .append("/index.html'>details</a></td>");
			} else {
				sb.append("<td class='none'>not run yet</td><td></td><td></td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table></body></html>");
		Files.writeString(out.resolve("summary.html"), sb.toString(), StandardCharsets.UTF_8);
	}

	private static void deleteRecursively(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			return;
		}
		try (var paths = Files.walk(dir)) {
			for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}

	// ------------------------------------------------------------------ scenario

	private void runReview(String athlete) throws Exception {
		if (this.scenario.startClockAnyway()) {
			runStartClockAnyway(athlete);
		} else if (this.scenario.speakerDecides()) {
			runSpeakerDecidesReview(athlete);
		} else if (this.scenario.brokenJuror() > 0) {
			runBrokenJurorReview(athlete);
		} else if (this.scenario.setup().panel) {
			runPanelReview(athlete);
		} else {
			runJuryReview(athlete);
		}
	}

	/**
	 * A jury device never votes on the lift. The Speaker attempts to start the next clock, owlcms refuses it and asks
	 * for confirmation, then Start clock anyway abandons the incomplete jury vote and leaves the referees' decision.
	 */
	private void runStartClockAnyway(String athlete) throws Exception {
		Scenario sc = this.scenario;
		int broken = sc.brokenJuror();
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String refsText = describe(refs);
		Page speaker = this.pages.get("speaker");

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ". Jury box " + broken
		        + " does not work: its votes are never sent.", startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; 4 empty jury circles; the "
		        + "decision is held on the boards until the jury has voted.", List.of());

		for (int j = 1; j <= 4; j++) {
			if (j != broken) {
				juror(j, sc.liftJury()[j - 1]);
			}
		}
		pause(1500);
		step("Three of four jurors vote on the lift; jury box " + broken + " sends nothing",
		        "The jury vote on the lift is incomplete, so there is no automatic Adjudication Panel Review. Jury "
		                + "Control shows 3 grey \"voted\" circles. Speaker: no dialog.",
		        List.of(new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		clock("start");
		pause(1500);
		step("Speaker tries to start the next clock",
		        "owlcms refuses the clock start. The Speaker sees \"The jury has not finished voting\" with Start "
		                + "clock anyway. The decision and the three jury votes stay displayed.",
		        List.of(new Check("speaker", "pending-jury dialog is open",
		                dialogContains("jury has not finished voting")),
		                new Check("speaker", "Start clock anyway is offered", buttonVisible("Start clock anyway"))));

		click(speaker, "vaadin-button:has-text('Start clock anyway')", "Speaker clicks Start clock anyway");
		pause(1500);
		step("Speaker: Start clock anyway",
		        "The incomplete jury vote is discarded and no jury decision is entered. The referees' " + liftDecision
		                + " stands. The next athlete's clock is running; the jury lights are cleared and Jury Control "
		                + "is back to Deliberation / Challenge.",
		        List.of(new Check("speaker", "the pending-jury dialog is closed",
		                "!(" + dialogContains("jury has not finished voting") + ")"),
		                new Check("jury", "Deliberation and Challenge are offered",
		                        buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")),
		                new Check("attempt", "no jury decision was shown",
		                        "!document.body.innerText.includes('JURY REVERSAL') && "
		                                + "!document.body.innerText.includes('JURY CONFIRMATION')")));
	}

	private void runPanelReview(String athlete) throws Exception {
		Scenario sc = this.scenario;
		String[] refs = sc.refs();
		String[] liftJury = sc.liftJury();
		boolean good = sc.goodResult();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String reviewResult = good ? "Good Lift" : "No Lift";
		String outcomeWord = liftDecision.equals(reviewResult) ? "confirmed" : "reversal";
		String refsText = describe(refs);

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ".",
		        startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; the decision stays shown "
		        + "while the jury votes; 4 empty jury circles on the boards.", List.of());

		juror(1, liftJury[0]);
		juror(2, liftJury[1]);
		juror(3, liftJury[2]);
		pause(1000);
		step("Three of four jurors have voted on the lift",
		        "No review yet: the jury votes are not complete. Jury Control: Deliberation / Challenge still "
		                + "offered; 3 grey \"voted\" jury circles, values hidden. Boards: the referees' "
		                + liftDecision + " still shown, 3 grey jury circles. Speaker: no dialog.",
		        List.of(new Check("jury", "Deliberation and Challenge still offered", buttonVisible("Jury Deliberation")),
		                new Check("speaker", "no jury dialog yet", "!(" + dialogContains("Waiting for") + ")")));

		juror(4, liftJury[3]);
		List<Check> reviewChecks = List.of(new Check("jury", "Call for Votes is orange",
		        buttonColor("Call for Votes", ORANGE)),
		        new Check("jury", "No Lift and Good Lift replace Deliberation and Challenge",
		                buttonVisible("No Lift") + " && " + buttonVisible("Good Lift") + " && !"
		                        + buttonVisible("Jury Deliberation")),
		        new Check("jury", "Summon Referees (all) is disabled", buttonDisabled("Summon Referees")),
		        new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for")));
		if (sc.challenge()) {
			pause(1000);
			step("Fourth juror: split jury, the referees' decision stands",
			        "Jury " + describe(liftJury) + ": with the referees' " + refsText + ", the seven-person majority "
			                + "agrees with the " + liftDecision + ", so there is no automatic review. The four jury "
			                + "votes are revealed. Jury Control: Deliberation / Challenge offered. Speaker: no dialog.",
			        List.of(new Check("jury", "Deliberation and Challenge still offered",
			                buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")),
			                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

			// the decision display ends and lifting resumes before the coach's challenge
			pause(4000);
			click(this.pages.get("jury"), "vaadin-button:has-text('Challenge')",
			        "Jury Chair clicks Challenge on Jury Control");
			pause(1500);
			step("Challenge: the review starts",
			        "Challenge break, same procedure as the automatic review: the referees are summoned to the video "
			                + "review, the votes on the lift stay displayed. Jury Control: No Lift / Good Lift / Call "
			                + "for Votes (orange) / Resume; Summon Referees (all) disabled. Speaker: \"Waiting for "
			                + "decision\" dialog titled Challenge. Boards: Challenge message, the votes on the lift.",
			        List.of(reviewChecks.get(0), reviewChecks.get(1), reviewChecks.get(2), reviewChecks.get(3),
			                new Check("speaker", "the dialog is titled Challenge", dialogContains("Challenge"))));
		} else {
			step("Fourth juror votes " + describe(liftJury) + ": automatic Adjudication Panel Review",
			        "The seven-person majority is against the referees' " + liftDecision + ": the competition stops "
			                + "for an Adjudication Panel Review. Jury Control: No Lift / Good Lift / Call for Votes "
			                + "(orange) / Resume; Summon Referees (all) disabled; the votes on the lift stay displayed. "
			                + "Speaker: \"Waiting for decision\" dialog with Good Lift / No Lift / Resume. Boards: "
			                + "review message, the votes on the lift.",
			        reviewChecks);
		}

		publish("owlcms/jurybox/summon/" + this.options.fop(), "all");
		pause(1500);
		step("Jury box Summon All: the vote is called",
		        "Same event as Call for Votes on Jury Control: all cells cleared, referees buzzed. Jury Control: "
		                + "Call for Votes no longer orange. Boards: empty cells.",
		        List.of(new Check("jury", "Call for Votes no longer orange",
		                "!(" + buttonColor("Call for Votes", ORANGE) + ")")));

		String[] reviewRefs = sc.reviewRefs();
		String[] reviewJury = sc.reviewJury();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, reviewRefs[r]);
		}
		juror(1, reviewJury[0]);
		juror(2, reviewJury[1]);
		juror(3, reviewJury[2]);
		pause(1500);
		step("Six of seven have voted", "Grey \"voted\" cells everywhere, values hidden.", List.of());

		juror(4, reviewJury[3]);
		pause(1500);
		finishReview("Seventh vote", "the seven votes (referees " + describe(reviewRefs) + ", jury "
		        + describe(reviewJury) + ")", outcomeWord, reviewResult);
	}

	private static final String[] COUNT = { "zero", "one", "two", "three", "four", "five" };
	private static final String[] ORDINAL = { "", "First", "Second", "Third", "Fourth", "Fifth" };

	/**
	 * Jury vote (no seven-person panel: 3 members with or without VPT, 4 members without VPT): no automatic trigger;
	 * the Chair starts the review; the referees' decision stays shown and the referees do not vote again; the jury
	 * lights are cleared, the vote is called verbally and the jurors punch in their votes; unanimity reverses the
	 * referees. A four-member jury holds the decision on the boards until its four votes are in.
	 */
	private void runJuryReview(String athlete) throws Exception {
		Scenario sc = this.scenario;
		int n = sc.setup().jurors;
		String all = COUNT[n];
		String last = ORDINAL[n];
		String allButLast = COUNT[n - 1];
		String[] refs = sc.refs();
		String[] liftJury = sc.liftJury();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String reviewResult = sc.goodResult() ? "Good Lift" : "No Lift";
		String outcomeWord = liftDecision.equals(reviewResult) ? "confirmed" : "reversal";
		String refsText = describe(refs);

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ".",
		        startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; " + n + " empty jury "
		        + "circles on the boards." + (n == 4 ? " The four-member jury holds the decision on the "
		                + "boards until its votes are in." : ""), List.of());

		for (int j = 1; j < n; j++) {
			juror(j, liftJury[j - 1]);
		}
		pause(1000);
		step(capitalize(allButLast) + " of " + all + " jurors have voted on the lift",
		        "Jury Control: Deliberation / Challenge offered; " + (n - 1) + " grey \"voted\" jury circles, values "
		                + "hidden. Boards: the referees' " + liftDecision + ", " + (n - 1) + " grey jury circles. "
		                + "Speaker: no dialog.",
		        List.of(new Check("jury", "Deliberation and Challenge still offered", buttonVisible("Jury Deliberation")),
		                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		juror(n, liftJury[n - 1]);
		pause(1500);
		Page juryPage = this.pages.get("jury");
		String reviewName;
		if (sc.challenge()) {
			step(last + " juror: split jury, the referees' decision stands",
			        "Jury " + describe(liftJury) + ": not unanimous, the referees' " + liftDecision + " stands. The "
			                + all + " jury votes are revealed. Jury Control: Deliberation / Challenge offered, Summon "
			                + "Referees (all) not orange. Speaker: no dialog.",
			        List.of(new Check("jury", "Deliberation and Challenge still offered",
			                buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")),
			                new Check("jury", "Summon Referees (all) is not orange",
			                        "!(" + buttonColor("Summon Referees", ORANGE) + ")"),
			                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

			// the decision display ends and lifting resumes before the coach's challenge
			pause(4000);
			click(juryPage, "vaadin-button:has-text('Challenge')", "Jury Chair clicks Challenge on Jury Control");
			reviewName = "Challenge";
		} else {
			step(last + " juror: the jury is unanimous against the referees",
			        "Jury " + describe(liftJury) + " against the referees' " + liftDecision + ". There is no "
			                + "automatic trigger" + (sc.setup().vpt ? "" : " (automaticBreakNoVPT off)") + ": the "
			                + "competition goes on until the Chair acts. The " + all + " jury votes are revealed. Jury Control: Summon Referees (all) orange (prompt), Deliberation / "
			                + "Challenge offered. Speaker: no dialog.",
			        List.of(new Check("jury", "Summon Referees (all) is orange", buttonColor("Summon Referees", ORANGE)),
			                new Check("jury", "Deliberation still offered", buttonVisible("Jury Deliberation")),
			                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

			click(juryPage, "vaadin-button:has-text('Jury Deliberation')",
			        "Jury Chair clicks Jury Deliberation on Jury Control");
			reviewName = "Deliberation";
		}
		pause(1500);
		step(reviewName + ": the review starts",
		        "Jury break started by the Chair: the referees' decision stays shown (they do not vote again), "
		                + "the jury lights are cleared and the jury vote is open at once (called verbally). Jury Control: No Lift / Good Lift / Call for Votes (not orange, the vote is "
		                + "open) / Resume; Summon Referees (all) enabled: it summons the referees. Speaker: "
		                + "\"Waiting for decision\" dialog. Boards: " + reviewName + " message, the referees' "
		                + liftDecision + ", empty jury cells.",
		        List.of(new Check("jury", "No Lift and Good Lift replace Deliberation and Challenge",
		                buttonVisible("No Lift") + " && " + buttonVisible("Good Lift") + " && !"
		                        + buttonVisible("Jury Deliberation")),
		                new Check("jury", "Call for Votes is not orange",
		                        "!(" + buttonColor("Call for Votes", ORANGE) + ")"),
		                new Check("jury", "Summon Referees (all) is enabled",
		                        buttonVisible("Summon Referees") + " && !(" + buttonDisabled("Summon Referees") + ")"),
		                new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for"))));

		// the Chair calls the vote verbally; the jurors vote a moment later
		pause(1000);
		String[] reviewJury = sc.reviewJury();
		for (int j = 1; j < n; j++) {
			juror(j, reviewJury[j - 1]);
		}
		pause(1500);
		step("Verbal vote: " + allButLast + " of " + all + " jurors have punched in",
		        "Grey \"voted\" jury cells, values hidden. The referees do not vote.", List.of());

		juror(n, reviewJury[n - 1]);
		pause(1500);
		String unanimity = Arrays.stream(reviewJury).distinct().count() == 1 ? "unanimous"
		        : "not unanimous, the referees' " + liftDecision + " stands";
		finishReview(last + " juror", "the " + all + " jury votes (" + describe(reviewJury) + ", " + unanimity + ")",
		        outcomeWord, reviewResult);
	}

	/**
	 * Jury box only (no Jury Control clicks): the jury votes, the Jury Chair never confirms the result nor gives the
	 * rule; the Speaker enters the decision in the "Waiting for decision" dialog, without a rule. Seven-person panel:
	 * automatic review, the jury box Summon All calls the vote. Jury vote: the jury box starts the deliberation and the
	 * vote is open at once.
	 */
	private void runSpeakerDecidesReview(String athlete) throws Exception {
		Scenario sc = this.scenario;
		boolean panel = sc.setup().panel;
		int n = sc.setup().jurors;
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String reviewResult = sc.goodResult() ? "Good Lift" : "No Lift";
		String outcomeWord = liftDecision.equals(reviewResult) ? "confirmed" : "reversal";
		String refsText = describe(refs);
		String fop = this.options.fop();
		Page speaker = this.pages.get("speaker");

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ". The jury uses only "
		        + "the jury box; Jury Control is open but nobody touches it.", startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; " + n + " empty jury "
		        + "circles.", List.of());

		for (int j = 1; j <= n; j++) {
			juror(j, sc.liftJury()[j - 1]);
		}
		pause(1500);
		if (panel) {
			step("The " + COUNT[n] + " jurors vote " + describe(sc.liftJury()) + ": automatic Adjudication Panel "
			        + "Review", "The seven-person majority is against the referees' " + liftDecision + ". Speaker: "
			                + "\"Waiting for decision\" dialog with Good Lift / No Lift / Resume.",
			        List.of(new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for"))));
			publish("owlcms/jurybox/summon/" + fop, "all");
			pause(1500);
			step("Jury box Summon All: the vote is called", "All cells cleared, referees buzzed.", List.of());
			for (int r = 0; r < 3; r++) {
				referee(r + 1, sc.reviewRefs()[r]);
			}
		} else {
			step("The " + COUNT[n] + " jurors vote " + describe(sc.liftJury()) + " against the referees",
			        "No automatic trigger: the competition would go on until the jury acts. Speaker: no dialog.",
			        List.of(new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));
			publish("owlcms/jurybox/break/" + fop, "deliberation");
			pause(1500);
			step("Jury box: Deliberation",
			        "Jury break started from the jury box: the referees' decision stays shown, the jury lights are "
			                + "cleared and the jury vote is open at once (called verbally). Speaker: \"Waiting for "
			                + "decision\" dialog.",
			        List.of(new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for"))));
			// the Chair asks for the vote verbally; the jurors vote a moment later
			pause(1000);
		}
		for (int j = 1; j <= n; j++) {
			juror(j, sc.reviewJury()[j - 1]);
		}
		pause(1500);
		String allVotes = panel
		        ? "the seven votes (referees " + describe(sc.reviewRefs()) + ", jury " + describe(sc.reviewJury()) + ")"
		        : "the " + COUNT[n] + " jury votes (" + describe(sc.reviewJury()) + ")";
		step("The vote is complete: " + reviewResult + " result",
		        "Jury Control shows " + allVotes + " and waits for the Jury Chair (rule dialog). The boards reveal "
		                + "the votes. Speaker still waiting.",
		        List.of(new Check("jury", "Jury Control waits for the Chair's rule", dialogContains("Bending")),
		                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));

		// the Jury Chair never confirms nor gives the rule
		pause(2000);
		click(speaker, "vaadin-button:has-text('" + reviewResult + "')",
		        "Speaker clicks " + reviewResult + " in the \"Waiting for decision\" dialog");
		pause(1500);
		step("The Speaker enters " + reviewResult,
		        "Decision applied at once without a rule: " + reviewResult + " (" + outcomeWord + ") on the boards. "
		                + "The Speaker's dialog closes (the Speaker entered it and announces it). Jury Control: the "
		                + "rule dialog goes away.",
		        List.of(new Check("speaker", "the Speaker's dialog is closed",
		                "!(" + dialogContains("Waiting for") + ") && !(" + dialogContains(reviewResult.toUpperCase())
		                        + ")"),
		                new Check("jury", "the rule dialog is gone", "!(" + dialogContains("Bending") + ")")));

		pause(4000);
		// the Speaker has announced the decision: lifting goes on, the jury does not need to resume
		step("Lifting resumes", "Lifting resumed by itself after 3.5 s: next athlete. Jury Control: Deliberation "
		        + "and Challenge are back. Nobody presses Resume.",
		        List.of(new Check("jury", "Deliberation and Challenge are back", buttonVisible("Jury Deliberation"))));
	}

	/**
	 * Seven-person panel with a jury box that never sends a vote: its votes are simply not sent. The jury vote on the
	 * lift is incomplete, so there is no automatic review; the Chair starts a deliberation and calls the vote, which
	 * never completes, so no result screen opens; after a second the Chair gives the decision with the No Lift button,
	 * then the rule. The decision is marked "Jury Chair Entered Decision" (it is not the result of a vote).
	 */
	private void runBrokenJurorReview(String athlete) throws Exception {
		Scenario sc = this.scenario;
		int broken = sc.brokenJuror();
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String reviewResult = sc.goodResult() ? "Good Lift" : "No Lift";
		String outcomeWord = liftDecision.equals(reviewResult) ? "confirmed" : "reversal";
		String refsText = describe(refs);
		Page juryPage = this.pages.get("jury");

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ". Jury box " + broken
		        + " does not work: its votes are never sent.", startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; 4 empty jury circles; the "
		        + "decision is held on the boards until the jury has voted.", List.of());

		for (int j = 1; j <= 4; j++) {
			if (j != broken) {
				juror(j, sc.liftJury()[j - 1]);
			}
		}
		pause(1500);
		step("Three of four jurors vote on the lift; jury box " + broken + " sends nothing",
		        "The jury vote on the lift is incomplete, so there is no automatic Adjudication Panel Review. Jury "
		                + "Control: Deliberation / Challenge offered, 3 grey \"voted\" circles. Speaker: no dialog.",
		        List.of(new Check("jury", "Deliberation and Challenge offered",
		                buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")),
		                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		click(juryPage, "vaadin-button:has-text('Jury Deliberation')",
		        "Jury Chair clicks Jury Deliberation on Jury Control");
		pause(1500);
		step("Deliberation: the review starts",
		        "Same procedure as the automatic review: the votes on the lift stay displayed until the vote is "
		                + "called. Jury Control: No Lift / Good Lift / Call for Votes (orange) / Resume; Summon "
		                + "Referees (all) disabled. Speaker: \"Waiting for decision\" dialog.",
		        List.of(new Check("jury", "Call for Votes is orange", buttonColor("Call for Votes", ORANGE)),
		                new Check("jury", "No Lift and Good Lift replace Deliberation and Challenge",
		                        buttonVisible("No Lift") + " && " + buttonVisible("Good Lift") + " && !"
		                                + buttonVisible("Jury Deliberation")),
		                new Check("jury", "Summon Referees (all) is disabled", buttonDisabled("Summon Referees")),
		                new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for"))));

		click(juryPage, "vaadin-button:has-text('Call for Votes')", "Jury Chair clicks Call for Votes on Jury Control");
		pause(1500);
		step("Call for Votes", "All cells cleared, referees buzzed. Call for Votes no longer orange.",
		        List.of(new Check("jury", "Call for Votes no longer orange",
		                "!(" + buttonColor("Call for Votes", ORANGE) + ")")));

		for (int r = 0; r < 3; r++) {
			referee(r + 1, sc.reviewRefs()[r]);
		}
		for (int j = 1; j <= 4; j++) {
			if (j != broken) {
				juror(j, sc.reviewJury()[j - 1]);
			}
		}
		// we know jury box 1 will not vote: wait one second
		pause(1000);
		step("Six of seven have voted; jury box " + broken + " never does",
		        "The vote never completes: the votes stay hidden (grey cells), no result screen and no rule dialog "
		                + "open on Jury Control. Speaker still waiting.",
		        List.of(new Check("jury", "no rule dialog", "!(" + dialogContains("Bending") + ")"),
		                new Check("jury", "no result screen", "!(" + dialogContains("Cancel vote result") + ")"),
		                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));

		click(juryPage, "vaadin-button:has-text('No Lift')", "Jury Chair clicks No Lift on Jury Control");
		pause(1500);
		step("The Chair gives No Lift with the button",
		        "Jury Control opens the rule dialog. Nothing is applied until the rule is chosen. Speaker still "
		                + "waiting.",
		        List.of(new Check("jury", "the rule dialog is open", dialogContains("Bending")),
		                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));

		click(juryPage, "vaadin-button[aria-label^='1 - ']", "Jury Chair picks rule 1 on Jury Control");
		pause(1500);
		step("The Jury Chair gives the rule",
		        "Decision applied at once: No Lift (" + outcomeWord + ") with rule 1 on the boards. Speaker dialog: "
		                + "No Lift with the rule, marked Jury Chair Entered Decision (not the result of a vote), "
		                + "Close button.",
		        List.of(new Check("speaker", "the Speaker sees the decision with a Close button",
		                dialogContains("NO LIFT") + " && " + buttonVisible("Close")),
		                new Check("speaker", "marked as entered by the Jury Chair", dialogContains("Jury Chair Entered"))));

		closeAndResume(reviewResult);
	}

	/**
	 * After the last review vote: the Chair confirms (Good Lift) or gives the rule (No Lift), the Speaker closes the
	 * dialog, lifting resumes, the Chair presses Resume.
	 */
	private void finishReview(String lastVote, String allVotes, String outcomeWord, String reviewResult)
	        throws Exception {
		boolean good = this.scenario.goodResult();
		Page juryPage = this.pages.get("jury");
		Page speaker = this.pages.get("speaker");
		if (good) {
			step(lastVote + ": Good Lift result",
			        "Jury Control shows " + allVotes + " and the result screen: Confirm Good Lift / Cancel vote "
			                + "result. Attempt board and scoreboard reveal the votes at once. Speaker still "
			                + "waiting. Nothing applied yet.",
			        List.of(new Check("jury", "the result screen offers Confirm Good Lift",
			                dialogButton("Confirm Good Lift")),
			                new Check("jury", "and Cancel vote result", dialogButton("Cancel vote result")),
			                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));

			click(juryPage, "vaadin-button:has-text('Confirm Good Lift')",
			        "Jury Chair clicks Confirm Good Lift on Jury Control");
			pause(1500);
			step("The Jury Chair confirms the Good Lift",
			        "Decision applied at once: Good Lift (" + outcomeWord + ") pops over the votes on the "
			                + "boards. Speaker dialog: Good Lift (" + outcomeWord + "), \"announce\" text, Close "
			                + "button. Not marked Jury Chair "
			                + "Entered Decision.",
			        List.of(new Check("speaker", "the Speaker sees the decision with a Close button",
			                dialogContains("GOOD LIFT") + " && " + buttonVisible("Close")),
			                new Check("speaker", "not marked as entered by the Jury Chair",
			                        "!(" + dialogContains("Jury Chair Entered") + ")")));
		} else {
			step(lastVote + ": No Lift result",
			        "Jury Control shows " + allVotes + " and the rule dialog, with Cancel vote result. Attempt "
			                + "board and scoreboard reveal the votes at once. Speaker still waiting.",
			        List.of(new Check("jury", "the rule dialog is open", dialogContains("Bending")),
			                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));

			click(juryPage, "vaadin-button[aria-label^='1 - ']", "Jury Chair picks rule 1 on Jury Control");
			pause(1500);
			step("The Jury Chair gives the rule",
			        "Decision applied at once: No Lift with rule 1 pops over the votes on the boards. Speaker "
			                + "dialog: No Lift (" + outcomeWord + ") with the rule, \"announce\" text, Close button. "
			                + "Not marked "
			                + "Jury Chair Entered Decision.",
			        List.of(new Check("speaker", "the Speaker sees the decision with a Close button",
			                dialogContains("NO LIFT") + " && " + buttonVisible("Close")),
			                new Check("speaker", "not marked as entered by the Jury Chair",
			                        "!(" + dialogContains("Jury Chair Entered") + ")")));
		}

		closeAndResume(reviewResult);
	}

	/** the Speaker closes the decision dialog, lifting resumes, the Chair presses Resume */
	private void closeAndResume(String reviewResult) throws Exception {
		Page juryPage = this.pages.get("jury");
		Page speaker = this.pages.get("speaker");
		click(speaker, "vaadin-button:has-text('Close')", "Speaker clicks Close");
		pause(5000);
		step("Speaker closes the dialog; lifting resumes",
		        "Lifting resumed by itself after 3.5 s: next athlete. Jury Control: Deliberation and Challenge "
		                + "are back, the votes are still shown.",
		        List.of(new Check("speaker", "the dialog is closed",
		                "!(" + dialogContains(reviewResult.toUpperCase()) + ")"),
		                new Check("jury", "Deliberation and Challenge are back", buttonVisible("Jury Deliberation"))));

		click(juryPage, "vaadin-button:has-text('Resume Competition')",
		        "Jury Chair clicks Resume Competition on Jury Control");
		pause(1500);
		step("Jury Chair: Resume", "The jury votes are cleared on Jury Control and on the jury box.", List.of());
	}

	private static String capitalize(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private List<Check> startingChecks() {
		List<Check> checks = new ArrayList<>(this.setupChecks);
		checks.add(new Check("jury", "Deliberation and Challenge are offered",
		        buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")));
		return checks;
	}

	private static String describe(String[] refs) {
		long whites = Arrays.stream(refs).filter("good"::equals).count();
		return whites + " white, " + (refs.length - whites) + " red";
	}

	// ------------------------------------------------------------------ settings through the screens

	/**
	 * Apply the jury setup through the settings screens, then reload the pages (the Jury Control layout depends on the
	 * jury). The jury size is always selected on Jury Control.
	 */
	private void configure(Page page, Setup setup) {
		// nothing is assumed about the database: every setting the scenario depends on is set, then read back
		List<Check> checks = new ArrayList<>();
		Map<String, Boolean> switches = new LinkedHashMap<>();
		switches.put("automaticBreakNoVPT", false);
		switches.put("juryDecisionsOnAttemptBoard", true);
		switches.put("juryDecisionsOnScoreboard", true);
		String configPage = this.options.base() + "/preparation/config/3";
		page.navigate(configPage);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		switches.forEach((id, on) -> setChecked(switchBox(page, id), on));
		clickUpdate(page);
		page.navigate(configPage);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		switches.forEach((id, on) -> checks.add(readBack("Features: " + id + (on ? " on" : " off"),
		        isChecked(switchBox(page, id)) == on)));

		String competitionPage = this.options.base() + "/preparation/competition/1";
		page.navigate(competitionPage);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		setChecked(vptBox(page), setup.vpt);
		clickUpdate(page);
		page.navigate(competitionPage);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		checks.add(readBack("Competition Rules: Video Playback Technology " + (setup.vpt ? "in use" : "not in use"),
		        isChecked(vptBox(page)) == setup.vpt));
		action("Features: automaticBreakNoVPT off, juryDecisionsOnAttemptBoard on, juryDecisionsOnScoreboard on");
		action("Competition Rules: Video Playback Technology " + (setup.vpt ? "in use" : "not in use"));

		Page jury = this.pages.get("jury");
		jury.reload();
		jury.waitForLoadState(LoadState.NETWORKIDLE);
		setJurySize(jury, setup.jurors);
		String shown = jurySizeShown(jury);
		checks.add(readBack(setup.jurors + " jurors selected on Jury Control (shows \"" + shown + "\")",
		        shown.equals(String.valueOf(setup.jurors))));
		this.setupChecks = List.copyOf(checks);
		// the boards read the number of jurors when they load
		for (Page p : this.pages.values()) {
			if (p != jury) {
				p.reload();
				p.waitForLoadState(LoadState.NETWORKIDLE);
			}
		}
		startLiftingIfNeeded(this.pages.get("speaker"));
	}

	/** the Jury Control toolbar selection of the number of jurors (shown without the seven-person panel) */
	private void setJurySize(Page jury, int jurors) {
		// the only select on Jury Control
		Locator select = jury.locator("vaadin-select").first();
		select.click();
		Locator item = jury.getByRole(AriaRole.OPTION,
		        new Page.GetByRoleOptions().setName(String.valueOf(jurors)).setExact(true)).first();
		try {
			item.click(new Locator.ClickOptions().setTimeout(5_000));
		} catch (PlaywrightException e) {
			System.out.println("  jury size options: " + jury.evaluate(
			        "() => [...document.querySelectorAll('vaadin-select-item')].map(i => JSON.stringify(i.innerText)"
			                + " + ' ' + i.getAttribute('role')).join(', ')"));
			throw e;
		}
		jury.waitForTimeout(1500);
		action("Jury Control: " + jurors + " jurors selected");
	}

	private static Locator switchBox(Page page, String id) {
		return page.locator("span").filter(new Locator.FilterOptions().setHasText(id))
		        .locator("xpath=..").locator("vaadin-checkbox").first();
	}

	private static Locator vptBox(Page page) {
		return page.locator("vaadin-form-item").filter(new Locator.FilterOptions()
		        .setHasText("Video Playback Technology")).locator("vaadin-checkbox").first();
	}

	/** the value shown by the jury size selection of Jury Control (the selected item is copied into the select) */
	private static String jurySizeShown(Page jury) {
		Object shown = jury.locator("vaadin-select").first().evaluate("""
		        s => {
		          const vb = s.querySelector('vaadin-select-value-button') || s.shadowRoot?.querySelector('[part~="value"]');
		          return ((vb && (vb.innerText || vb.textContent)) || '').trim();
		        }""");
		return shown == null ? "" : shown.toString();
	}

	/** a setting read back from its page: the check passes or fails as read */
	private static Check readBack(String description, boolean ok) {
		return new Check("jury", description + " (read back from the settings page)", ok ? "true" : "false");
	}

	private static boolean isChecked(Locator checkbox) {
		checkbox.waitFor();
		return checkbox.getAttribute("checked") != null;
	}

	private static void setChecked(Locator checkbox, boolean on) {
		if (isChecked(checkbox) != on) {
			checkbox.click();
		}
	}

	private static void clickUpdate(Page page) {
		page.locator("vaadin-button:has-text('Update')").first().click();
		page.waitForTimeout(1500);
	}

	// ------------------------------------------------------------------ devices (MQTT)

	private MqttClient connectMqtt() throws Exception {
		MqttClient client = new MqttClient(this.options.mqttUri(), "owlcms-jury-scenario-" + System.currentTimeMillis(),
		        new MemoryPersistence());
		client.setCallback(new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				synchronized (JuryScenario.this.deviceMessages) {
					JuryScenario.this.deviceMessages
					        .add(topic + " " + new String(message.getPayload(), StandardCharsets.UTF_8).trim());
				}
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
		});
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(true);
		client.connect(connectOptions);
		// what owlcms sends to the devices (referee boxes, jury box)
		client.subscribe("owlcms/fop/#", 0);
		return client;
	}

	private void publish(String topic, String payload) throws Exception {
		MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
		message.setQos(0);
		this.mqtt.publish(topic, message);
		action("MQTT " + topic + " " + payload);
		pause(250);
	}

	/** the clock is set to 1:00, runs for 2 seconds and is stopped: the athlete lifts */
	private void athleteLifts() throws Exception {
		clock("60");
		pause(1000);
		clock("start");
		pause(2000);
		clock("stop");
		pause(500);
		step("Clock set to 1:00, started, stopped after 2 seconds",
		        "The athlete lifts: the clock was set to 1:00, ran for 2 s and is stopped at 0:58. No decision yet.",
		        List.of());
	}

	private void clock(String command) throws Exception {
		publish("owlcms/clock/" + this.options.fop(), command);
	}

	private void referee(int ref, String decision) throws Exception {
		publish("owlcms/refbox/decision/" + this.options.fop(), ref + " " + decision);
	}

	private void juror(int juror, String decision) throws Exception {
		publish("owlcms/jurybox/juryMember/decision/" + this.options.fop(), juror + " " + decision);
	}

	// ------------------------------------------------------------------ pages

	private static Page newPage(Browser browser, int width, int height) {
		BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(width, height));
		context.setDefaultTimeout(15_000);
		return context.newPage();
	}

	private Page open(Browser browser, String path, int width, int height) {
		Page page = newPage(browser, width, height);
		page.navigate(this.options.base() + "/" + path);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		return page;
	}

	/** after loading a group the platform may be in a break: the Speaker starts lifting */
	private void startLiftingIfNeeded(Page speaker) {
		speaker.waitForTimeout(2000);
		Locator start = speaker.locator("vaadin-button:has-text('Start Lifting'):visible");
		if (start.count() > 0 && start.first().isEnabled()) {
			start.first().click();
			action("Speaker clicks Start Lifting");
			speaker.waitForTimeout(2000);
		}
	}

	private static String currentAthlete(Page speaker) {
		try {
			Locator name = speaker.locator(UpdateCheck.ATHLETE_NAME_SELECTOR).first();
			name.waitFor(new Locator.WaitForOptions().setTimeout(10_000));
			return name.innerText().trim();
		} catch (PlaywrightException e) {
			return null;
		}
	}

	/** light-DOM state of a page: visible buttons, open dialogs, notifications */
	private static final String OBSERVE = """
	        () => {
	          const visible = e => !!(e.offsetWidth || e.offsetHeight || e.getClientRects().length);
	          const buttons = [...document.querySelectorAll('vaadin-button')].filter(visible).map(b =>
	            (b.innerText || b.getAttribute('aria-label') || '').trim().replace(/\\s+/g, ' ')
	            + (b.hasAttribute('disabled') ? ' [disabled]' : '')
	            + (getComputedStyle(b).backgroundColor === 'rgb(249, 115, 22)' ? ' [orange]' : ''))
	            .filter(t => t.length > 0);
	          const dialogs = [...document.querySelectorAll('vaadin-dialog-overlay, vaadin-dialog')]
	            .filter(d => d.opened || d.hasAttribute('opened'))
	            .map(d => (d.innerText || '').trim().replace(/\\s+/g, ' ').slice(0, 400))
	            .filter(t => t.length > 0);
	          const notifications = [...document.querySelectorAll('vaadin-notification-card')]
	            .map(n => (n.innerText || '').trim().replace(/\\s+/g, ' '));
	          return 'buttons: ' + [...new Set(buttons)].join(' | ')
	            + '\\ndialogs: ' + [...new Set(dialogs)].join(' || ')
	            + '\\nnotifications: ' + notifications.join(' || ');
	        }
	        """;

	private static String buttonVisible(String text) {
		return "[...document.querySelectorAll('vaadin-button')].some(b => (b.offsetWidth || b.offsetHeight) && "
		        + "(b.innerText || '').includes(" + js(text) + "))";
	}

	private static String buttonDisabled(String text) {
		return "[...document.querySelectorAll('vaadin-button')].some(b => (b.offsetWidth || b.offsetHeight) && "
		        + "(b.innerText || '').includes(" + js(text) + ") && b.hasAttribute('disabled'))";
	}

	private static String buttonColor(String text, String color) {
		return "[...document.querySelectorAll('vaadin-button')].some(b => (b.offsetWidth || b.offsetHeight) && "
		        + "(b.innerText || '').includes(" + js(text) + ") && getComputedStyle(b).backgroundColor === "
		        + js(color) + ")";
	}

	private static String dialogButton(String text) {
		return "[...document.querySelectorAll('vaadin-dialog-overlay vaadin-button, vaadin-dialog vaadin-button,"
		        + " vaadin-button')].some(b => (b.offsetWidth || b.offsetHeight) && (b.innerText || '').includes("
		        + js(text) + "))";
	}

	private static String dialogContains(String text) {
		return "[...document.querySelectorAll('vaadin-dialog-overlay, vaadin-dialog')].some(d => "
		        + "(d.opened || d.hasAttribute('opened')) && (d.innerText || '').includes(" + js(text) + "))";
	}

	private static String js(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	// ------------------------------------------------------------------ steps and report

	/** a click that cannot be done is reported as a failure; the scenario goes on so that the report is complete */
	private void click(Page page, String selector, String description) {
		try {
			page.locator(selector + ":visible").first().click(new Locator.ClickOptions().setTimeout(5_000));
			action(description);
		} catch (PlaywrightException e) {
			this.failures++;
			this.scenarioFailures++;
			action("FAILED: " + description + " (not found)");
		}
	}

	private void action(String description) {
		this.pendingActions.add(description);
		System.out.println("  " + description);
	}

	private void step(String title, String expected, List<Check> checks) throws IOException {
		int number = this.results.size() + 1;
		System.out.println(number + ". " + title);
		List<String> checkResults = new ArrayList<>();
		for (Check check : checks) {
			boolean ok;
			try {
				this.pages.get(check.page()).waitForFunction("() => " + check.predicate(), null,
				        new Page.WaitForFunctionOptions().setTimeout(8_000));
				ok = true;
			} catch (PlaywrightException e) {
				ok = false;
				this.failures++;
			this.scenarioFailures++;
			}
			checkResults.add((ok ? "PASS " : "FAIL ") + check.page() + ": " + check.description());
			System.out.println("     " + (ok ? "pass " : "FAIL ") + check.page() + ": " + check.description());
		}
		Map<String, String> shots = new LinkedHashMap<>();
		Map<String, String> observed = new LinkedHashMap<>();
		for (Map.Entry<String, Page> entry : this.pages.entrySet()) {
			String file = String.format("%02d-%s.png", number, entry.getKey());
			entry.getValue().screenshot(new Page.ScreenshotOptions().setPath(this.reportDir.resolve(file)));
			shots.put(entry.getKey(), file);
			observed.put(entry.getKey(), String.valueOf(entry.getValue().evaluate(OBSERVE)));
		}
		List<String> actions = new ArrayList<>(this.pendingActions);
		synchronized (this.deviceMessages) {
			if (!this.deviceMessages.isEmpty()) {
				actions.add("owlcms to devices: " + String.join("; ", this.deviceMessages));
				this.deviceMessages.clear();
			}
		}
		this.pendingActions = new ArrayList<>();
		this.results.add(new StepResult(number, title, actions, expected, checkResults, shots, observed));
	}

	private void writeReport() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Jury scenario</title><style>")
		        .append("body{font-family:sans-serif;margin:1em}h2{margin-top:2em}.shots{display:grid;")
		        .append("grid-template-columns:repeat(4,1fr);gap:6px}.shots img{width:100%;border:1px solid #888}")
		        .append(".pass{color:green}.fail{color:#c00;font-weight:bold}pre{white-space:pre-wrap;")
		        .append("background:#f4f4f4;padding:4px;font-size:80%}</style></head><body>")
		        .append("<h1>").append(esc(this.scenario.title())).append("</h1>")
		        .append("<p><a href='timeline.html'>Timeline view (screens side by side)</a></p>")
		        .append("<p>").append(esc(this.options.base())).append(", platform ").append(esc(this.options.fop()))
		        .append(". ").append(this.scenarioFailures == 0 ? "All checks passed."
		                : this.scenarioFailures + " check(s) failed.")
		        .append("</p>");
		for (StepResult r : this.results) {
			sb.append("<h2>").append(r.number()).append(". ").append(esc(r.title())).append("</h2>");
			if (!r.actions().isEmpty()) {
				sb.append("<p><b>Actions</b></p><ul>");
				r.actions().forEach(a -> sb.append("<li>").append(esc(a)).append("</li>"));
				sb.append("</ul>");
			}
			sb.append("<p><b>Expected</b> ").append(esc(r.expected())).append("</p>");
			if (!r.checks().isEmpty()) {
				sb.append("<ul>");
				r.checks().forEach(c -> sb.append("<li class='").append(c.startsWith("PASS") ? "pass" : "fail")
				        .append("'>").append(esc(c)).append("</li>"));
				sb.append("</ul>");
			}
			sb.append("<div class='shots'>");
			r.shots().forEach((page, file) -> sb.append("<figure><figcaption>").append(esc(page))
			        .append("</figcaption><a href='").append(file).append("'><img src='").append(file)
			        .append("'></a><pre>").append(esc(r.observed().get(page))).append("</pre></figure>"));
			sb.append("</div>");
		}
		sb.append("</body></html>");
		Files.writeString(this.reportDir.resolve("index.html"), sb.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * Compact view: one row per step, one column per screen, so the screens can be followed side by side as the
	 * events unfold. Click a screenshot for full size; arrow keys move between steps and screens.
	 */
	private void writeTimeline() throws IOException {
		List<String> pageNames = new ArrayList<>(this.pages.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>").append(esc(this.scenario.title())).append("</title><style>")
		        .append("body{font-family:sans-serif;margin:0;background:#1e1e1e;color:#ddd}")
		        .append("table{border-collapse:collapse;width:100%}")
		        .append("th{position:sticky;top:0;background:#333;padding:6px;z-index:1}")
		        .append("td{border-top:1px solid #444;padding:4px;vertical-align:top}")
		        .append("td.step{width:16%;font-size:85%}td.step b{font-size:110%;color:#fff}")
		        .append("td img{width:100%;cursor:zoom-in;display:block}")
		        .append(".pass{color:#6c6}.fail{color:#f66;font-weight:bold}details{color:#aaa}")
		        .append("tr.current td{background:#2b3a4a}")
		        .append("#viewer{display:none;position:fixed;inset:0;background:rgba(0,0,0,.92);z-index:9;")
		        .append("text-align:center}#viewer img{max-width:98vw;max-height:90vh;margin-top:1vh}")
		        .append("#caption{color:#fff;padding:6px}</style></head><body><table><thead><tr><th>Step</th>");
		pageNames.forEach(n -> sb.append("<th>").append(esc(n)).append("</th>"));
		sb.append("</tr></thead><tbody>");
		for (StepResult r : this.results) {
			sb.append("<tr id='s").append(r.number()).append("'><td class='step'><b>").append(r.number())
			        .append(". ").append(esc(r.title())).append("</b><p>").append(esc(r.expected())).append("</p>");
			r.checks().forEach(c -> sb.append("<div class='").append(c.startsWith("PASS") ? "pass" : "fail")
			        .append("'>").append(esc(c)).append("</div>"));
			if (!r.actions().isEmpty()) {
				sb.append("<details><summary>actions</summary><ul>");
				r.actions().forEach(a -> sb.append("<li>").append(esc(a)).append("</li>"));
				sb.append("</ul></details>");
			}
			sb.append("</td>");
			for (String n : pageNames) {
				String file = r.shots().get(n);
				sb.append("<td>").append(file == null ? "" : "<img src='" + file + "' data-step='" + r.number()
				        + "' data-page='" + esc(n) + "' title='" + esc(r.observed().getOrDefault(n, "")) + "'>")
				        .append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</tbody></table><div id='viewer'><div id='caption'></div><img id='big'></div><script>")
		        .append("const imgs=[...document.querySelectorAll('td img')];const pages=")
		        .append(pageNames.size()).append(";let cur=-1;const v=document.getElementById('viewer');")
		        .append("function show(i){if(i<0||i>=imgs.length)return;cur=i;const im=imgs[i];")
		        .append("document.getElementById('big').src=im.src;document.getElementById('caption').textContent=")
		        .append("'Step '+im.dataset.step+' — '+im.dataset.page+'   (arrows: ←→ screen, ↑↓ step, Esc close)';")
		        .append("v.style.display='block';document.querySelectorAll('tr').forEach(t=>t.classList.remove('current'));")
		        .append("im.closest('tr').classList.add('current');im.closest('tr').scrollIntoView({block:'center'});}")
		        .append("imgs.forEach((im,i)=>im.onclick=()=>show(i));v.onclick=()=>v.style.display='none';")
		        .append("document.onkeydown=e=>{if(v.style.display!=='block')return;")
		        .append("if(e.key==='Escape')v.style.display='none';else if(e.key==='ArrowRight')show(cur+1);")
		        .append("else if(e.key==='ArrowLeft')show(cur-1);else if(e.key==='ArrowDown')show(cur+pages);")
		        .append("else if(e.key==='ArrowUp')show(cur-pages);else return;e.preventDefault();};")
		        .append("</script></body></html>");
		Files.writeString(this.reportDir.resolve("timeline.html"), sb.toString(), StandardCharsets.UTF_8);
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
