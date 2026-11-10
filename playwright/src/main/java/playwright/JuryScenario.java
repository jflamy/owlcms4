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
 * <li>four-member jury with VPT (010-060). The review starts automatically (the jury votes against the referees) or
 * with the Challenge button of Jury Control (030, 035: the jury agreed with the referees' No Lift; 040, 050: split
 * jury); Summon All from the jury box calls the vote; the seven officials vote.</li>
 * <li>jury vote, no seven-person panel: three-member jury without VPT (070-110), four-member jury without VPT
 * (120-160), a three-member jury with VPT behaves exactly as without VPT, so it has no scenarios of its own.
 * <li>170: four-member jury with VPT, a jury box that does not work: the vote never completes and the Chair gives the
 * decision with the No Lift button and the rule.
 * <li>180 (VPT) and 190 (3-member jury): jury box only, the Chair never confirms; the Speaker enters the decision. No automatic stop: the Jury Chair starts the review with Jury Deliberation
 * (unanimous jury against the referees) or Challenge (split jury). The vote is open at once and called verbally; the
 * three jurors punch in their votes; the referees do not vote; unanimity is needed to reverse.</li>
 * <li>200 (VPT) and 205 (four-member jury, no VPT): a jury box that does not work; the Speaker starts the next clock
 * anyway.</li>
 * <li>210-230: the Jury Chair decides with the jury box buttons (MQTT), applied at once without a rule; the Speaker
 * dialog asks to get the reason from the Technical Controller. 210: the decision agrees with the vote; 220: against
 * the vote; 230: no vote.</li>
 * <li>240-250: manual deliberation or automatic panel review ended with Resume without a jury decision.</li>
 * <li>260-290: normal three-member jury-box decisions after a complete review vote: confirm No Lift, confirm Good
 * Lift, reverse to No Lift, reverse to Good Lift. The Chair presses the physical box button; the Speaker only closes
 * the announcement dialog.</li>
 * <li>300-320: the jury agrees with the referees, no review. 300 (three-member jury) and 320 (VPT): the attempt
 * board stays on the athlete under review for 10 s after the last jury vote, then shows the next athlete. 310: the
 * Speaker starts the next clock during that time; no confirmation is asked and the board follows.</li>
 * <li>330-340: no jury. The attempt board moves to the next athlete after the 3.5 s decision display; the scoreboard
 * decision block stays on the athlete under review for 20 s (330) or until the next clock starts (340).</li>
 * <li>350-360: the review starts after the Speaker has started the next athlete's clock. 350: three-member jury,
 * deliberation. 360: VPT, challenge after the panel agreed with the referees' No Lift. The review stops the clock;
 * the attempt board and the decision block go back to the athlete under review; the Speaker stays on the next
 * athlete.</li>
 * <li>370-380: the lift ends the snatches (370, session M2) or the session (380, session M3). The attempt board
 * stays on the athlete under review until the jury has voted and 10 s more, then shows the break before clean and
 * jerk or the end of the session. The scoreboard decision block stays on the last lift 20 s into the break or the
 * end of the session, then clears.</li>
 * <li>390-400: same lifts (390, session M4; 400, session M5), but the jury never votes. The scoreboard decision
 * block clears all the same after 20 s. The attempt board stays on the athlete under review, without time limit,
 * until the Speaker reloads the session. 370 to 400 need the
 * single-athlete sessions of the fixture ({@code run-jury-scenario.sh --server}); each one uses up its session.</li>
 * </ul>
 * Completed decisions end after the Speaker closes the dialog and lifting resumes automatically. Only reviews
 * ending without a jury decision use Resume. The settings are applied through the screens before each scenario.
 * <p>
 * After the referees' decision, every scenario checks that the Speaker and the scoreboard top line move to the next
 * athlete while the attempt board and the scoreboard decision block stay on the athlete under review for the jury
 * votes. The attempt board shows the referee lights with the jury circles on the lift; in a jury-only review (no
 * seven-person panel) it shows the jury circles alone, centered, from the start of the review. With the seven-person
 * panel it keeps the votes on the lift until the vote is called, then shows seven cleared cells.
 * <p>
 * The scoreboard decision block shows the athlete under review from the referees' decision until the next clock
 * starts, then the athlete of the running clock. Without a jury it also returns to the clock after 20 s. After a
 * jury decision it keeps the decision when lifting resumes, for 20 s (180 and 190 wait for the return to the clock).
 * <p>
 * Each scenario has its own folder {@code <out>/<scenario>/}, cleaned and rewritten each time it runs, with
 * {@code timeline.html} (screens side by side, with header checkboxes to hide/show screen columns) and
 * {@code index.html} (details). After every run,
 * {@code <out>/index.html} lists the last run of every scenario.
 *
 * <pre>
 * Options: --base=http://localhost:8080 --mqtt=tcp://localhost:1883 --fop=A --group=M1 --out=playwright/reports
 *          --scenario=010-vpt-reversal-nolift,040-vpt-challenge-success --headed
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
		JURY4("four-member jury, no VPT (unanimity, manual trigger)", false, 4),
		NONE("no jury", false, 0);

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
	 * @param boxDecision    "good" or "bad": the Jury Chair presses Good Lift / No Lift on the jury box (MQTT
	 *                       {@code owlcms/jurybox/decision}), after the review vote, or before any vote when
	 *                       {@code reviewJury} is null. null: no jury box decision
	 * @param resumeWithoutDecision the Jury Chair ends the review with Resume and leaves the referees' decision in place
	 * @param summonForDiscussion summon the referees during a manual deliberation before the jury votes again
	 * @param noReview       the jury agrees with the referees and there is no review. "hold": the attempt board stays
	 *                       on the athlete under review for 10 s after the last jury vote; "clock": the Speaker starts
	 *                       the next clock during that time. null: the scenario has a review
	 *                       "lastSnatch" or "lastLift": the lift ends the snatches or the session (see session)
	 * @param afterNextClock the Speaker starts the next athlete's clock before the Jury Chair starts the review
	 * @param session        the session the scenario needs, prepared in the fixture of the dedicated server
	 *                       (run-jury-scenario.sh --server); null: the session of the run (--group)
	 */
	record Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
	        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror, boolean speakerDecides,
	        boolean startClockAnyway, String boxDecision, boolean resumeWithoutDecision, boolean summonForDiscussion,
	        String noReview, boolean afterNextClock, String session) {

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, 0, false, false,
			        null, false, false, null, false, null);
		}

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, brokenJuror, false,
			        false, null, false, false, null, false, null);
		}

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror, boolean speakerDecides) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, brokenJuror,
			        speakerDecides, false, null, false, false, null, false, null);
		}

		Scenario(String id, String title, Setup setup, String[] refs, String[] liftJury, boolean challenge,
		        String[] reviewRefs, String[] reviewJury, boolean goodResult, int brokenJuror, boolean speakerDecides,
		        boolean startClockAnyway) {
			this(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, brokenJuror,
			        speakerDecides, startClockAnyway, null, false, false, null, false, null);
		}

		/** the Jury Chair decides with the jury box buttons */
		static Scenario boxDecision(String id, String title, Setup setup, String[] refs, String[] liftJury,
		        String[] reviewRefs, String[] reviewJury, String boxDecision) {
			return boxDecision(id, title, setup, refs, liftJury, reviewRefs, reviewJury, boxDecision, false);
		}

		static Scenario boxDecision(String id, String title, Setup setup, String[] refs, String[] liftJury,
		        String[] reviewRefs, String[] reviewJury, String boxDecision, boolean summonForDiscussion) {
			return new Scenario(id, title, setup, refs, liftJury, false, reviewRefs, reviewJury,
			        "good".equals(boxDecision), 0, false, false, boxDecision, false, summonForDiscussion, null, false, null);
		}

		static Scenario resumeWithoutDecision(String id, String title, Setup setup, String[] refs, String[] liftJury) {
			return new Scenario(id, title, setup, refs, liftJury, false, null, null, false, 0, false, false, null,
			        true, false, null, false, null);
		}

		/** the jury agrees with the referees: no review */
		static Scenario noReview(String id, String title, Setup setup, String[] refs, String[] liftJury,
		        String noReview) {
			return new Scenario(id, title, setup, refs, liftJury, false, null, null,
			        Arrays.stream(refs).filter("good"::equals).count() >= 2, 0, false, false, null, false, false,
			        noReview, false, null);
		}

		/**
		 * A lift that ends the snatches ("lastSnatch") or the session ("lastLift"), in a single-athlete session
		 * prepared in the fixture. The jury agrees with the referees, or never votes (no jury votes given).
		 */
		static Scenario lastLift(String id, String title, Setup setup, String session, String kind, String[] refs,
		        String[] liftJury) {
			return new Scenario(id, title, setup, refs, liftJury, false, null, null,
			        Arrays.stream(refs).filter("good"::equals).count() >= 2, 0, false, false, null, false, false,
			        kind, false, session);
		}

		/** the Speaker starts the next athlete's clock before the Jury Chair starts the review */
		static Scenario afterNextClock(String id, String title, Setup setup, String[] refs, String[] liftJury,
		        boolean challenge, String[] reviewRefs, String[] reviewJury, boolean goodResult) {
			return new Scenario(id, title, setup, refs, liftJury, challenge, reviewRefs, reviewJury, goodResult, 0,
			        false, false, null, false, false, null, true, null);
		}
	}

	private static String[] votes(String... v) {
		return v;
	}

	static final Map<String, Scenario> SCENARIOS = new LinkedHashMap<>();
	static {
		add(new Scenario("010-vpt-reversal-nolift",
		        "010. VPT: Good Lift reversed to No Lift by the panel (automatic review), the Jury Chair gives the rule",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("good", "bad", "bad"), votes("good", "bad", "bad", "bad"), false));
		add(new Scenario("020-vpt-reversal-goodlift",
		        "020. VPT: No Lift reversed to Good Lift by the panel (automatic review), the Jury Chair confirms",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        votes("bad", "good", "good"), votes("good", "good", "good", "bad"), true));
		add(new Scenario("030-vpt-challenge-jury-agreed-nolift",
		        "030. VPT: unsuccessful challenge after a jury that agreed with the referees' No Lift, the review "
		                + "vote confirms the No Lift, the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("bad", "bad", "bad", "bad"), true,
		        votes("bad", "bad", "good"), votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("035-vpt-challenge-jury-agreed-goodlift",
		        "035. VPT: successful challenge after a jury that agreed with the referees' No Lift, the review vote "
		                + "reverses it to Good Lift, the Jury Chair confirms",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("bad", "bad", "bad", "bad"), true,
		        votes("good", "good", "good"), votes("good", "good", "bad", "bad"), true));
		add(new Scenario("040-vpt-challenge-success",
		        "040. VPT: successful challenge, split jury, No Lift reversed to Good Lift, the Jury Chair confirms",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        votes("good", "good", "good"), votes("good", "good", "bad", "good"), true));
		add(new Scenario("050-vpt-challenge-fail",
		        "050. VPT: unsuccessful challenge, split jury, No Lift confirmed, the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        votes("bad", "bad", "bad"), votes("good", "bad", "good", "good"), false));
		add(new Scenario("060-vpt-confirmation-nolift",
		        "060. VPT: No Lift confirmed by the panel (automatic review), the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "good"), false));
		add(new Scenario("070-jury3-reversal-nolift",
		        "070. 3-member jury, no VPT: unanimous jury against a Good Lift, the Chair starts a deliberation, "
		                + "reversed to No Lift, the Jury Chair gives the rule",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad"), false));
		add(new Scenario("080-jury3-reversal-goodlift",
		        "080. 3-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, "
		                + "reversed to Good Lift, the Jury Chair confirms",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"), false,
		        null, votes("good", "good", "good"), true));
		add(new Scenario("090-jury3-challenge-success",
		        "090. 3-member jury, no VPT: successful challenge, split jury, unanimous review vote, No Lift reversed "
		                + "to Good Lift, the Jury Chair confirms",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "bad", "bad"), true,
		        null, votes("good", "good", "good"), true));
		add(new Scenario("100-jury3-challenge-fail",
		        "100. 3-member jury, no VPT: unsuccessful challenge, review vote not unanimous, No Lift stands, the Jury "
		                + "Chair gives the rule",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "bad", "bad"), true,
		        null, votes("good", "good", "bad"), false));
		add(new Scenario("110-jury3-confirmation-nolift",
		        "110. 3-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, No Lift "
		                + "confirmed by the unanimous review vote, the Jury Chair gives the rule",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"), false,
		        null, votes("bad", "bad", "bad"), false));
		add(new Scenario("120-jury4-reversal-nolift",
		        "120. 4-member jury, no VPT: unanimous jury against a Good Lift, the Chair starts a deliberation, "
		                + "reversed to No Lift, the Jury Chair gives the rule",
		        Setup.JURY4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("130-jury4-reversal-goodlift",
		        "130. 4-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, "
		                + "reversed to Good Lift, the Jury Chair confirms",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        null, votes("good", "good", "good", "good"), true));
		add(new Scenario("140-jury4-challenge-success",
		        "140. 4-member jury, no VPT: successful challenge, split jury, unanimous review vote, No Lift reversed "
		                + "to Good Lift, the Jury Chair confirms",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        null, votes("good", "good", "good", "good"), true));
		add(new Scenario("150-jury4-challenge-fail",
		        "150. 4-member jury, no VPT: unsuccessful challenge, review vote not unanimous (3 to 1), No Lift "
		                + "stands, the Jury Chair gives the rule",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "bad", "bad"), true,
		        null, votes("good", "good", "good", "bad"), false));
		add(new Scenario("160-jury4-confirmation-nolift",
		        "160. 4-member jury, no VPT: unanimous jury against a No Lift, the Chair starts a deliberation, No Lift "
		                + "confirmed by the unanimous review vote, the Jury Chair gives the rule",
		        Setup.JURY4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"), false,
		        null, votes("bad", "bad", "bad", "bad"), false));
		add(new Scenario("170-vpt-broken-juror-nolift",
		        "170. VPT: jury box 1 does not work, the Chair starts a deliberation and calls the vote, which never "
		                + "completes; the Chair gives No Lift with the button, then the rule",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "bad"), false, 1));
		add(new Scenario("180-vpt-speaker-enters-nolift",
		        "180. VPT, jury box only: automatic review, the jury box calls the vote, No Lift result; the Jury "
		                + "Chair never confirms nor gives the rule, the Speaker enters No Lift",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "bad"), false, 0, true));
		add(new Scenario("190-jury3-speaker-enters-nolift",
		        "190. 3-member jury, no VPT, jury box only: the jury box starts the deliberation, unanimous No Lift "
		                + "vote; the Jury Chair never confirms nor gives the rule, the Speaker enters No Lift",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad"), false, 0, true));
		add(new Scenario("200-vpt-broken-juror-start-anyway",
		        "200. VPT: jury box 1 does not work; the Speaker starts the next clock anyway, abandoning the "
		                + "incomplete jury vote and leaving the referees' Good Lift in place",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        null, null, true, 1, false, true));
		add(new Scenario("205-jury4-broken-juror-start-anyway",
		        "205. 4-member jury, no VPT: jury box 1 does not work; the Speaker starts the next clock anyway, "
		                + "abandoning the incomplete jury vote and leaving the referees' Good Lift in place",
		        Setup.JURY4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"), false,
		        null, null, true, 1, false, true));
		add(Scenario.boxDecision("210-vpt-jurybox-nolift",
		        "210. VPT: automatic review, the vote confirms the No Lift result; the Jury Chair presses No Lift on "
		                + "the jury box: applied at once without a rule, the Speaker is told to get the reason",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad"),
		        votes("bad", "bad", "bad"), votes("bad", "bad", "bad", "bad"), "bad"));
		add(Scenario.boxDecision("220-vpt-jurybox-nolift-against-vote",
		        "220. VPT: automatic review, the vote gives a Good Lift result; the Jury Chair presses No Lift on the "
		                + "jury box: Jury Chair Decision Against the Vote, no rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("good", "good", "good", "good"),
		        votes("good", "good", "good"), votes("good", "good", "good", "bad"), "bad"));
		add(Scenario.boxDecision("230-jury3-jurybox-nolift-no-vote",
		        "230. 3-member jury, no VPT: the jury box starts a deliberation and the Jury Chair presses No Lift on "
		                + "the jury box before any vote: Jury Chair Entered Decision, no rule",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"),
		        null, null, "bad"));
		add(Scenario.resumeWithoutDecision("240-jury3-deliberation-resume",
		        "240. 3-member jury, no VPT: the jury starts a deliberation, takes no action and resumes competition; "
		                + "the referees' Good Lift stands",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad")));
		add(Scenario.resumeWithoutDecision("250-vpt-automatic-review-resume",
		        "250. VPT: automatic Adjudication Panel Review is ended abnormally with Resume before the vote; the "
		                + "referees' Good Lift stands",
		        Setup.VPT4, votes("good", "good", "bad"), votes("bad", "bad", "bad", "bad")));
		add(Scenario.boxDecision("260-jury3-jurybox-confirmation-nolift",
		        "260. 3-member jury, jury box only: the review vote confirms No Lift; the Jury Chair presses No Lift "
		                + "on the box; the TC gives the reason verbally; the Speaker announces and closes",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"),
		        null, votes("bad", "bad", "bad"), "bad"));
		add(Scenario.boxDecision("270-jury3-jurybox-confirmation-goodlift",
		        "270. 3-member jury, jury box only: the review vote confirms Good Lift; the Jury Chair presses Good "
		                + "Lift on the box; the Speaker only announces and closes",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"),
		        null, votes("good", "good", "good"), "good"));
		add(Scenario.boxDecision("280-jury3-jurybox-reversal-nolift",
		        "280. 3-member jury, jury box only: referees summoned for discussion, then a unanimous jury review "
		                + "vote reverses Good Lift to No Lift; the Chair presses No Lift on the box; the TC gives the "
		                + "reason verbally; the Speaker announces and closes",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"),
		        null, votes("bad", "bad", "bad"), "bad", true));
		add(Scenario.boxDecision("290-jury3-jurybox-reversal-goodlift",
		        "290. 3-member jury, jury box only: the unanimous review vote reverses No Lift to Good Lift; the "
		                + "Jury Chair presses Good Lift on the box; the Speaker only announces and closes",
		        Setup.JURY3, votes("bad", "bad", "good"), votes("good", "good", "good"),
		        null, votes("good", "good", "good"), "good"));
		add(Scenario.noReview("300-jury3-agreed-board-hold",
		        "300. 3-member jury, no VPT: the jury agrees with the referees' Good Lift, no review; the attempt "
		                + "board stays on the athlete under review for 10 s after the last jury vote, then shows the "
		                + "next athlete",
		        Setup.JURY3, votes("good", "good", "bad"), votes("good", "good", "good"), "hold"));
		add(Scenario.noReview("310-jury3-agreed-clock-start",
		        "310. 3-member jury, no VPT: the jury agrees with the referees' Good Lift, no review; the Speaker "
		                + "starts the next clock while the attempt board is still on the athlete under review: no "
		                + "confirmation, the board follows",
		        Setup.JURY3, votes("good", "good", "bad"), votes("good", "good", "good"), "clock"));
		add(Scenario.noReview("320-vpt-agreed-board-hold",
		        "320. VPT: the seven-person majority confirms the referees' Good Lift, no review; the attempt board "
		                + "stays on the athlete under review for 10 s after the last jury vote, then shows the next "
		                + "athlete",
		        Setup.VPT4, votes("good", "good", "bad"), votes("good", "good", "good", "good"), "hold"));
		add(Scenario.noReview("330-nojury-decision-block-timeout",
		        "330. No jury: the attempt board moves to the next athlete after the 3.5 s decision display; the "
		                + "scoreboard decision block stays on the athlete under review for 20 s, then returns to the "
		                + "clock",
		        Setup.NONE, votes("good", "good", "bad"), votes(), "hold"));
		add(Scenario.noReview("340-nojury-decision-block-clock-start",
		        "340. No jury: the Speaker starts the next clock while the scoreboard decision block is on the "
		                + "athlete under review; the block returns to the clock at once",
		        Setup.NONE, votes("good", "good", "bad"), votes(), "clock"));
		add(Scenario.afterNextClock("350-jury3-deliberation-after-next-clock",
		        "350. 3-member jury, no VPT: unanimous jury against a Good Lift, but the Speaker has already started "
		                + "the next athlete's clock; the Chair starts a deliberation, which stops the clock; reversed "
		                + "to No Lift, the Jury Chair gives the rule",
		        Setup.JURY3, votes("good", "good", "bad"), votes("bad", "bad", "bad"), false,
		        null, votes("bad", "bad", "bad"), false));
		add(Scenario.afterNextClock("360-vpt-challenge-after-next-clock",
		        "360. VPT: the panel agreed with the referees' No Lift and the Speaker has already started the next "
		                + "athlete's clock when the coach challenges; the Challenge stops the clock; the review vote "
		                + "confirms the No Lift, the Jury Chair gives the rule",
		        Setup.VPT4, votes("bad", "bad", "good"), votes("bad", "bad", "bad", "bad"), true,
		        votes("bad", "bad", "good"), votes("bad", "bad", "bad", "bad"), false));
		add(Scenario.lastLift("370-jury3-last-snatch-board-hold",
		        "370. 3-member jury, no VPT, single-athlete session M2: the last snatch starts the break before clean "
		                + "and jerk; the attempt board stays on the athlete under review until the jury has voted and "
		                + "10 s more, then shows the break",
		        Setup.JURY3, "M2", "lastSnatch", votes("good", "good", "bad"), votes("good", "good", "good")));
		add(Scenario.lastLift("380-jury3-last-lift-board-hold",
		        "380. 3-member jury, no VPT, single-athlete session M3: the last lift ends the session; the attempt "
		                + "board stays on the athlete under review until the jury has voted and 10 s more, then shows "
		                + "the end of the session",
		        Setup.JURY3, "M3", "lastLift", votes("good", "good", "bad"), votes("good", "good", "good")));
		add(Scenario.lastLift("390-jury3-last-snatch-silent-jury-reload",
		        "390. 3-member jury, no VPT, single-athlete session M4: the last snatch starts the break before clean "
		                + "and jerk and the jury never votes; the attempt board stays on the athlete under review "
		                + "until the Speaker reloads the session",
		        Setup.JURY3, "M4", "lastSnatch", votes("good", "good", "bad"), votes()));
		add(Scenario.lastLift("400-jury3-last-lift-silent-jury-reload",
		        "400. 3-member jury, no VPT, single-athlete session M5: the last lift ends the session and the jury "
		                + "never votes; the attempt board stays on the athlete under review until the Speaker "
		                + "reloads the session",
		        Setup.JURY3, "M5", "lastLift", votes("good", "good", "bad"), votes()));
	}

	private static void add(Scenario sc) {
		SCENARIOS.put(sc.id(), sc);
	}

	/** a check on one page: a JavaScript predicate waited for, with a description for the report */
	record Check(String page, String description, String predicate) {
	}

	enum DeviceLight {
		UNKNOWN, OFF, WHITE, RED, VOTED
	}

	record MqttInstruction(String topic, String payload) {
		String display() {
			return topic + (payload.isBlank() ? "" : " " + payload);
		}
	}

	record JuryBoxState(List<DeviceLight> referees, List<DeviceLight> jurors) {
	}

	record RefBoxState(List<Boolean> summoned) {
	}

	record JuryBoxExpectation(JuryBoxState state, String description) {
	}

	record StepResult(int number, String title, List<String> actions, String expected, List<String> checks,
	        Map<String, String> shots, Map<String, String> observed, JuryBoxState juryBox, RefBoxState refBox) {
	}

	private final Options options;
	private final Map<String, Page> pages = new LinkedHashMap<>();
	private List<StepResult> results = new ArrayList<>();
	private Scenario scenario;
	private final List<MqttInstruction> deviceMessages = new ArrayList<>();
	private final DeviceLight[] juryBoxRefereeLights = unknownLights(3);
	private final DeviceLight[] juryBoxJurorLights = unknownLights(5);
	private Path reportDir;
	private MqttClient mqtt;
	private List<String> pendingActions = new ArrayList<>();
	/** the settings read back after they were applied, shown in the Starting point step */
	private List<Check> setupChecks = List.of();
	private int failures;
	private int scenarioFailures;
	/** read at the starting point: the Speaker and scoreboard top lines, the last name on the attempt board */
	private String speakerStart = "";
	private String scoreboardStart = "";
	private String attemptStart = "";
	/** last name and attempt on the attempt board: tells apart two attempts of an athlete who follows himself */
	private String attemptStartLine = "";
	/** the session loaded on the Speaker page */
	private String currentSession;

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
			try {
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
						throw new IllegalArgumentException(
						        "unknown scenario " + id + "; known: " + SCENARIOS.keySet());
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
						selectSession(sc.session() != null ? sc.session() : this.options.group());
						configure(settings, sc.setup());
						String athlete = currentAthlete(this.pages.get("speaker"));
						if (athlete == null || athlete.isBlank()) {
							throw new IllegalStateException("no current athlete on platform " + this.options.fop()
							        + ": load a group and start lifting first");
						}
						runReview(athlete);
					} catch (Exception e) {
						this.failures++;
						this.scenarioFailures++;
						String failure = "FAIL scenario: " + e;
						System.err.println(failure);
						// the screens at the time of the failure
						Map<String, String> shots = new LinkedHashMap<>();
						Map<String, String> observed = new LinkedHashMap<>();
						try {
							capture(this.results.size() + 1, shots, observed);
						} catch (RuntimeException captureFailure) {
							System.err.println("cannot capture the screens: " + captureFailure);
						}
						synchronized (this.deviceMessages) {
							this.results.add(new StepResult(this.results.size() + 1, "Scenario aborted",
							        List.copyOf(this.pendingActions), "All required checks must complete.",
							        List.of(failure), shots, observed, juryBoxState(),
							        refBoxState(this.deviceMessages)));
						}
						throw e;
					} finally {
						writeReport();
						writeTimeline();
						writeStatus();
						System.out.println("timeline: " + this.reportDir.resolve("timeline.html").toAbsolutePath());
					}
				}
			} finally {
				triggerUnloadBeacons(settings);
			}
		} finally {
			writeSummary(out);
			this.pages.clear();
			this.mqtt.disconnect();
			this.mqtt.close();
		}
		System.out.println("summary: " + out.resolve("index.html").toAbsolutePath());
		System.out.println(this.failures == 0 ? "all checks passed" : this.failures + " check(s) failed");
		return this.failures;
	}

	/** The Speaker page loads the session given in its URL, which switches the platform to that session. */
	private void selectSession(String session) {
		if (this.currentSession == null) {
			// the Speaker page was opened on the session of the run
			this.currentSession = this.options.group();
		}
		if (session == null) {
			// no --group and no session of its own: the Speaker stays where it is, unless a scenario moved it
			if (this.currentSession != null) {
				throw new IllegalStateException("--group is needed to go back to the session of the run after a "
				        + "scenario that uses its own session");
			}
			return;
		}
		if (session.equals(this.currentSession)) {
			return;
		}
		Page speaker = this.pages.get("speaker");
		speaker.navigate(this.options.base() + "/lifting/announcer?fop=" + this.options.fop() + "&group="
		        + URLEncoder.encode(session, StandardCharsets.UTF_8));
		speaker.waitForLoadState(LoadState.NETWORKIDLE);
		this.currentSession = session;
		action("Speaker loads session " + session);
	}

	/** status.txt in the scenario folder: time of the run, number of failed checks, number of steps */
	private void writeStatus() throws IOException {
		String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
		Files.writeString(this.reportDir.resolve("status.txt"),
		        when + "\n" + this.scenarioFailures + "\n" + this.results.size() + "\n", StandardCharsets.UTF_8);
	}

	/** index.html: the last run of every scenario, from the status.txt of each scenario folder */
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
		Files.writeString(out.resolve("index.html"), sb.toString(), StandardCharsets.UTF_8);
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
		this.speakerStart = String.valueOf(this.pages.get("speaker").evaluate("() => " + SPEAKER_TOP_LINE));
		this.scoreboardStart = String.valueOf(this.pages.get("scoreboard").evaluate("() => " + SCOREBOARD_TOP_LINE));
		this.attemptStart = String.valueOf(this.pages.get("attempt").evaluate("() => " + ATTEMPT_LAST_NAME));
		this.attemptStartLine = String.valueOf(this.pages.get("attempt").evaluate("() => " + ATTEMPT_LINE));
		if (this.scenario.session() != null) {
			runLastLift(athlete);
		} else if (this.scenario.setup().jurors == 0) {
			runNoJury(athlete);
		} else if (this.scenario.noReview() != null) {
			runNoReview(athlete);
		} else if (this.scenario.resumeWithoutDecision()) {
			runResumeWithoutDecision(athlete);
		} else if (this.scenario.boxDecision() != null) {
			runJuryBoxDecision(athlete);
		} else if (this.scenario.startClockAnyway()) {
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

	private void runResumeWithoutDecision(String athlete) throws Exception {
		Scenario sc = this.scenario;
		boolean panel = sc.setup().panel;
		int jurors = sc.setup().jurors;
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String refsText = describe(refs);
		Page juryPage = this.pages.get("jury");

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ".",
		        startingChecks());

		athleteLifts();
		for (int r = 0; r < refs.length; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText,
		        liftDecision + " (" + refsText + ") on all boards; " + jurors + " empty jury circles. "
		                + juryVotingText(),
		        juryVotingChecks());

		for (int j = 0; j < jurors; j++) {
			juror(j + 1, sc.liftJury()[j]);
		}
		pause(1500);
		if (panel) {
			step("The four jurors vote 0 white, 4 red: automatic Adjudication Panel Review",
			        "The seven-person majority is against the referees' Good Lift. The automatic jury break starts, "
			                + "but no review vote is called. Speaker: \"Waiting for decision\".",
			        List.of(new Check("jury", "Call for Votes is orange", buttonColor("Call for Votes", ORANGE)),
			                new Check("speaker", "the Speaker waits for the decision",
			                        dialogContains("Waiting for")),
			                panelLiftVotesCheck()),
			        expectedJuryBox(refs, sc.liftJury(),
			                "the original referee and Phase A jury decisions remain displayed"));
		} else {
			step("The three jurors vote 0 white, 3 red against the referees",
			        "There is no automatic review. The Jury Chair can start a manual deliberation.",
			        List.of(new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")),
			        expectedJuryBox(refs, sc.liftJury(),
			                "the original referee and jury decisions remain displayed before Deliberation"));
			publish("owlcms/jurybox/break/" + this.options.fop(), "deliberation");
			pause(1500);
			step("Jury box: Deliberation",
			        "The manual jury break starts and the review vote opens, but no one votes. The original referee "
			                + "decision is shown at the right of the top line. Speaker: \"Waiting for decision\".",
			        List.of(new Check("jury", "the original referee decision is at the right of the top line",
			                originalRefereesShown(refs)),
			                new Check("speaker", "the Speaker waits for the decision",
			                        dialogContains("Waiting for")),
			                juryOnlyLayoutCheck()),
			        expectedJuryBox(refs, null,
			                "manual mode keeps the original referee lights on and clears the jury lights"));
		}

		click(juryPage, "vaadin-button:has-text('Resume Competition')",
		        "Jury Chair clicks Resume Competition on Jury Control");
		pause(1500);
		List<Check> checks = new ArrayList<>(List.of(
		        new Check("speaker", "the waiting dialog is closed", "!(" + dialogContains("Waiting for") + ")"),
		        new Check("jury", "Deliberation and Challenge are back", buttonVisible("Jury Deliberation")),
		        new Check("attempt", "no jury decision was entered",
		                "!document.body.innerText.includes('JURY REVERSAL') && "
		                        + "!document.body.innerText.includes('JURY CONFIRMATION')")));
		checks.addAll(originalRefereeChecks(!panel));
		step("Jury Chair: Resume without a decision",
		        "Resume ends the " + (panel ? "automatic review" : "manual deliberation")
		                + ". The jury takes no action, so the referees' " + liftDecision
		                + " stands without an explicit Good Lift or No Lift from the Jury Chair."
		                + (panel
		                        ? " Because the vote was never called, the original referee decision was not moved to "
		                                + "the top line."
		                        : " The reviewed athlete and original referee decision remain in the top line until "
		                                + "the next clock starts."),
		        checks, panel
		                ? expectedJuryBox(refs, sc.liftJury(),
		                        "Resume leaves the original referee and Phase A jury lights unchanged")
		                : expectedJuryBox(refs, null,
		                        "Resume leaves the original referee lights on and the jury lights off"));
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
		long lastReferee = System.nanoTime();
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; 4 empty jury circles. "
		        + juryVotingText(), juryVotingChecks());

		for (int j = 1; j <= 4; j++) {
			if (j != broken) {
				juror(j, sc.liftJury()[j - 1]);
			}
		}
		pause(1500);
		step("Three of four jurors vote on the lift; jury box " + broken + " sends nothing",
		        "The jury vote on the lift is incomplete" + (sc.setup().panel
		                ? ", so there is no automatic Adjudication Panel Review"
		                : "; without VPT there is no automatic review in any case")
		                + ". Jury Control shows 3 grey \"voted\" circles. Speaker: no dialog, already on the next "
		                + "athlete.",
		        List.of(new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		// the jury vote is not complete: the attempt board has no time limit and must still be waiting
		pauseUntil(lastReferee, 16_000);
		// an athlete who follows himself has 2:00: the next clock is set to 1:00 so that it can be checked
		clock("60");
		awaitClock(clockCheck("speaker", 60, false));
		clock("start");
		pause(1500);
		step("Speaker tries to start the next clock",
		        "owlcms refuses the clock start. The Speaker sees \"The jury has not finished voting\" with Start "
		                + "clock anyway. More than 16 s after the referees' decision, the attempt board and the "
		                + "decision block are still on the athlete under review: they wait for the missing jury "
		                + "vote without a time limit. Jury Control shows no "
		                + "warning: the jury does not start the clock.",
		        List.of(new Check("speaker", "pending-jury dialog is open",
		                dialogContains("jury has not finished voting")),
		                new Check("speaker", "Start clock anyway is offered", buttonVisible("Start clock anyway")),
		                new Check("jury", "no clock warning", "!(" + notificationContains("Clock not started") + ")"),
		                decisionBlockReviewCheck(colors(refs), juryStates(broken, "voted")),
		                attemptHeldCheck(),
		                attemptVotesCheck("the referee decision and who has voted in the jury are shown",
		                        colors(refs), juryStates(broken, "voted"))));

		click(speaker, "vaadin-button:has-text('Start clock anyway')", "Speaker clicks Start clock anyway");
		int overrideStartMs;
		try {
			overrideStartMs = clockMessageMillis(awaitClockMessage("start"));
			if (overrideStartMs != 60_000) {
				throw new IllegalStateException("override clock must start from 60000 ms: " + overrideStartMs);
			}
			awaitRunningClocks();
			List<Check> overrideChecks = new ArrayList<>(List.of(
			        new Check("speaker", "the pending-jury dialog is closed",
			                "!(" + dialogContains("jury has not finished voting") + ")"),
			        new Check("jury", "no clock warning", "!(" + notificationContains("Clock not started") + ")"),
			        new Check("jury", "Deliberation and Challenge are offered",
			                buttonVisible("Jury Deliberation") + " && " + buttonVisible("Challenge")),
			        new Check("attempt", "no jury decision was shown",
			                "!document.body.innerText.includes('JURY REVERSAL') && "
			                        + "!document.body.innerText.includes('JURY CONFIRMATION')"),
			        new Check("attempt", "the referee and jury decisions of the previous lift are gone",
			                "!document.querySelector('attempt-board-template')?.shadowRoot"
			                        + "?.querySelector('#decisions')?.offsetWidth")));
			for (String page : List.of("speaker", "attempt", "scoreboard")) {
				overrideChecks.add(runningClockCheck(page));
			}
			overrideChecks.add(decisionBlockClockCheck());
			step("Speaker: Start clock anyway",
			        "The incomplete jury vote is discarded and no jury decision is entered. The referees' " + liftDecision
			                + " stands. The next athlete's clock has genuinely counted down from the "
			                + "server's " + overrideStartMs + " ms starting time; the attempt board and the decision "
			                + "block leave the athlete under review; the jury lights are cleared and Jury "
			                + "Control is back to Deliberation / Challenge.",
			        overrideChecks);
		} finally {
			clock("stop");
		}
		clockStoppedStep(overrideStartMs, "override");
	}

	/**
	 * The Speaker was quick: the next athlete's clock is running when the Jury Chair starts the review. The jury has
	 * finished voting on the lift, so the clock starts without confirmation. The review stops the clock.
	 */
	private void startNextClockBeforeReview(String review) throws Exception {
		// an athlete who follows himself has 2:00: the next clock is set to 1:00 so that it can be checked
		clock("60");
		awaitClock(clockCheck("speaker", 60, false));
		clock("start");
		try {
			int startMs = clockMessageMillis(awaitClockMessage("start"));
			if (startMs != 60_000) {
				throw new IllegalStateException("clock must start from 60000 ms: " + startMs);
			}
			awaitRunningClocks();
			List<Check> checks = new ArrayList<>(List.of(
			        new Check("speaker", "no confirmation is asked: the jury has finished voting",
			                "!(" + dialogContains("jury has not finished voting") + ")"),
			        attemptOnNextCheck(), decisionBlockClockCheck(),
			        new Check("jury", review + " can still be started",
			                "Challenge".equals(review) ? buttonEnabled("Challenge")
			                        : buttonEnabled("Jury Deliberation"))));
			for (String page : List.of("speaker", "attempt", "scoreboard")) {
				checks.add(runningClockCheck(page));
			}
			step("The Speaker starts the next athlete's clock before the " + review,
			        "The clock runs from the server's " + startMs + " ms. The attempt board and the scoreboard "
			                + "decision block have left " + this.attemptStart + " and show the next athlete with the "
			                + "running clock. Jury Control: " + review + " can still be started for the previous lift.",
			        checks);
		} catch (Exception e) {
			clock("stop");
			throw e;
		}
	}

	private String reviewAfterNextClockText() {
		return this.scenario.afterNextClock()
		        ? " The review stops the next athlete's clock. The attempt board and the scoreboard decision block "
		                + "are back on " + this.attemptStart + ", the athlete under review; the Speaker's top line "
		                + "stays on the next athlete and the dialog names the athlete under review."
		        : "";
	}

	/** the review started while the next athlete's clock was running */
	private Check[] reviewAfterNextClockChecks() {
		if (!this.scenario.afterNextClock()) {
			return new Check[0];
		}
		return new Check[] {
		        topLinesMovedChecks().get(0),
		        new Check("speaker", "the next athlete's clock is stopped by the review, below 1:00",
		                """
		                (() => {
		                  const timers = [...document.querySelectorAll('passive-timer-element')];
		                  return timers.length === 1 && timers[0].running === false
		                    && Number.isFinite(timers[0].currentTime) && timers[0].currentTime > 0
		                    && timers[0].currentTime < 59;
		                })()
		                """),
		        new Check("speaker", "the dialog names the athlete under review", dialogContains(this.attemptStart)),
		        new Check("attempt", "back on " + this.attemptStart + ", the athlete under review, with the review "
		                + "message",
		                """
		                (() => {
		                  const board = document.querySelector('attempt-board-template')?.shadowRoot;
		                  const banner = board?.querySelector('.reviewWarning');
		                  return !!banner && banner.checkVisibility() && banner.textContent.trim().length > 0
		                    && (%s) === %s;
		                })()
		                """.formatted(ATTEMPT_LAST_NAME, js(this.attemptStart))),
		        decisionBlockLabelCheck("the review label") };
	}

	/** after the scenario stopped a clock it had started: the server's stop acknowledgement and the stopped clocks */
	private void clockStoppedStep(int startMs, String which) throws Exception {
		int stopMs = clockMessageMillis(awaitClockMessage("stop"));
		if (stopMs <= 0 || stopMs >= startMs - 1000) {
			throw new IllegalStateException(which + " clock did not count down before Stop: " + stopMs);
		}
		List<Check> stopChecks = stoppedClockChecks((int) Math.ceil(stopMs / 1000.0));
		for (Check check : stopChecks) {
			awaitClock(check);
		}
		pause(1000);
		step("Clock stopped",
		        "OWLCMS confirms the " + which + " clock stopped with " + stopMs + " ms remaining. All clocks "
		                + "show the server-confirmed time and stay stopped after a further second.",
		        stopChecks);
	}

	/**
	 * The lift ends the snatches or the session, in a single-athlete session. The platform starts the break before
	 * clean and jerk, or ends the session, after the 3.5 s decision display (6.5 s after the third referee). When the
	 * jury agrees with the referees, the attempt board stays on the athlete under review until the jury has voted and
	 * 10 s more, then shows the break or the end of the session. When the jury never votes, the attempt board stays
	 * on the athlete under review until the Speaker reloads the session. In both cases the scoreboard decision block
	 * stays on the last lift for 20 s after the break or the end of the session started, then clears.
	 */
	private void runLastLift(String athlete) throws Exception {
		Scenario sc = this.scenario;
		boolean lastSnatch = "lastSnatch".equals(sc.noReview());
		int n = sc.setup().jurors;
		String[] refs = sc.refs();
		String[] liftJury = sc.liftJury();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String refsText = describe(refs);
		String what = lastSnatch ? "last snatch of the session" : "last lift of the session";
		String after = lastSnatch ? "the break before clean and jerk" : "the end of the session";
		Check left = new Check("attempt", "shows " + after + ": no decision lights, no longer on " + this.attemptStart,
		        """
		        (() => {
		          const board = document.querySelector('attempt-board-template')?.shadowRoot;
		          return !!board && !board.querySelector('#decisions')?.offsetWidth && (%s) !== %s
		            && !board.querySelector('#athleteTimer')?.checkVisibility()
		            && Boolean(board.querySelector('#breakTimer')?.checkVisibility()) === %s;
		        })()
		        """.formatted(ATTEMPT_LAST_NAME, js(this.attemptStart), lastSnatch));

		List<Check> starting = new ArrayList<>(startingChecks());
		starting.add(new Check("attempt", "the next lift is the third attempt (" + this.attemptStartLine + ")",
		        "/3/.test(((" + ATTEMPT_LINE + ").split('|')[1]) || '')"));
		step("Starting point", "Session " + sc.session() + ", one athlete: " + athlete + ", about to take the "
		        + what + "; " + sc.setup().description + ".", starting);

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		long decided = System.nanoTime();
		Check blockWaiting = decisionBlockReviewCheck(colors(refs), juryStates(0, "empty"));
		Check blockCleared = decisionBlockClearedCheck();
		Check waiting = attemptVotesCheck("the referee decision and " + n + " empty jury circles are shown",
		        colors(refs), juryStates(0, "empty"));
		pause(4000);
		step("Referees: " + refsText + " on the " + what,
		        liftDecision + " (" + refsText + "). The jury is voting: the attempt board and the scoreboard decision "
		                + "block show " + this.attemptStartLine + " with the referee decision and " + n
		                + " empty jury circles.",
		        List.of(attemptHeldCheck(), waiting, blockWaiting,
		                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		if (liftJury.length == 0) {
			pauseUntil(decided, 16_000);
			step("Sixteen seconds after the referees, the jury has not voted",
			        "The platform went to " + after + " once the decision had been shown for 3.5 s. The attempt "
			                + "board and the scoreboard decision block are still on " + this.attemptStartLine
			                + " with the referee decision and " + n + " empty jury circles.",
			        List.of(attemptHeldCheck(), waiting, blockWaiting));

			pauseUntil(decided, 30_000);
			step("Thirty seconds after the referees, the jury has still not voted",
			        "The scoreboard decision block stayed 20 s into " + after + ", then cleared. There is no time "
			                + "limit on the attempt board while jury votes are missing: it is still on "
			                + this.attemptStartLine + ".",
			        List.of(attemptHeldCheck(), waiting, blockCleared));

			click(this.pages.get("speaker"), "vaadin-button:has-text('Reload Session')",
			        "Speaker clicks Reload Session");
			pause(2000);
			step("The Speaker reloads the session",
			        "The platform is in a break: the session is reloaded without confirmation. The attempt board "
			                + "no longer waits for the jury and shows " + after + ".",
			        List.of(left, blockCleared, new Check("speaker", "no confirmation dialog",
			                "!(" + dialogContains("attempt is in progress") + ")")));
			return;
		}

		for (int j = 1; j <= n; j++) {
			juror(j, liftJury[j - 1]);
		}
		long lastVote = System.nanoTime();
		Check votesShown = attemptVotesCheck("the referee decision and the " + n + " revealed jury votes are shown",
		        colors(refs), colors(liftJury));
		Check blockVotes = decisionBlockReviewCheck(colors(refs), colors(liftJury));
		pauseUntil(decided, 9_000);
		step("The " + COUNT[n] + " jurors vote " + describe(liftJury) + ": the jury agrees with the referees",
		        "No review. The platform went to " + after + " once the decision had been shown for 3.5 s. The "
		                + "attempt board and the scoreboard decision block still show " + this.attemptStartLine
		                + ", with the jury votes.",
		        List.of(attemptHeldCheck(), votesShown, blockVotes,
		                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

		pauseUntil(lastVote, 12_000);
		step("Twelve seconds after the last jury vote",
		        "The decisions stayed 10 s after the last jury vote on the attempt board, which now shows " + after
		                + ". The scoreboard decision block is still on " + this.attemptStartLine + ".",
		        List.of(left, blockVotes));

		pauseUntil(decided, 30_000);
		step("Thirty seconds after the referees",
		        "The scoreboard decision block stayed 20 s into " + after + ", then cleared.",
		        List.of(left, blockCleared));
	}

	/**
	 * No jury. The attempt board moves to the next athlete after the 3.5 s decision display. The scoreboard decision
	 * block stays on the athlete under review, with the referee decision, for 20 s ("hold") or until the Speaker starts
	 * the next clock ("clock").
	 */
	private void runNoJury(String athlete) throws Exception {
		Scenario sc = this.scenario;
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String refsText = describe(refs);
		Check blockOnReview = decisionBlockReviewCheck(colors(refs), "");

		List<Check> starting = new ArrayList<>(this.setupChecks);
		starting.add(clockCheck("speaker", 60, false));
		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ".", starting);

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		long lastReferee = System.nanoTime();
		pause(4000);
		step("Referees: " + refsText,
		        liftDecision + " (" + refsText + ") on all boards, no jury circles. After the 3.5 s decision display "
		                + "the Speaker, the scoreboard top line and the attempt board move to the next athlete; the "
		                + "scoreboard decision block stays on " + this.attemptStart + " with the referee decision.",
		        plus(topLinesMovedChecks(), attemptOnNextCheck(), blockOnReview));

		if ("clock".equals(sc.noReview())) {
			// an athlete who follows himself has 2:00: the next clock is set to 1:00 so that it can be checked
			clock("60");
			awaitClock(clockCheck("speaker", 60, false));
			// the start must come while the decision block is still on the athlete under review
			awaitClock(blockOnReview);
			clock("start");
			int startMs;
			try {
				startMs = clockMessageMillis(awaitClockMessage("start"));
				if (startMs != 60_000) {
					throw new IllegalStateException("clock must start from 60000 ms: " + startMs);
				}
				awaitRunningClocks();
				List<Check> startedChecks = new ArrayList<>(List.of(decisionBlockClockCheck(), attemptOnNextCheck()));
				for (String page : List.of("speaker", "attempt", "scoreboard")) {
					startedChecks.add(runningClockCheck(page));
				}
				step("Speaker starts the next clock while the decision block is on the athlete under review",
				        "The clock starts from the server's " + startMs + " ms. The scoreboard decision block leaves "
				                + this.attemptStart + " at once and shows the clock for the athlete of the top line.",
				        startedChecks);
			} finally {
				clock("stop");
			}
			clockStoppedStep(startMs, "next");
			return;
		}

		pauseUntil(lastReferee, 14_000);
		step("Fourteen seconds after the referees' decision",
		        "Well after the 3.5 s decision display: the scoreboard decision block is still on " + this.attemptStart
		                + " with the referee decision. The attempt board is on the next athlete.",
		        List.of(blockOnReview, attemptOnNextCheck()));

		pauseUntil(lastReferee, 27_000);
		step("Twenty-seven seconds after the referees' decision",
		        "The decision block kept the decision for 20 s. It is back to the clock, on the athlete of the top "
		                + "line, without decision lights.",
		        List.of(decisionBlockClockCheck(), attemptOnNextCheck()));
	}

	/**
	 * The jury agrees with the referees: no review. The attempt board keeps the athlete under review, with the referee
	 * and jury decisions, for 10 s after the last jury vote ("hold"), or until the Speaker starts the next clock; the
	 * jury has finished voting, so no confirmation is asked ("clock").
	 */
	private void runNoReview(String athlete) throws Exception {
		Scenario sc = this.scenario;
		boolean panel = sc.setup().panel;
		int n = sc.setup().jurors;
		String[] refs = sc.refs();
		String[] liftJury = sc.liftJury();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		String refsText = describe(refs);
		Check votesShown = attemptVotesCheck("the referee decision and the " + n + " revealed jury votes are shown",
		        colors(refs), colors(liftJury));
		Check blockOnReview = decisionBlockReviewCheck(colors(refs), colors(liftJury));

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ".",
		        startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; " + n + " empty jury "
		        + "circles. " + juryVotingText(), juryVotingChecks());

		for (int j = 1; j <= n; j++) {
			juror(j, liftJury[j - 1]);
		}
		long lastVote = System.nanoTime();
		pause(1500);
		List<Check> agreedChecks = new ArrayList<>(List.of(
		        new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")"),
		        new Check("jury", "Deliberation and Challenge still offered", buttonVisible("Jury Deliberation")),
		        attemptHeldCheck(), votesShown, blockOnReview));
		if (!panel) {
			agreedChecks.add(new Check("jury", "Summon Referees (all) is not orange",
			        "!(" + buttonColor("Summon Referees", ORANGE) + ")"));
		}
		step("The " + COUNT[n] + " jurors vote " + describe(liftJury) + ": "
		        + (panel ? "the seven-person majority confirms the referees' decision"
		                : "the jury agrees with the referees"),
		        "No review: the referees' " + liftDecision + " stands. The attempt board stays on "
		                + this.attemptStartLine + " and reveals the " + n + " jury votes with the referee decision. "
		                + "Speaker: no dialog, already on the next athlete.",
		        agreedChecks);

		if ("clock".equals(sc.noReview())) {
			// an athlete who follows himself has 2:00: the next clock is set to 1:00 so that it can be checked
			clock("60");
			awaitClock(clockCheck("speaker", 60, false));
			// the start must come while the board is still on the athlete under review
			awaitClock(attemptHeldCheck());
			clock("start");
			int startMs;
			try {
				startMs = clockMessageMillis(awaitClockMessage("start"));
				if (startMs != 60_000) {
					throw new IllegalStateException("clock must start from 60000 ms: " + startMs);
				}
				awaitRunningClocks();
				List<Check> startedChecks = new ArrayList<>(List.of(
				        new Check("speaker", "no confirmation is asked",
				                "!(" + dialogContains("jury has not finished voting") + ")"),
				        new Check("speaker", "no rejected clock command notification",
				                "!(" + notificationContains("Clock not started") + ")"),
				        attemptOnNextCheck(), decisionBlockClockCheck()));
				for (String page : List.of("speaker", "attempt", "scoreboard")) {
					startedChecks.add(runningClockCheck(page));
				}
				step("Speaker starts the next clock while the board is on the athlete under review",
				        "The jury has finished voting: the clock starts without confirmation, from the server's "
				                + startMs + " ms. The attempt board leaves " + this.attemptStartLine
				                + " and shows the next athlete with the running clock. The scoreboard decision "
				                + "block returns to the clock.",
				        startedChecks);
			} finally {
				clock("stop");
			}
			clockStoppedStep(startMs, "next");
			return;
		}

		pauseUntil(lastVote, 6_000);
		step("At least six seconds after the last jury vote",
		        "Longer than the 3.5 s decision display: the attempt board is still on " + this.attemptStartLine
		                + " with the referee decision and the jury votes, and so is the scoreboard decision block. "
		                + "The Speaker and the scoreboard top line are on the next athlete.",
		        List.of(attemptHeldCheck(), votesShown, blockOnReview));

		pauseUntil(lastVote, 12_000);
		step("Twelve seconds after the last jury vote",
		        "The decisions stayed 10 s after the last jury vote. The attempt board now shows the next athlete "
		                + "with a stopped clock and no decision. The scoreboard decision block is still on the "
		                + "athlete under review: no clock has started. Speaker: no dialog.",
		        List.of(attemptOnNextCheck(), blockOnReview,
		                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")"),
		                new Check("jury", "Deliberation and Challenge still offered",
		                        buttonVisible("Jury Deliberation"))));
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
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; 4 empty jury circles on "
		        + "the boards. " + juryVotingText()
		        + (sc.challenge() ? " Jury Control: Challenge available (the referees' majority is red)." : ""),
		        juryVotingChecks(sc.challenge() ? List.of(challengeAvailable("the referees' majority is red"))
		                : List.of()));

		juror(1, liftJury[0]);
		juror(2, liftJury[1]);
		juror(3, liftJury[2]);
		pause(1000);
		List<Check> threeVotedChecks = new ArrayList<>(List.of(
		        new Check("jury", "Deliberation and Challenge still offered", buttonVisible("Jury Deliberation")),
		        new Check("speaker", "no jury dialog yet", "!(" + dialogContains("Waiting for") + ")")));
		if (sc.challenge()) {
			threeVotedChecks.add(challengeAvailable("the referees' No Lift, no review"));
		}
		step("Three of four jurors have voted on the lift",
		        "No review yet: the jury votes are not complete. Jury Control: Deliberation / Challenge still "
		                + "offered; 3 grey \"voted\" jury circles, values hidden. Boards: the referees' "
		                + liftDecision + " still shown, 3 grey jury circles. Speaker: no dialog."
		                + (sc.challenge() ? " Challenge still available." : ""),
		        threeVotedChecks);

		juror(4, liftJury[3]);
		List<Check> reviewChecks = new ArrayList<>(List.of(new Check("jury", "Call for Votes is orange",
		        buttonColor("Call for Votes", ORANGE)),
		        new Check("jury", "No Lift and Good Lift replace Deliberation and Challenge",
		                buttonVisible("No Lift") + " && " + buttonVisible("Good Lift") + " && !"
		                        + buttonVisible("Jury Deliberation")),
		        new Check("jury", "Summon Referees (all) is disabled", buttonDisabled("Summon Referees")),
		        new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for"))));
		reviewChecks.add(reviewWeightCheck());
		reviewChecks.add(panelLiftVotesCheck());
		if (sc.challenge()) {
			pause(1000);
			boolean juryAgrees = Arrays.stream(liftJury)
			        .allMatch((liftDecision.equals("Good Lift") ? "good" : "bad")::equals);
			step(juryAgrees ? "Fourth juror: the jury agrees with the referees' decision"
			        : "Fourth juror: split jury, the referees' decision stands",
			        "Jury " + describe(liftJury) + ": with the referees' " + refsText + ", the seven-person majority "
			                + "agrees with the " + liftDecision + ", so there is no automatic review. The four jury "
			                + "votes are revealed. Jury Control: Deliberation offered, Challenge still available. "
			                + "Speaker: no dialog.",
			        List.of(new Check("jury", "Deliberation still offered", buttonVisible("Jury Deliberation")),
			                challengeAvailable("no review: the referees' " + liftDecision + " stands"),
			                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

			// the decision display ends and lifting resumes before the coach's challenge
			pause(4000);
			if (sc.afterNextClock()) {
				startNextClockBeforeReview("Challenge");
			}
			click(this.pages.get("jury"), "vaadin-button:has-text('Challenge')",
			        "Jury Chair clicks Challenge on Jury Control");
			pause(1500);
			List<Check> challengeChecks = new ArrayList<>(List.of(reviewChecks.get(0), reviewChecks.get(1),
			        reviewChecks.get(2), reviewChecks.get(3), reviewChecks.get(4),
			        // the new clock cleared the jury votes on the lift
			        sc.afterNextClock()
			                ? attemptVotesCheck("the referees' decision on the lift is shown; the jury votes on the "
			                        + "lift were cleared by the new clock", colors(refs), "empty,empty,empty,empty")
			                : reviewChecks.get(5),
			        new Check("speaker", "the dialog is titled Challenge", dialogContains("Challenge"))));
			challengeChecks.addAll(List.of(reviewAfterNextClockChecks()));
			step("Challenge: the review starts",
			        "Challenge break, same procedure as the automatic review: the referees are not summoned until the "
			                + "vote is called, the votes on the lift stay displayed. Jury Control: No Lift / Good Lift / Call "
			                + "for Votes (orange) / Resume; Summon Referees (all) disabled. Speaker: \"Waiting for "
			                + "decision\" dialog titled Challenge. Boards: Challenge message, the votes on the lift."
			                + reviewAfterNextClockText(),
			        challengeChecks);
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
		        "Same event as Call for Votes on Jury Control: all cells cleared, referees summoned. Jury Control: "
		                + "Call for Votes no longer orange. Boards: empty cells.",
		        List.of(new Check("jury", "Call for Votes no longer orange",
		                "!(" + buttonColor("Call for Votes", ORANGE) + ")"),
		                panelVoteCalledCheck()));

		String[] reviewRefs = sc.reviewRefs();
		String[] reviewJury = sc.reviewJury();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, reviewRefs[r]);
		}
		juror(1, reviewJury[0]);
		juror(2, reviewJury[1]);
		juror(3, reviewJury[2]);
		pause(1500);
		step("Six of seven have voted", "Grey \"voted\" cells everywhere, values hidden.",
		        List.of(attemptVotesCheck("who has voted is shown in grey, values hidden", "voted,voted,voted",
		                "voted,voted,voted,empty")));

		juror(4, reviewJury[3]);
		pause(1500);
		finishReview("Seventh vote", "the seven votes (referees " + describe(reviewRefs) + ", jury "
		        + describe(reviewJury) + ")", outcomeWord, reviewResult);
	}

	private static final String[] COUNT = { "zero", "one", "two", "three", "four", "five" };
	private static final String[] ORDINAL = { "", "First", "Second", "Third", "Fourth", "Fifth" };

	/**
	 * Jury vote (no seven-person panel: 3 members with or without VPT, 4 members without VPT): no automatic trigger;
	 * the Chair starts the review; the referees do not vote again; Jury Control and the boards clear the referee and jury
	 * lights, the jury box clears only the jury lights. Jury Control keeps the original referee decision at the top
	 * right; public boards keep the referee lights blank or hidden, including when the jury votes are revealed.
	 * The vote is called verbally and the jurors punch in their votes; unanimity reverses the
	 * referees. The attempt board then shows the jury circles alone, centered. While the jury votes on the lift, the
	 * attempt board and the scoreboard decision block stay on the athlete under review; the Speaker and the scoreboard
	 * top line move to the next athlete.
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
		        + "circles on the boards. " + juryVotingText()
		        + (sc.challenge() ? " Jury Control: Challenge available (the referees' majority is red)." : ""),
		        juryVotingChecks(sc.challenge() ? List.of(challengeAvailable("the referees' majority is red"))
		                : List.of()));

		for (int j = 1; j < n; j++) {
			juror(j, liftJury[j - 1]);
		}
		pause(1000);
		List<Check> partialChecks = new ArrayList<>(List.of(
		        new Check("jury", "Deliberation and Challenge still offered", buttonVisible("Jury Deliberation")),
		        new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));
		if (sc.challenge()) {
			partialChecks.add(challengeAvailable("the referees' No Lift, no deliberation"));
		}
		step(capitalize(allButLast) + " of " + all + " jurors have voted on the lift",
		        "Jury Control: Deliberation / Challenge offered; " + (n - 1) + " grey \"voted\" jury circles, values "
		                + "hidden. Boards: the referees' " + liftDecision + ", " + (n - 1) + " grey jury circles. "
		                + "Speaker: no dialog." + (sc.challenge() ? " Challenge still available." : ""),
		        partialChecks);

		juror(n, liftJury[n - 1]);
		pause(1500);
		Page juryPage = this.pages.get("jury");
		String reviewName;
		if (sc.challenge()) {
			step(last + " juror: split jury, the referees' decision stands",
			        "Jury " + describe(liftJury) + ": not unanimous, the referees' " + liftDecision + " stands. The "
			                + all + " jury votes are revealed. Jury Control: Deliberation offered, Challenge still "
			                + "available, Summon Referees (all) not orange. Speaker: no dialog.",
			        List.of(new Check("jury", "Deliberation still offered", buttonVisible("Jury Deliberation")),
			                challengeAvailable("no deliberation: the referees' " + liftDecision + " stands"),
			                new Check("jury", "Summon Referees (all) is not orange",
			                        "!(" + buttonColor("Summon Referees", ORANGE) + ")"),
			                new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")));

			// the decision display ends and lifting resumes before the coach's challenge
			pause(4000);
			if (sc.afterNextClock()) {
				startNextClockBeforeReview("Challenge");
			}
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

			if (sc.afterNextClock()) {
				startNextClockBeforeReview("Deliberation");
			}
			click(juryPage, "vaadin-button:has-text('Jury Deliberation')",
			        "Jury Chair clicks Jury Deliberation on Jury Control");
			reviewName = "Deliberation";
		}
		pause(1500);
		step(reviewName + ": the review starts",
		        "Jury break started by the Chair: the referee lights are cleared (they do not vote again), "
		                + "the original referee decision is shown at the right of the top line, the jury lights are "
		                + "cleared and the jury vote is open at once (called verbally). Jury Control: No Lift / Good Lift / Call for Votes (not orange, the vote is "
		                + "open) / Resume; Summon Referees (all) enabled: it summons the referees. Speaker: "
		                + "\"Waiting for decision\" dialog. Attempt board: " + reviewName + " message, no referee "
		                + "lights, " + n + " empty jury circles centered. Scoreboard: referee lights hidden, empty "
		                + "jury cells." + reviewAfterNextClockText(),
		        plus(List.of(new Check("jury", "No Lift and Good Lift replace Deliberation and Challenge",
		                buttonVisible("No Lift") + " && " + buttonVisible("Good Lift") + " && !"
		                        + buttonVisible("Jury Deliberation")),
		                new Check("jury", "Call for Votes is not orange",
		                        "!(" + buttonColor("Call for Votes", ORANGE) + ")"),
		                new Check("jury", "Summon Referees (all) is enabled",
		                        buttonVisible("Summon Referees") + " && !(" + buttonDisabled("Summon Referees") + ")"),
		                new Check("jury", "the original referee decision is at the right of the top line",
		                        originalRefereesShown(refs)),
		                new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for")),
		                reviewWeightCheck(), juryOnlyLayoutCheck()), reviewAfterNextClockChecks()));

		// the Chair calls the vote verbally; the jurors vote a moment later
		pause(1000);
		String[] reviewJury = sc.reviewJury();
		for (int j = 1; j < n; j++) {
			juror(j, reviewJury[j - 1]);
		}
		pause(1500);
		step("Verbal vote: " + allButLast + " of " + all + " jurors have punched in",
		        "Grey \"voted\" jury cells, values hidden; no referee lights on the attempt board. The referees do "
		                + "not vote.",
		        List.of(juryOnlyLayoutCheck()));

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
		        + "circles. " + juryVotingText(), juryVotingChecks(),
		        expectedJuryBox(refs, null, "the referee lights show the original decision; jury lights are off"));

		for (int j = 1; j <= n; j++) {
			juror(j, sc.liftJury()[j - 1]);
		}
		pause(1500);
		if (panel) {
			step("The " + COUNT[n] + " jurors vote " + describe(sc.liftJury()) + ": automatic Adjudication Panel "
			        + "Review", "The seven-person majority is against the referees' " + liftDecision + ". Speaker: "
			                + "\"Waiting for decision\" dialog with Good Lift / No Lift / Resume.",
			        List.of(new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for")),
			                panelLiftVotesCheck()));
			publish("owlcms/jurybox/summon/" + fop, "all");
			pause(1500);
			step("Jury box Summon All: the vote is called", "All cells cleared, referees buzzed.",
			        List.of(panelVoteCalledCheck()));
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
			        "Jury break started from the jury box: Jury Control clears its middle referee indicators and "
			                + "shows the original referee decision at the right of the top line; the physical jury "
			                + "box keeps its referee lights. The jury lights are cleared and the jury vote is open at "
			                + "once (called verbally). Speaker: \"Waiting for decision\" dialog.",
			        List.of(new Check("jury", "the original referee decision is at the right of the top line",
			                originalRefereesShown(refs)),
			                new Check("speaker", "the Speaker waits for the decision",
			                        dialogContains("Waiting for")),
			                juryOnlyLayoutCheck()));
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
		List<Check> completeChecks = new ArrayList<>(List.of(
		        new Check("jury", "Jury Control waits for the Chair's rule", dialogContains("Bending")),
		        new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));
		completeChecks.addAll(reviewVoteChecks());
		step("The vote is complete: " + reviewResult + " result",
		        "Jury Control shows " + allVotes + " and waits for the Jury Chair (rule dialog). The boards reveal "
		                + (panel ? "the seven votes" : "the jury votes; the attempt board shows the jury circles "
		                        + "alone, centered")
		                + ". Speaker still waiting.",
		        completeChecks);

		// the Jury Chair never confirms nor gives the rule
		pause(2000);
		click(speaker, "vaadin-button:has-text('" + reviewResult + "')",
		        "Speaker clicks " + reviewResult + " in the \"Waiting for decision\" dialog");
		long decided = System.nanoTime();
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
		        + "and Challenge are back. Nobody presses Resume. The scoreboard decision block keeps the reviewed "
		        + "athlete and the jury decision.",
		        List.of(new Check("jury", "Deliberation and Challenge are back", buttonVisible("Jury Deliberation")),
		                decisionBlockLabelCheck("the jury decision")));

		pauseUntil(decided, 28_000);
		step("Twenty-eight seconds after the jury decision",
		        "The decision block kept the jury decision for 20 s after lifting resumed. It is back to the clock, "
		                + "on the athlete of the top line, without decision lights.",
		        List.of(decisionBlockClockCheck()));
	}

	/**
	 * The Jury Chair decides with the Good Lift / No Lift buttons of the jury box (MQTT {@code owlcms/jurybox/decision}).
	 * The box has no screen: the decision is applied at once, with no confirmation and no rule. The Speaker dialog shows
	 * it with Close; for a No Lift, in red, the Speaker is told to get the reason from the Technical Controller. The
	 * decision is marked "Jury Chair Decision Against the Vote" against a complete vote, "Jury Chair Entered Decision"
	 * without a vote, and not marked when it agrees with the vote.
	 */
	private void runJuryBoxDecision(String athlete) throws Exception {
		Scenario sc = this.scenario;
		boolean panel = sc.setup().panel;
		int n = sc.setup().jurors;
		String fop = this.options.fop();
		String[] refs = sc.refs();
		String liftDecision = Arrays.stream(refs).filter("good"::equals).count() >= 2 ? "Good Lift" : "No Lift";
		boolean boxGood = "good".equals(sc.boxDecision());
		String boxResult = boxGood ? "Good Lift" : "No Lift";
		String outcomeWord = liftDecision.equals(boxResult) ? "confirmed" : "reversal";
		String refsText = describe(refs);
		boolean voted = sc.reviewJury() != null;
		String outcomeLabel = (panel ? "Panel " : "Jury ")
		        + (liftDecision.equals(boxResult) ? "Confirmation" : "REVERSAL");

		step("Starting point", "Current athlete " + athlete + "; " + sc.setup().description + ". The Jury Chair "
		        + "decides with the jury box buttons.", startingChecks());

		athleteLifts();
		for (int r = 0; r < 3; r++) {
			referee(r + 1, refs[r]);
		}
		pause(4000);
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; " + n + " empty jury "
		        + "circles. " + juryVotingText(), juryVotingChecks());

		for (int j = 1; j <= n; j++) {
			juror(j, sc.liftJury()[j - 1]);
		}
		pause(1500);
		if (panel) {
			step("The " + COUNT[n] + " jurors vote " + describe(sc.liftJury()) + ": automatic Adjudication Panel "
			        + "Review", "The seven-person majority is against the referees' " + liftDecision + ". Speaker: "
			                + "\"Waiting for decision\" dialog.",
			        List.of(new Check("speaker", "the Speaker waits for the decision",
			                dialogContains("Waiting for")),
			                reviewWeightCheck(), panelLiftVotesCheck()),
			        expectedJuryBox(refs, sc.liftJury(),
			                "the original referee and Phase A jury decisions remain displayed until the vote is called"));
		} else {
			step("The " + COUNT[n] + " jurors vote " + describe(sc.liftJury()) + " against the referees",
			        "No automatic trigger. Speaker: no dialog.",
			        List.of(new Check("speaker", "no jury dialog", "!(" + dialogContains("Waiting for") + ")")),
			        expectedJuryBox(refs, sc.liftJury(),
			                "the original referee and jury decisions remain displayed before Deliberation"));
			publish("owlcms/jurybox/break/" + fop, "deliberation");
			pause(1500);
			step("Jury box: Deliberation",
			        "Jury break started from the jury box. Jury Control clears its middle referee indicators and "
			                + "shows the original referee decision at the right of the top line; the jury vote is "
			                + "open at once. Speaker: \"Waiting for decision\" dialog.",
			        List.of(new Check("jury", "the original referee decision is at the right of the top line",
			                originalRefereesShown(refs)),
			                new Check("speaker", "the Speaker waits for the decision",
			                        dialogContains("Waiting for")),
			                reviewWeightCheck(), juryOnlyLayoutCheck()),
			        expectedJuryBox(refs, null,
			                "manual mode keeps the original referee lights on and clears the jury lights"));
		}

		if (sc.summonForDiscussion()) {
			publish("owlcms/jurybox/summon/" + fop, "all");
			pause(1500);
			action("Scenario assumption: the referees discuss the lift with the jury before the new jury vote; "
			        + "the referees do not vote again");
			step("Jury box: Summon Referees for discussion",
			        "OWLCMS summons referees 1, 2 and 3. Their referee-box lights are blue for this step. "
			                + "The jury deliberates with them before its new vote; the original referee decision "
			                + "and the open jury vote are unchanged. Speaker still waits for the decision.",
			        List.of(new Check("jury", "the original referee decision stays at the right of the top line",
			                originalRefereesShown(refs)),
			                new Check("speaker", "the Speaker still waits for the decision",
			                        dialogContains("Waiting for"))),
			        expectedJuryBox(refs, null,
			                "summoning for discussion keeps the original referee lights and empty jury lights"));
		}

		boolean voteGood = false;
		if (voted) {
			if (panel) {
				publish("owlcms/jurybox/summon/" + fop, "all");
				pause(1500);
				step("Jury box Summon All: the vote is called",
				        "The vote indicators are cleared and the referees are summoned. The original referee "
				                + "decision stays at the right of the top line.",
				        plus(originalRefereeChecks(true), panelVoteCalledCheck()),
				        juryBoxOff("the seven-person vote call hides all original referee and jury lights"));
				for (int r = 0; r < 3; r++) {
					referee(r + 1, sc.reviewRefs()[r]);
				}
			}
			for (int j = 1; j <= n; j++) {
				juror(j, sc.reviewJury()[j - 1]);
			}
			pause(1500);
			long good = Arrays.stream(sc.reviewJury()).filter("good"::equals).count();
			if (panel) {
				good += Arrays.stream(sc.reviewRefs()).filter("good"::equals).count();
				voteGood = good >= 4;
			} else {
				// unanimity reverses, otherwise the referees' decision stands
				voteGood = good == n || (good != 0 && "Good Lift".equals(liftDecision));
			}
			String voteResult = voteGood ? "Good Lift" : "No Lift";
			String allVotes = panel
			        ? "the seven votes (referees " + describe(sc.reviewRefs()) + ", jury " + describe(sc.reviewJury())
			                + ")"
			        : "the " + COUNT[n] + " jury votes (" + describe(sc.reviewJury()) + ")";
			List<Check> voteChecks = new ArrayList<>(List.of(new Check("jury", "Jury Control waits for the Chair",
			        voteGood ? dialogButton("Confirm Good Lift") : dialogContains("Bending")),
			        new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));
			voteChecks.addAll(reviewVoteChecks());
			step("The vote is complete: " + voteResult + " result",
			        "Jury Control shows " + allVotes + " and waits for the Jury Chair ("
			                + (voteGood ? "Confirm Good Lift" : "rule dialog") + "). Speaker still waiting.",
			        voteChecks,
			        expectedJuryBox(panel ? sc.reviewRefs() : refs, sc.reviewJury(),
			                panel ? "the seven review votes are revealed"
			                        : "the original referee lights remain and the jury review votes are revealed"));
		}

		publish("owlcms/jurybox/decision/" + fop, sc.boxDecision());
		pause(1500);
		boolean againstVote = voted && voteGood != boxGood;
		String marker = !voted ? "Jury Chair Entered Decision"
		        : againstVote ? "Jury Chair Decision Against the Vote" : null;
		String tcReason = "Get the reason from the Technical Controller";
		List<Check> checks = new ArrayList<>(List.of(
		        new Check("speaker", "the Speaker sees the decision with a Close button",
		                dialogContains(boxResult.toUpperCase()) + " && " + buttonVisible("Close")),
		        new Check("speaker", "the Speaker sees " + outcomeLabel, dialogContains(outcomeLabel)),
		        new Check("speaker", "the Chair's box decision is already applied; the Speaker cannot enter a decision",
		                "!(" + dialogButton("Good Lift") + ") && !(" + dialogButton("No Lift") + ")"),
		        boxGood ? new Check("speaker", "no request for the reason", "!(" + dialogContains(tcReason) + ")")
		                : new Check("speaker", "the Speaker is told to wait for the Technical Controller's verbal reason",
		                        dialogContains(tcReason)),
		        marker != null ? new Check("speaker", "marked " + marker, dialogContains(marker))
		                : new Check("speaker", "not marked as a Chair decision",
		                        "!(" + dialogContains("Jury Chair Entered") + ") && !("
		                                + dialogContains("Against the Vote") + ")"),
		        new Check("jury", "no rule dialog nor result screen left on Jury Control",
		                "!(" + dialogContains("Bending") + ") && !(" + dialogContains("Cancel vote result") + ")")));
		checks.addAll(originalRefereeChecks(originalRefereesAtTopRight()));
		JuryBoxExpectation decisionLights = againstVote || !voted
		        ? juryBoxOff("a Chair button decision not matching a complete vote clears the device lights")
		        : expectedJuryBox(panel ? sc.reviewRefs() : refs, sc.reviewJury(),
		                "the Chair decision agrees with the complete vote, so its lights remain");
		step("Jury box: the Jury Chair presses " + boxResult,
		        "Applied at once, no confirmation and no rule: " + boxResult + " (" + outcomeWord + ") on the boards. "
		                + "Speaker dialog: " + boxResult + ", Close button"
		                + (boxGood ? "" : ". The Speaker is instructed to obtain the Technical Controller's verbal "
		                        + "reason, then announce the decision and reason before calling the next athlete.")
		                + (marker != null ? " Marked " + marker + "." : " Not marked: it agrees with the vote.")
		                + (againstVote ? " The review vote indicators are cleared because the decision is not the "
		                        + "vote's." : "")
		                + " The original referee decision stays at the right of the top line.",
		        checks, decisionLights);

		if (!panel && voted && !againstVote) {
			if (!boxGood) {
				action("Scenario assumption: the Technical Controller gives the reason verbally to the Speaker; "
				        + "no reason is entered in OWLCMS");
			}
			action("Scenario assumption: the Speaker announces " + boxResult
			        + (boxGood ? "" : " and the reason supplied verbally by the Technical Controller"));
		}
		closeDecision(boxResult, originalRefereesAtTopRight());
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
		step("Referees: " + refsText, liftDecision + " (" + refsText + ") on all boards; 4 empty jury circles. "
		        + juryVotingText(), juryVotingChecks());

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
		                new Check("speaker", "the Speaker waits for the decision", dialogContains("Waiting for")),
		                panelLiftVotesCheck()));

		click(juryPage, "vaadin-button:has-text('Call for Votes')", "Jury Chair clicks Call for Votes on Jury Control");
		pause(1500);
		step("Call for Votes", "All vote cells cleared, referees buzzed. Call for Votes no longer orange. The original "
		        + "referee decision appears at the right of the top line and stays until the next clock starts.",
		        List.of(new Check("jury", "Call for Votes no longer orange",
		                "!(" + buttonColor("Call for Votes", ORANGE) + ")"),
		                panelVoteCalledCheck()));

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
		                new Check("speaker", "the Speaker still waits", dialogContains("Waiting for")),
		                attemptVotesCheck("who has voted is shown in grey, values hidden; juror " + broken
		                        + " stays empty", "voted,voted,voted", juryStates(broken, "voted"))));

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

		closeDecision(reviewResult, originalRefereesAtTopRight());
	}

	/**
	 * After the last review vote: the Chair confirms (Good Lift) or gives the rule (No Lift), then the Speaker closes
	 * the dialog and lifting resumes.
	 */
	@SuppressWarnings("unused")
	private void finishReview(String lastVote, String allVotes, String outcomeWord, String reviewResult)
	        throws Exception {
		boolean good = this.scenario.goodResult();
		Page juryPage = this.pages.get("jury");
		Page speaker = this.pages.get("speaker");
		List<Check> original = originalRefereeChecks(originalRefereesAtTopRight());
		String publicVotes = this.scenario.setup().panel
		        ? "Attempt board and scoreboard reveal all seven new review votes at once."
		        : "Attempt board and scoreboard reveal only the " + this.scenario.setup().jurors
		                + " new jury votes, centered on the attempt board; the referee lights are not shown on the "
		                + "attempt board and hidden on the scoreboard.";
		if (good) {
			List<Check> resultChecks = new ArrayList<>(List.of(new Check("jury",
			        "the result screen offers Confirm Good Lift", dialogButton("Confirm Good Lift")),
			        new Check("jury", "and Cancel vote result", dialogButton("Cancel vote result")),
			        new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));
			resultChecks.addAll(original);
			resultChecks.addAll(reviewVoteChecks());
			step(lastVote + ": Good Lift result",
			        "Jury Control shows " + allVotes + " and the result screen: Confirm Good Lift / Cancel vote "
			                + "result. " + publicVotes + " Speaker still "
			                + "waiting. Nothing applied yet."
			                + (originalRefereesAtTopRight() ? " Jury Control shows the original referee decision "
			                        + "at the right, in place of the platform selectors." : ""),
			        resultChecks);

			click(juryPage, "vaadin-button:has-text('Confirm Good Lift')",
			        "Jury Chair clicks Confirm Good Lift on Jury Control");
			pause(1500);
			List<Check> decisionChecks = new ArrayList<>(List.of(new Check("speaker",
			        "the Speaker sees the decision with a Close button",
			        dialogContains("GOOD LIFT") + " && " + buttonVisible("Close")),
			        new Check("speaker", "not marked as entered by the Jury Chair",
			                "!(" + dialogContains("Jury Chair Entered") + ")")));
			decisionChecks.addAll(original);
			step("The Jury Chair confirms the Good Lift",
			        "Decision applied at once: Good Lift (" + outcomeWord + ") pops over the votes on the "
			                + "boards. Speaker dialog: Good Lift (" + outcomeWord + "), \"announce\" text, Close "
			                + "button. Not marked Jury Chair "
			                + "Entered Decision."
			                + (originalRefereesAtTopRight() ? " The original referee decision stays shown on Jury "
			                        + "Control." : ""),
			        decisionChecks);
		} else {
			List<Check> resultChecks = new ArrayList<>(List.of(
			        new Check("jury", "the rule dialog is open", dialogContains("Bending")),
			        new Check("speaker", "the Speaker still waits", dialogContains("Waiting for"))));
			resultChecks.addAll(original);
			resultChecks.addAll(reviewVoteChecks());
			step(lastVote + ": No Lift result",
			        "Jury Control shows " + allVotes + " and the rule dialog, with Cancel vote result. "
			                + publicVotes + " Speaker still waiting."
			                + (originalRefereesAtTopRight() ? " Jury Control shows the original referee decision "
			                        + "at the right, in place of the platform selectors." : ""),
			        resultChecks);

			click(juryPage, "vaadin-button[aria-label^='1 - ']", "Jury Chair picks rule 1 on Jury Control");
			pause(1500);
			List<Check> decisionChecks = new ArrayList<>(List.of(new Check("speaker",
			        "the Speaker sees the decision with a Close button",
			        dialogContains("NO LIFT") + " && " + buttonVisible("Close")),
			        new Check("speaker", "not marked as entered by the Jury Chair",
			                "!(" + dialogContains("Jury Chair Entered") + ")")));
			decisionChecks.addAll(original);
			step("The Jury Chair gives the rule",
			        "Decision applied at once: No Lift with rule 1 pops over the votes on the boards. Speaker "
			                + "dialog: No Lift (" + outcomeWord + ") with the rule, \"announce\" text, Close button. "
			                + "Not marked "
			                + "Jury Chair Entered Decision."
			                + (originalRefereesAtTopRight() ? " The rule dialog is closed; the original referee "
			                        + "decision stays shown on Jury Control." : ""),
			        decisionChecks);
		}

		closeDecision(reviewResult, originalRefereesAtTopRight());
	}

	private List<Check> reviewVoteChecks() {
		boolean panel = this.scenario.setup().panel;
		String reviewRefs = panel
		        ? String.join(",", Arrays.stream(this.scenario.reviewRefs()).map(v -> "good".equals(v) ? "white" : "red")
		                .toList())
		        : "none,none,none";
		String expectedJury = String.join(",",
		        Arrays.stream(this.scenario.reviewJury()).map(v -> "good".equals(v) ? "white" : "red").toList());
		List<Check> checks = new ArrayList<>();
		for (String page : List.of("attempt", "scoreboard")) {
			boolean attempt = "attempt".equals(page);
			String host = attempt ? "attempt-board-template" : "results-template";
			String decisionId = attempt ? "#decisions" : "#decisionSectionReferee";
			String jurySelector = attempt ? ".juryLight" : ".juryIcon";
			// jury-only vote: the attempt board does not draw the referee boxes, the scoreboard hides them
			String expectedRefs = attempt && !panel ? "" : reviewRefs;
			String predicate = """
			        (() => {
			          const board = document.querySelector(%s);
			          const decision = board?.shadowRoot?.querySelector(%s);
			          if (!decision || Boolean(decision.offsetWidth) !== %s) return false;
			          const refs = [...(decision.shadowRoot?.querySelectorAll('.decisions span') || [])];
			          const juryRoot = %s;
			          const jury = [...(juryRoot?.querySelectorAll(%s) || [])];
			          const colors = lights => lights.map(e =>
			            ['white', 'red', 'none'].find(c => e.classList.contains(c))).join(',');
			          return refs.length === %d && colors(refs) === %s && colors(jury) === %s
			            && jury.every(e => e.offsetWidth > 0);
			        })()
			        """.formatted(js(host), js(decisionId), panel || attempt,
			        attempt ? "decision.shadowRoot" : "board.shadowRoot",
			        js(jurySelector), expectedRefs.isEmpty() ? 0 : 3, js(expectedRefs), js(expectedJury));
			checks.add(new Check(page, panel ? "all seven new review votes are shown"
			        : "only the " + this.scenario.setup().jurors
			                + " jury review votes are shown; the referee lights are "
			                + (attempt ? "not drawn" : "hidden"), predicate));
		}
		if (!panel) {
			checks.add(juryOnlyLayoutCheck());
		}
		return checks;
	}

	/** jury-only review (no seven-person panel): the attempt board shows the jury circles alone, centered */
	private Check juryOnlyLayoutCheck() {
		int jurors = this.scenario.setup().jurors;
		return new Check("attempt", "no referee lights; the " + jurors + " jury circles are centered",
		        """
		        (() => {
		          const decision = document.querySelector('attempt-board-template')?.shadowRoot
		            ?.querySelector('#decisions');
		          const root = decision?.shadowRoot;
		          if (!root || !decision.offsetWidth) return false;
		          const jury = [...root.querySelectorAll('.juryLight')];
		          if (root.querySelectorAll('.decisions span').length !== 0 || jury.length !== %d
		              || !jury.every(e => e.offsetWidth > 0)) return false;
		          const box = decision.getBoundingClientRect();
		          const first = jury[0].getBoundingClientRect();
		          const last = jury[jury.length - 1].getBoundingClientRect();
		          return Math.abs((first.left + last.right) / 2 - (box.left + box.right) / 2) < 5
		            && Math.abs((first.top + first.bottom) / 2 - (box.top + box.bottom) / 2) < 5;
		        })()
		        """.formatted(jurors));
	}

	private static final String SPEAKER_TOP_LINE = """
	        ['current-athlete-name', 'current-athlete-attempt'].map(id =>
	          (document.querySelector('[data-testid="' + id + '"]')?.innerText || '').trim().replace(/\\s+/g, ' '))
	          .join(' | ')
	        """;

	private static final String SCOREBOARD_TOP_LINE = """
	        ['.attemptBar .fullName', '.attemptBar .attempt'].map(selector =>
	          (document.querySelector('results-template')?.shadowRoot?.querySelector(selector)?.textContent || '')
	            .trim().replace(/\\s+/g, ' ')).join(' | ')
	        """;

	/** last name and attempt of the attempt board, also when the attempt is hidden by the decision lights */
	private static final String ATTEMPT_LINE = """
	        ['attempt-board-last-name', 'attempt-board-attempt'].map(id =>
	          (document.querySelector('attempt-board-template')?.shadowRoot
	            ?.querySelector('[data-testid="' + id + '"]')?.textContent || '').trim().replace(/\\s+/g, ' '))
	          .join(' | ')
	        """;

	private static String colors(String[] votes) {
		return String.join(",", Arrays.stream(votes).map(v -> "good".equals(v) ? "white" : "red").toList());
	}

	/** the given state for every juror who voted, "empty" for a jury box that does not work (0: none) */
	private String juryStates(int broken, String voted) {
		List<String> states = new ArrayList<>();
		for (int j = 1; j <= this.scenario.setup().jurors; j++) {
			states.add(j == broken ? "empty" : voted);
		}
		return String.join(",", states);
	}

	private static List<Check> plus(List<Check> checks, Check... more) {
		List<Check> all = new ArrayList<>(checks);
		all.addAll(List.of(more));
		return all;
	}

	/**
	 * The decision area of the attempt board.
	 *
	 * @param expectedRefs the three referee boxes, left to right: white, red, voted or none (empty box)
	 * @param expectedJury the jury circles, left to right: white, red, voted or empty
	 */
	private static Check attemptVotesCheck(String description, String expectedRefs, String expectedJury) {
		return new Check("attempt", description,
		        """
		        (() => {
		          const decision = document.querySelector('attempt-board-template')?.shadowRoot
		            ?.querySelector('#decisions');
		          const root = decision?.shadowRoot;
		          if (!root || !decision.offsetWidth) return false;
		          const states = (lights, names) => lights.map(e =>
		            names.find(c => e.classList.contains(c))).join(',');
		          return states([...root.querySelectorAll('.decisions span')], ['white', 'red', 'voted', 'none']) === %s
		            && states([...root.querySelectorAll('.juryLight')], ['white', 'red', 'voted', 'empty']) === %s;
		        })()
		        """.formatted(js(expectedRefs), js(expectedJury)));
	}

	/**
	 * Seven-person panel, review started, vote not called yet: the attempt board keeps the votes on the lift. The jury
	 * votes are revealed only when the four jurors voted on the lift.
	 */
	private Check panelLiftVotesCheck() {
		Scenario sc = this.scenario;
		return attemptVotesCheck("the referee decision and the jury votes on the lift stay shown until the vote is "
		        + "called", colors(sc.refs()),
		        sc.brokenJuror() > 0 ? juryStates(sc.brokenJuror(), "voted") : colors(sc.liftJury()));
	}

	/** seven-person panel, vote called: the seven cells are cleared */
	private static Check panelVoteCalledCheck() {
		return attemptVotesCheck("the three referee boxes and the four jury circles are cleared", "none,none,none",
		        "empty,empty,empty,empty");
	}

	/** the attempt board is still on the attempt of the athlete under review, with the decision lights */
	private Check attemptHeldCheck() {
		return new Check("attempt", "still on " + this.attemptStartLine + " with the decision lights",
		        """
		        (() => {
		          const decision = document.querySelector('attempt-board-template')?.shadowRoot
		            ?.querySelector('#decisions');
		          return !!decision && decision.offsetWidth > 0 && (%s) === %s;
		        })()
		        """.formatted(ATTEMPT_LINE, js(this.attemptStartLine)));
	}

	/** the attempt board has left the athlete under review: next attempt, athlete clock, no decision lights */
	private Check attemptOnNextCheck() {
		return new Check("attempt", "on the next attempt (was " + this.attemptStartLine + "), athlete clock "
		        + "visible, no decision lights",
		        """
		        (() => {
		          const board = document.querySelector('attempt-board-template')?.shadowRoot;
		          const timer = board?.querySelector('#athleteTimer');
		          const line = %s;
		          return !!board && !board.querySelector('#decisions')?.offsetWidth && !!timer
		            && timer.checkVisibility() && line.length > 3 && line !== %s;
		        })()
		        """.formatted(ATTEMPT_LINE, js(this.attemptStartLine)));
	}

	private static final String ATTEMPT_LAST_NAME = """
	        (document.querySelector('attempt-board-template')?.shadowRoot
	          ?.querySelector('[data-testid="attempt-board-last-name"]')?.textContent || '').trim()
	        """;

	private String juryVotingText() {
		return "After the 3.5 s decision display the Speaker and the scoreboard top line move to the next athlete; "
		        + "the attempt board and the scoreboard decision block stay on " + this.attemptStart
		        + ", with the referee decision, while the jury votes.";
	}

	/** the competition flow has moved on: the Speaker and the scoreboard top line show the next athlete or attempt */
	private List<Check> topLinesMovedChecks() {
		return List.of(
		        new Check("speaker", "the top line has moved to the next athlete or attempt (was \""
		                + this.speakerStart + "\")",
		                "(() => { const now = " + SPEAKER_TOP_LINE + "; return now.length > 3 && now !== "
		                        + js(this.speakerStart) + "; })()"),
		        new Check("scoreboard", "the top line has moved to the next athlete or attempt (was \""
		                + this.scoreboardStart + "\")",
		                "(() => { const now = " + SCOREBOARD_TOP_LINE + "; return now.length > 3 && now !== "
		                        + js(this.scoreboardStart) + "; })()"));
	}

	/**
	 * The scoreboard decision block shows the athlete under review.
	 *
	 * @param expectedRefs the three referee lights: white, red, voted or none
	 * @param expectedJury the visible jury circles: white, red, voted or empty; "" for none; null when not checked
	 */
	private Check decisionBlockReviewCheck(String expectedRefs, String expectedJury) {
		String juryJs = expectedJury == null ? "null" : js(expectedJury);
		String jury = expectedJury == null ? "" : expectedJury.isEmpty() ? "; no jury circles" : "; jury " + expectedJury;
		return new Check("scoreboard", "the decision block is on " + this.attemptStart
		        + ", the athlete under review: referees " + expectedRefs + jury,
		        """
		        (() => {
		          const host = document.querySelector('results-template');
		          const root = host?.shadowRoot;
		          const decision = root?.querySelector('#decisionSectionReferee');
		          if (!root || host.decisionSectionDecisionActive !== true || !decision?.offsetWidth
		              || !root.querySelector('.dsDecisionAthlete')?.offsetWidth) return false;
		          const name = (root.querySelector('.dsDecisionAthleteFullName')?.textContent || '').toUpperCase();
		          const states = (lights, names) => lights.map(e =>
		            names.find(c => e.classList.contains(c))).join(',');
		          const jury = [...root.querySelectorAll('.dsJurySlot .juryIcon')].filter(e => e.offsetWidth > 0);
		          return name.includes(%s)
		            && states([...(decision.shadowRoot?.querySelectorAll('.decisions span') || [])],
		              ['white', 'red', 'voted', 'none']) === %s
		            && (%s === null || states(jury, ['white', 'red', 'voted', 'empty']) === %s);
		        })()
		        """.formatted(js(this.attemptStart.toUpperCase()), js(expectedRefs), juryJs, juryJs));
	}

	/**
	 * The decision block shows the athlete under review with a label: the review in progress, or the jury decision
	 * (kept after lifting resumes, whatever the jury).
	 */
	private Check decisionBlockLabelCheck(String label) {
		return new Check("scoreboard", "the decision block is on " + this.attemptStart
		        + ", the athlete under review, with " + label,
		        """
		        (() => {
		          const host = document.querySelector('results-template');
		          const root = host?.shadowRoot;
		          const message = root?.querySelector('.dsJuryMessage');
		          if (!root || host.decisionSectionDecisionActive !== true
		              || !root.querySelector('.dsDecisionAthlete')?.offsetWidth) return false;
		          const name = (root.querySelector('.dsDecisionAthleteFullName')?.textContent || '').toUpperCase();
		          return name.includes(%s) && !!message?.offsetWidth && message.textContent.trim().length > 0;
		        })()
		        """.formatted(js(this.attemptStart.toUpperCase())));
	}

	/** the scoreboard decision block no longer shows the athlete under review: no name, no decision lights */
	private Check decisionBlockClearedCheck() {
		return new Check("scoreboard", "the decision block no longer shows " + this.attemptStart
		        + ": no decision lights, no jury circles",
		        """
		        (() => {
		          const host = document.querySelector('results-template');
		          const root = host?.shadowRoot;
		          if (!root || host.decisionSectionDecisionActive !== false) return false;
		          const name = root.querySelector('.dsDecisionAthleteFullName');
		          const shown = name?.offsetWidth ? (name.textContent || '').toUpperCase() : '';
		          const jury = [...root.querySelectorAll('.dsJurySlot .juryIcon')].some(e => e.offsetWidth > 0);
		          return !shown.includes(%s) && !root.querySelector('#decisionSectionReferee')?.offsetWidth && !jury;
		        })()
		        """.formatted(js(this.attemptStart.toUpperCase())));
	}

	/** the scoreboard decision block is back to the clock: the athlete of the top line, no decision lights */
	private static Check decisionBlockClockCheck() {
		return new Check("scoreboard", "the decision block is back to the clock, on the athlete of the top line, "
		        + "without decision lights",
		        """
		        (() => {
		          const host = document.querySelector('results-template');
		          const root = host?.shadowRoot;
		          if (!root || host.decisionSectionDecisionActive !== false) return false;
		          const clean = e => (e?.textContent || '').trim().replace(/\\s+/g, ' ').toUpperCase();
		          const name = clean(root.querySelector('.dsDecisionAthleteFullName'));
		          const top = clean(root.querySelector('.attemptBar .fullName'));
		          const timer = root.querySelector('#decisionSectionTimer');
		          const jury = [...root.querySelectorAll('.dsJurySlot .juryIcon')].some(e => e.offsetWidth > 0);
		          return name.length > 1 && top.includes(name.split(',')[0].trim())
		            && !!root.querySelector('.dsDecisionAthlete')?.offsetWidth
		            && !!timer && timer.checkVisibility()
		            && !root.querySelector('#decisionSectionReferee')?.offsetWidth && !jury;
		        })()
		        """);
	}

	private List<Check> juryVotingChecks() {
		return juryVotingChecks(List.of());
	}

	/**
	 * After the referees' decision, with a jury: the competition flow does not wait for the jury. The checks on the
	 * boards come after the checks on the top lines, so they are made once the competition flow has moved on.
	 */
	private List<Check> juryVotingChecks(List<Check> first) {
		Scenario sc = this.scenario;
		String expectedRefs = String.join(",",
		        Arrays.stream(sc.refs()).map(v -> "good".equals(v) ? "white" : "red").toList());
		List<Check> checks = new ArrayList<>(first);
		checks.addAll(topLinesMovedChecks());
		checks.add(new Check("attempt", "still on " + this.attemptStartLine + " with the referee decision and "
		        + sc.setup().jurors + " jury circles",
		        """
		        (() => {
		          const board = document.querySelector('attempt-board-template')?.shadowRoot;
		          const decision = board?.querySelector('#decisions');
		          if (!decision || !decision.offsetWidth || (%s) !== %s) return false;
		          const refs = [...(decision.shadowRoot?.querySelectorAll('.decisions span') || [])];
		          const jury = [...(decision.shadowRoot?.querySelectorAll('.juryLight') || [])];
		          const colors = lights => lights.map(e =>
		            ['white', 'red', 'none'].find(c => e.classList.contains(c))).join(',');
		          return colors(refs) === %s && jury.length === %d && jury.every(e => e.offsetWidth > 0);
		        })()
		        """.formatted(ATTEMPT_LINE, js(this.attemptStartLine), js(expectedRefs), sc.setup().jurors)));
		checks.add(new Check("scoreboard", "the decision block is still on " + this.attemptStart
		        + " with the referee decision",
		        """
		        (() => {
		          const root = document.querySelector('results-template')?.shadowRoot;
		          const athlete = root?.querySelector('.dsDecisionAthlete');
		          const decision = root?.querySelector('#decisionSectionReferee');
		          if (!athlete || !athlete.offsetWidth || !decision || !decision.offsetWidth) return false;
		          const name = (root.querySelector('.dsDecisionAthleteFullName')?.textContent || '').toUpperCase();
		          const refs = [...(decision.shadowRoot?.querySelectorAll('.decisions span') || [])];
		          const colors = lights => lights.map(e =>
		            ['white', 'red', 'none'].find(c => e.classList.contains(c))).join(',');
		          return name.includes(%s) && colors(refs) === %s;
		        })()
		        """.formatted(js(this.attemptStart.toUpperCase()), js(expectedRefs))));
		return checks;
	}

	/** the Speaker closes the decision dialog and lifting resumes */
	private void closeDecision(String reviewResult, boolean originalRefereesShown) throws Exception {
		Page speaker = this.pages.get("speaker");
		click(speaker, "vaadin-button:has-text('Close')", "Speaker clicks Close");
		pause(5000);
		List<Check> resumedChecks = new ArrayList<>(List.of(new Check("speaker", "the dialog is closed",
		        "!(" + dialogContains(reviewResult.toUpperCase()) + ")"),
		        new Check("jury", "Deliberation and Challenge are back", buttonVisible("Jury Deliberation")),
		        challengeUnavailable("the lift has been reviewed")));
		resumedChecks.addAll(originalRefereeChecks(originalRefereesShown));
		resumedChecks.add(decisionBlockLabelCheck("the jury decision"));
		step("Speaker closes the dialog; lifting resumes",
		        "Lifting resumed by itself after 3.5 s: next athlete. Jury Control: Deliberation and Challenge "
		                + "are back."
		                + " The scoreboard decision block keeps the reviewed athlete and the jury decision (20 s "
		                + "after lifting resumes, or until the next clock)."
		                + (originalRefereesShown ? " The reviewed athlete and original referee decision are still shown "
		                        + "in the top line." : ""),
		        resumedChecks);
	}

	/** checks for the original referee decision shown at the right of the Jury Control top line */
	private List<Check> originalRefereeChecks(boolean shown) {
		if (!shown) {
			return List.of(new Check("jury", "no original referee decision in the top line, the platform selectors stay",
			        originalRefereesHidden()));
		}
		return List.of(new Check("jury", "the original referee decision (" + describe(this.scenario.refs())
		        + ") is shown at the right of the top line",
		        originalRefereesShown(this.scenario.refs())));
	}

	/**
	 * Mirrors DecisionDisplayRules.SHOW_ORIGINAL_DECISIONS in owlcms: when on, every review vote shows the original
	 * referee decision at the top right of Jury Control when the middle referee indicators are cleared for the review
	 * vote, not only for the seven-person panel. It stays there through Resume, until the next clock starts.
	 */
	private static final boolean SHOW_ORIGINAL_DECISIONS = true;

	private boolean originalRefereesAtTopRight() {
		return this.scenario.setup().panel || SHOW_ORIGINAL_DECISIONS;
	}

	private static String originalRefereesShown(String[] refs) {
		return "(() => { const e = document.getElementById('originalRefereeDecisions'); return !!e && !e.hidden "
		        + "&& e.offsetWidth > 0 && [...e.children].map(c => c.dataset.decision).join(',') === "
		        + js(String.join(",", refs)) + " && e.getBoundingClientRect().right > window.innerWidth - 40 "
		        + "&& ![...document.querySelectorAll('vaadin-combo-box, vaadin-select')].some(c => c.offsetWidth); })()";
	}

	private static String originalRefereesHidden() {
		return "(() => { const e = document.getElementById('originalRefereeDecisions'); "
		        + "return (!e || e.hidden || e.offsetWidth === 0) "
		        + "&& [...document.querySelectorAll('vaadin-combo-box')].some(c => c.offsetWidth); })()";
	}

	private static String capitalize(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private List<Check> startingChecks() {
		List<Check> checks = new ArrayList<>(this.setupChecks);
		checks.add(clockCheck("speaker", 60, false));
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
		switches.put("juryDecisionsOnScoreboard", true);
		String configPage = this.options.base() + "/preparation/config";
		openFeaturesTab(page, configPage);
		switches.forEach((id, on) -> setChecked(switchBox(page, id), on));
		clickUpdate(page);
		openFeaturesTab(page, configPage);
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
		action("Features: automaticBreakNoVPT off, juryDecisionsOnScoreboard on");
		action("Competition Rules: Video Playback Technology " + (setup.vpt ? "in use" : "not in use"));

		Page jury = this.pages.get("jury");
		jury.reload();
		jury.waitForLoadState(LoadState.NETWORKIDLE);
		endLeftoverJuryCycle(jury);
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
		try {
			// A clock used to clear the previous jury header is setup only; setting time stops it.
			clock("60");
			awaitClock(clockCheck("speaker", 60, false));
		} catch (Exception e) {
			throw new IllegalStateException("cannot reset the scenario clock to 1:00", e);
		}
	}

	/**
	 * An interrupted run can leave a jury cycle open. The jury box Resume ends it, even when a jury dialog covers the
	 * page; outside a jury cycle it changes nothing. Resume deliberately leaves the previous decision in the top line
	 * until the next clock starts.
	 */
	private void endLeftoverJuryCycle(Page jury) {
		try {
			publish("owlcms/jurybox/break/" + this.options.fop(), "stop");
		} catch (Exception e) {
			throw new IllegalStateException("cannot end a jury cycle left over from a previous run", e);
		}
		action("Jury box Resume ends any jury cycle left over from a previous run");
		jury.waitForTimeout(750);
	}

	/** the Jury Control toolbar selection of the number of jurors (shown without the seven-person panel) */
	private void setJurySize(Page jury, int jurors) {
		// the only select on Jury Control
		Locator select = jury.locator("vaadin-select").first();
		if (!select.isVisible()) {
			try {
				// an athlete who follows himself has 2:00: every clock of a scenario runs from 1:00
				clock("60");
				jury.waitForTimeout(500);
				clock("start");
			} catch (Exception e) {
				throw new IllegalStateException("cannot start the next clock to reset the previous top line", e);
			}
			action("Next athlete's clock is set to 1:00, starts and resets the previous top line");
			select.waitFor(new Locator.WaitForOptions().setTimeout(8_000));
		}
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

	/** the tab is selected by its name: the tab order of the settings page changes between versions */
	private static void openFeaturesTab(Page page, String configPage) {
		page.navigate(configPage);
		page.waitForLoadState(LoadState.NETWORKIDLE);
		page.locator("vaadin-tab").filter(new Locator.FilterOptions().setHasText("Features")).first().click();
		page.waitForLoadState(LoadState.NETWORKIDLE);
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
					String payload = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
					MqttInstruction instruction = new MqttInstruction(topic, payload);
					JuryScenario.this.deviceMessages.add(instruction);
					applyJuryBoxInstruction(instruction);
					JuryScenario.this.deviceMessages.notifyAll();
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

	private void applyJuryBoxInstruction(MqttInstruction instruction) {
		String[] topic = instruction.topic().split("/");
		if (topic.length != 4 || !"owlcms".equals(topic[0]) || !"fop".equals(topic[1])
		        || !this.options.fop().equals(topic[3])) {
			return;
		}
		if ("resetDecisions".equals(topic[2])) {
			Arrays.fill(this.juryBoxRefereeLights, DeviceLight.OFF);
			Arrays.fill(this.juryBoxJurorLights, DeviceLight.OFF);
			return;
		}
		String[] payload = instruction.payload().split("\\s+");
		if (payload.length < 2) {
			return;
		}
		int number;
		try {
			number = Integer.parseInt(payload[0]);
		} catch (NumberFormatException e) {
			return;
		}
		DeviceLight light = switch (payload[payload.length - 1]) {
			case "good" -> DeviceLight.WHITE;
			case "bad" -> DeviceLight.RED;
			case "hidden" -> DeviceLight.VOTED;
			default -> null;
		};
		if (light == null) {
			return;
		}
		if ("decision".equals(topic[2]) && number >= 1 && number <= this.juryBoxRefereeLights.length) {
			this.juryBoxRefereeLights[number - 1] = light;
		} else if ("juryMemberDecision".equals(topic[2]) && number >= 1
		        && number <= this.juryBoxJurorLights.length) {
			this.juryBoxJurorLights[number - 1] = light;
		}
	}

	private JuryBoxState juryBoxState() {
		return new JuryBoxState(List.copyOf(Arrays.asList(this.juryBoxRefereeLights.clone())),
		        List.copyOf(Arrays.asList(this.juryBoxJurorLights.clone())));
	}

	private RefBoxState refBoxState(List<MqttInstruction> instructions) {
		Boolean[] summoned = { false, false, false };
		for (MqttInstruction instruction : instructions) {
			String[] topic = instruction.topic().split("/");
			if (topic.length != 4 || !"owlcms".equals(topic[0]) || !"fop".equals(topic[1])
			        || !"summon".equals(topic[2]) || !this.options.fop().equals(topic[3])) {
				continue;
			}
			int referee = switch (instruction.payload()) {
				case "1" -> 1;
				case "2" -> 2;
				case "3" -> 3;
				default -> 0;
			};
			if (referee != 0) {
				summoned[referee - 1] = true;
			}
		}
		return new RefBoxState(List.copyOf(Arrays.asList(summoned)));
	}

	private static JuryBoxExpectation expectedJuryBox(String[] referees, String[] jurors, String description) {
		DeviceLight[] refereeLights = new DeviceLight[3];
		DeviceLight[] jurorLights = new DeviceLight[5];
		Arrays.fill(refereeLights, DeviceLight.OFF);
		Arrays.fill(jurorLights, DeviceLight.OFF);
		for (int i = 0; referees != null && i < Math.min(referees.length, refereeLights.length); i++) {
			refereeLights[i] = voteLight(referees[i]);
		}
		for (int i = 0; jurors != null && i < Math.min(jurors.length, jurorLights.length); i++) {
			jurorLights[i] = voteLight(jurors[i]);
		}
		return new JuryBoxExpectation(new JuryBoxState(List.copyOf(Arrays.asList(refereeLights)),
		        List.copyOf(Arrays.asList(jurorLights))), description);
	}

	private static JuryBoxExpectation juryBoxOff(String description) {
		return expectedJuryBox(null, null, description);
	}

	private static DeviceLight voteLight(String vote) {
		return "good".equals(vote) ? DeviceLight.WHITE : DeviceLight.RED;
	}

	private static String describeJuryBox(JuryBoxState state) {
		return "referees " + describeLights(state.referees()) + ", jury " + describeLights(state.jurors());
	}

	private static String describeLights(List<DeviceLight> lights) {
		StringBuilder sb = new StringBuilder();
		for (DeviceLight light : lights) {
			sb.append(switch (light) {
				case UNKNOWN -> "?";
				case OFF -> "-";
				case WHITE -> "W";
				case RED -> "R";
				case VOTED -> "V";
			});
		}
		return sb.toString();
	}

	private static DeviceLight[] unknownLights(int count) {
		DeviceLight[] lights = new DeviceLight[count];
		Arrays.fill(lights, DeviceLight.UNKNOWN);
		return lights;
	}

	private void publish(String topic, String payload) throws Exception {
		MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
		message.setQos(0);
		this.mqtt.publish(topic, message);
		action("MQTT " + topic + " " + payload);
		pause(250);
	}

	/**
	 * Observe a real countdown from the setup's 1:00, then stop and verify every public clock. The running clocks are
	 * all checked at 0:58; the next event the scenario schedules (the stop) comes two seconds later, at 0:56.
	 */
	private void athleteLifts() throws Exception {
		awaitClock(clockCheck("speaker", 60, false));
		clock("start");
		try {
			MqttInstruction started = awaitClockMessage("start");
			if (clockMessageMillis(started) != 60_000) {
				throw new IllegalStateException("clock must start from 60000 ms: " + started.display());
			}
			awaitRunningClocks();
		} finally {
			clock("stop");
		}
		MqttInstruction stopped = awaitClockMessage("stop");
		int remaining = clockMessageMillis(stopped);
		if (remaining <= 45_000 || remaining > 56_000) {
			throw new IllegalStateException("expected a real stop after displayed 0:56 (45000 < ms <= 56000): "
			        + stopped.display());
		}
		action("Server confirmed countdown from 60000 ms to " + remaining + " ms; no time was set after Start");
		int stoppedSeconds = (int) Math.ceil(remaining / 1000.0);
		String stoppedAt = String.format("%d:%02d", stoppedSeconds / 60, stoppedSeconds % 60);
		List<Check> checks = stoppedClockChecks(stoppedSeconds);
		for (Check check : checks) {
			awaitClock(check);
		}
		pause(1000);
		int failuresBefore = this.scenarioFailures;
		step("Clock reset to 1:00, counted down, then stopped at " + stoppedAt,
		        "The clock starts at 1:00, visibly runs down (0:58 on the Speaker, attempt board and scoreboard, "
		                + "then 0:56), and stops without resetting its time. OWLCMS confirms the remaining milliseconds "
		                + "over MQTT. Speaker, attempt board and both scoreboard clocks remain stopped at "
		                + stoppedAt + " after a further second. No decision yet.",
		        checks, juryBoxOff("all referee and jury lights are off after the new-clock reset"));
		if (this.scenarioFailures != failuresBefore) {
			throw new IllegalStateException("clock verification failed; referee decisions will not be sent");
		}
	}

	/**
	 * A clock started from 1:00 is seen running at 0:58 on every page. Returns two seconds later, at 0:56, so that the
	 * next event of the scenario is not scheduled within the same second.
	 */
	private void awaitRunningClocks() {
		for (String page : List.of("speaker", "attempt", "scoreboard")) {
			awaitClock(clockCheck(page, 58, true));
		}
		awaitClock(clockCheck("attempt", 56, true));
	}

	private static String clockTimers(String page) {
		return switch (page) {
			case "speaker" -> "[...document.querySelectorAll('passive-timer-element')]";
			case "attempt" -> "[document.querySelector('attempt-board-template')?.shadowRoot?.querySelector('#athleteTimer')]";
			case "scoreboard" -> "[...document.querySelector('results-template')?.shadowRoot"
			        + "?.querySelectorAll('timer-element#timer, timer-element#decisionSectionTimer') || []]";
			default -> throw new IllegalArgumentException("no athlete clock selector for page " + page);
		};
	}

	/** the athlete clock is visible and running, whatever its time: for steps, which take several seconds */
	private static Check runningClockCheck(String page) {
		String predicate = """
		        (() => {
		          const timers = %s;
		          return timers.length === %d && timers.every(t => t && t.checkVisibility()
		            && t.running === true && Number.isFinite(t.currentTime) && t.currentTime > 0
		            && t.currentTime < 59);
		        })()
		        """.formatted(clockTimers(page), "scoreboard".equals(page) ? 2 : 1);
		return new Check(page, ("scoreboard".equals(page) ? "both athlete clocks are " : "athlete clock is ")
		        + "running", predicate);
	}

	private static Check clockCheck(String page, int seconds, boolean running) {
		String timers = clockTimers(page);
		String display = String.format("%d:%02d", seconds / 60, seconds % 60);
		String predicate = """
		        (() => {
		          const timers = %s;
		          return timers.length === %d && timers.every(t => t && t.checkVisibility()
		            && t.running === %s && Number.isFinite(t.currentTime) && Math.ceil(t.currentTime) === %d
		            && t.shadowRoot?.querySelector('#timer')?.textContent.trim() === %s);
		        })()
		        """.formatted(timers, "scoreboard".equals(page) ? 2 : 1, running, seconds, js(display));
		return new Check(page, ("scoreboard".equals(page) ? "both athlete clocks are " : "athlete clock is ")
		        + (running ? "running at " : "stopped at ") + display, predicate);
	}

	private List<Check> stoppedClockChecks(int seconds) {
		List<Check> checks = new ArrayList<>();
		for (String page : List.of("speaker", "attempt", "scoreboard")) {
			checks.add(clockCheck(page, seconds, false));
			checks.add(new Check(page, "no rejected clock command notification",
			        "!(" + notificationContains("Cannot stop clock") + ") && !("
			                + notificationContains("Clock not started") + ") && !("
			                + notificationContains("jury has not finished voting") + ")"));
		}
		return checks;
	}

	private void awaitClock(Check check) {
		Page page = this.pages.get(check.page());
		try {
			page.waitForFunction("() => " + check.predicate(), null,
			        new Page.WaitForFunctionOptions().setTimeout(8_000));
		} catch (PlaywrightException e) {
			throw new IllegalStateException("expected " + check.page() + ": " + check.description() + "; timers: "
			        + page.evaluate(TIMER_STATE), e);
		}
		action("Verified " + check.page() + ": " + check.description());
	}

	/** every athlete or break timer of a page, for the message of a failed clock check */
	private static final String TIMER_STATE = """
	        () => {
	          const found = [];
	          const visit = root => {
	            for (const e of root.querySelectorAll('*')) {
	              if (e.localName === 'timer-element' || e.localName === 'passive-timer-element') {
	                found.push((e.id || e.localName) + ' visible=' + e.checkVisibility() + ' running=' + e.running
	                  + ' currentTime=' + e.currentTime + ' text='
	                  + (e.shadowRoot?.querySelector('#timer')?.textContent || '').trim());
	              }
	              if (e.shadowRoot) visit(e.shadowRoot);
	            }
	          };
	          visit(document);
	          return found.join('; ');
	        }
	        """;

	private MqttInstruction awaitClockMessage(String command) throws InterruptedException {
		String topic = "owlcms/fop/" + command + "/" + this.options.fop();
		long deadline = System.nanoTime() + 8_000_000_000L;
		synchronized (this.deviceMessages) {
			while (true) {
				for (MqttInstruction instruction : this.deviceMessages) {
					if (topic.equals(instruction.topic())) {
						action("Observed " + instruction.display());
						return instruction;
					}
				}
				long remaining = deadline - System.nanoTime();
				if (remaining <= 0) {
					throw new IllegalStateException("no clock acknowledgement received on " + topic);
				}
				this.deviceMessages.wait(Math.max(1, remaining / 1_000_000));
			}
		}
	}

	private static int clockMessageMillis(MqttInstruction instruction) {
		String payload = instruction.payload();
		return Integer.parseInt(payload.substring(payload.lastIndexOf(' ') + 1));
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

	private void triggerUnloadBeacons(Page settings) {
		List<Page> openPages = new ArrayList<>();
		openPages.add(settings);
		openPages.addAll(this.pages.values());
		Page waitPage = null;
		for (Page page : openPages) {
			if (page.isClosed()) {
				continue;
			}
			try {
				page.navigate("about:blank");
				waitPage = page;
			} catch (PlaywrightException e) {
				System.err.println("Could not navigate a scenario page to about:blank during cleanup: "
				        + e.getMessage());
			}
		}
		if (waitPage != null) {
			waitPage.waitForTimeout(1000);
		}
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

	private static String buttonEnabled(String text) {
		return "[...document.querySelectorAll('vaadin-button')].some(b => (b.offsetWidth || b.offsetHeight) && "
		        + "(b.innerText || '').includes(" + js(text) + ") && !b.hasAttribute('disabled'))";
	}

	/** Challenge is shown but disabled on Jury Control */
	private static Check challengeUnavailable(String why) {
		return new Check("jury", "Challenge is not available (" + why + ")", buttonDisabled("Challenge"));
	}

	/** Challenge is shown and enabled on Jury Control */
	private static Check challengeAvailable(String why) {
		return new Check("jury", "Challenge is available (" + why + ")", buttonEnabled("Challenge"));
	}

	private static Check reviewWeightCheck() {
		return new Check("attempt", "the attempted weight is visible in grey in its normal location",
		        """
		        (() => {
		          const board = document.querySelector('attempt-board-template');
		          const weight = board?.shadowRoot?.querySelector('[data-testid="attempt-board-weight"]');
		          return !!weight && weight.checkVisibility() && weight.classList.contains('reviewWeight')
		            && /^\\d+\\s*kg$/.test(weight.textContent.trim())
		            && getComputedStyle(weight).color === 'rgb(128, 128, 128)';
		        })()
		        """);
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

	private static String notificationContains(String text) {
		return "[...document.querySelectorAll('vaadin-notification-card')].some(n => "
		        + "(n.innerText || '').includes(" + js(text) + "))";
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

	/** screenshot and light-DOM state of every page */
	private void capture(int number, Map<String, String> shots, Map<String, String> observed) {
		for (Map.Entry<String, Page> entry : this.pages.entrySet()) {
			String file = String.format("%02d-%s.png", number, entry.getKey());
			entry.getValue().screenshot(new Page.ScreenshotOptions().setPath(this.reportDir.resolve(file)));
			shots.put(entry.getKey(), file);
			observed.put(entry.getKey(), String.valueOf(entry.getValue().evaluate(OBSERVE)));
		}
	}

	private void step(String title, String expected, List<Check> checks) throws IOException {
		step(title, expected, checks, null);
	}

	private void step(String title, String expected, List<Check> checks, JuryBoxExpectation juryBoxExpectation)
	        throws IOException {
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
		capture(number, shots, observed);
		List<String> actions = new ArrayList<>(this.pendingActions);
		List<MqttInstruction> mqttInstructions;
		JuryBoxState juryBox;
		synchronized (this.deviceMessages) {
			mqttInstructions = List.copyOf(this.deviceMessages);
			this.deviceMessages.clear();
			juryBox = juryBoxState();
		}
		List<String> instructions = mqttInstructions.stream().map(MqttInstruction::display).toList();
		RefBoxState refBox = refBoxState(mqttInstructions);
		if (!instructions.isEmpty()) {
			actions.add("MQTT owlcms to devices: " + String.join("; ", instructions));
		}
		if (juryBoxExpectation != null) {
			boolean ok = juryBoxExpectation.state().equals(juryBox);
			String result = (ok ? "PASS " : "FAIL ") + "jury box: " + juryBoxExpectation.description()
			        + "; actual " + describeJuryBox(juryBox);
			checkResults.add(result);
			System.out.println("     " + (ok ? "pass " : "FAIL ") + "jury box: "
			        + juryBoxExpectation.description() + "; actual " + describeJuryBox(juryBox));
			if (!ok) {
				this.failures++;
				this.scenarioFailures++;
			}
		}
		this.pendingActions = new ArrayList<>();
		this.results.add(new StepResult(number, title, actions, expected, checkResults, shots, observed, juryBox,
		        refBox));
	}

	private void writeReport() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>")
		        .append(esc(this.scenario.title())).append("</title><style>")
		        .append("body{font-family:sans-serif;margin:1em}h2{margin-top:2em}.shots{display:grid;")
		        .append("grid-template-columns:minmax(0,1fr) max-content repeat(3,minmax(0,1fr));gap:6px}")
		        .append(".shots figure{margin:0;min-width:0}.shots .device-stack{align-self:start;margin:0}")
		        .append(".shots img{width:100%;border:1px solid #888}")
		        .append(".pass{color:green}.fail{color:#c00;font-weight:bold}pre{white-space:pre-wrap;")
		        .append("background:#f4f4f4;padding:4px;font-size:80%}")
		        .append(JURY_BOX_CSS)
		        .append("</style></head><body>")
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
			r.shots().forEach((page, file) -> {
				if ("jury".equals(page)) {
					sb.append("<figure><figcaption>devices</figcaption>")
					        .append(deviceBoxesHtml(r.juryBox(), r.refBox())).append("</figure>");
				}
				sb.append("<figure><figcaption>").append(esc(page))
				        .append("</figcaption><a href='").append(file).append("'><img src='").append(file)
				        .append("'></a><pre>").append(esc(r.observed().get(page))).append("</pre></figure>");
			});
			sb.append("</div>");
		}
		sb.append("</body></html>");
		Files.writeString(this.reportDir.resolve("index.html"), sb.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * Compact view: one row per step, one column per screen, so the screens can be followed side by side as the
	 * events unfold. Click a screenshot for full size; arrow keys move between steps and screens.
	 */
	private static final String TIMELINE_COLUMN_CSS = "th label{cursor:pointer;white-space:nowrap}"
	        + "th.jury-screen,td.jury-screen{width:16.5%}"
	        + "th.off,td.off{width:1%}th.off{color:#888}td.off img,td.off .device-stack{display:none}";

	private static final String JURY_BOX_CSS = """
	        .device-stack{display:flex;flex-direction:column;gap:4px;margin:4px 0}
	        .jurybox,.refbox{background:#111;color:#ddd;border:1px solid #555;border-radius:5px;padding:5px;margin:0}
	        .jurybox-title{font-size:11px;font-weight:normal;color:#fff;margin-bottom:3px}
	        .light-row{display:flex;gap:5px;align-items:flex-end;margin:3px 0}
	        .light-row>span{font-size:10px;font-weight:normal}
	        .refbox .light-row{justify-content:center}
	        .light-group{display:flex;gap:3px}
	        .light-unit{text-align:center;font-size:9px;min-width:24px}
	        .light{box-sizing:border-box;width:22px;height:22px;border-radius:50%;border:2px solid #777;
	          display:flex;align-items:center;justify-content:center;color:#111;font-weight:bold;margin:auto}
	        .light.unknown{background:#555;color:#ddd}.light.off{background:#050505;color:#777}
	        .light.white{background:#fff;border-color:#ddd}.light.red{background:#f00;border-color:#d00}
	        .light.voted{background:#22c55e;border-color:#15803d}
	        .light.summoned{background:#1597ff;border-color:#0b69b0}
	        .light-label{margin-top:1px;color:#bbb}
	        """;

	/** Header checkboxes hide/show a screen column; every timeline opens with all columns shown. */
	private static final String TIMELINE_COLUMN_JS = "document.querySelectorAll('th input').forEach(cb=>{"
	        + "const col=cb.closest('th').cellIndex;const apply=()=>document.querySelectorAll('tr').forEach(tr=>{"
	        + "const c=tr.cells[col];if(c)c.classList.toggle('off',!cb.checked);});"
	        + "cb.checked=true;apply();cb.onchange=apply;});";

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
		        .append("#caption{color:#fff;padding:6px}")
		        .append(JURY_BOX_CSS)
		        .append(TIMELINE_COLUMN_CSS)
		        .append("</style></head><body><table><thead><tr><th>Step</th>");
		for (String n : pageNames) {
			if ("jury".equals(n)) {
				sb.append("<th><label class='jurybox-title'><input type='checkbox' checked> devices</label></th>");
			}
			sb.append("<th").append("jury".equals(n) ? " class='jury-screen'" : "")
			        .append("><label><input type='checkbox' checked> ").append(esc(n)).append("</label></th>");
		}
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
				if ("jury".equals(n)) {
					sb.append("<td class='jurybox-cell'>")
					        .append(deviceBoxesHtml(r.juryBox(), r.refBox()))
					        .append("</td>");
				}
				String file = r.shots().get(n);
				sb.append("<td").append("jury".equals(n) ? " class='jury-screen'" : "").append(">")
				        .append(file == null ? "" : "<img src='" + file + "' data-step='" + r.number()
				        + "' data-page='" + esc(n) + "' title='" + esc(r.observed().getOrDefault(n, "")) + "'>")
				        .append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</tbody></table><div id='viewer'><div id='caption'></div><img id='big'></div><script>")
		        .append("const imgs=[...document.querySelectorAll('td img')];const pages=")
		        .append(pageNames.size()).append(";let cur=-1;const v=document.getElementById('viewer');")
		        .append("function show(i,d){d=d||1;while(i>=0&&i<imgs.length&&!imgs[i].offsetParent)i+=d;")
		        .append("if(i<0||i>=imgs.length)return;cur=i;const im=imgs[i];")
		        .append("document.getElementById('big').src=im.src;document.getElementById('caption').textContent=")
		        .append("'Step '+im.dataset.step+' — '+im.dataset.page+'   (arrows: ←→ screen, ↑↓ step, Esc close)';")
		        .append("v.style.display='block';document.querySelectorAll('tr').forEach(t=>t.classList.remove('current'));")
		        .append("im.closest('tr').classList.add('current');im.closest('tr').scrollIntoView({block:'center'});}")
		        .append("imgs.forEach((im,i)=>im.onclick=()=>show(i));v.onclick=()=>v.style.display='none';")
		        .append("document.onkeydown=e=>{if(v.style.display!=='block')return;")
		        .append("if(e.key==='Escape')v.style.display='none';else if(e.key==='ArrowRight')show(cur+1);")
		        .append("else if(e.key==='ArrowLeft')show(cur-1,-1);else if(e.key==='ArrowDown')show(cur+pages,pages);")
		        .append("else if(e.key==='ArrowUp')show(cur-pages,-pages);else return;e.preventDefault();};")
		        .append(TIMELINE_COLUMN_JS)
		        .append("</script></body></html>");
		Files.writeString(this.reportDir.resolve("timeline.html"), sb.toString(), StandardCharsets.UTF_8);
	}

	private static String deviceBoxesHtml(JuryBoxState state, RefBoxState refBox) {
		StringBuilder sb = new StringBuilder(
		        "<div class='device-stack'><div class='jurybox'><div class='jurybox-title'>Jury box lights</div>");
		sb.append("<div class='light-row'><span>Referees</span><div class='light-group'>");
		String[] refereeLabels = { "L", "C", "R" };
		for (int i = 0; i < state.referees().size(); i++) {
			appendLight(sb, refereeLabels[i], state.referees().get(i));
		}
		sb.append("</div></div><div class='light-row'><span>Jury</span><div class='light-group'>");
		for (int i = 0; i < state.jurors().size(); i++) {
			appendLight(sb, "J" + (i + 1), state.jurors().get(i));
		}
		sb.append("</div></div></div><div class='refbox'><div class='jurybox-title'>Referee boxes</div>");
		sb.append("<div class='light-row'><div class='light-group'>");
		for (int i = 0; i < refBox.summoned().size(); i++) {
			appendRefBoxLight(sb, refereeLabels[i], refBox.summoned().get(i));
		}
		return sb.append("</div></div></div></div>").toString();
	}

	private static void appendRefBoxLight(StringBuilder sb, String label, boolean summoned) {
		sb.append("<div class='light-unit'><div class='light ").append(summoned ? "summoned" : "off")
		        .append("' title='").append(summoned ? "summoned" : "off")
		        .append("'></div><div class='light-label'>").append(label).append("</div></div>");
	}

	private static void appendLight(StringBuilder sb, String label, DeviceLight state) {
		String symbol = switch (state) {
			case UNKNOWN -> "?";
			case OFF -> "";
			case WHITE, RED -> "";
			case VOTED -> "✓";
		};
		sb.append("<div class='light-unit'><div class='light ").append(state.name().toLowerCase())
		        .append("' title='").append(state.name().toLowerCase()).append("'>").append(symbol)
		        .append("</div><div class='light-label'>").append(label).append("</div></div>");
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/** wait until the given time has elapsed since a reference instant ({@link System#nanoTime()}) */
	private static void pauseUntil(long sinceNanos, long millis) {
		long remaining = millis - (System.nanoTime() - sinceNanos) / 1_000_000;
		if (remaining > 0) {
			pause(remaining);
		}
	}

	private static void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
