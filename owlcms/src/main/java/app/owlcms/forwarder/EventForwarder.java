/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.forwarder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class EventForwarder implements BreakDisplay {

    private static HashMap<String, EventForwarder> registeredFop = new HashMap<>();

    public static void listenToFOP(FieldOfPlay fop) {
        String fopName = fop.getName();
        if (registeredFop.get(fopName) == null) {
            registeredFop.put(fopName, new EventForwarder(fop));
        }
    }

    private EventBus postBus;
    private EventBus fopEventBus;
    private FieldOfPlay fop;
    private String categoryName;
    private List<Athlete> categoryRankings;
    private boolean wideTeamNames;
    private JsonValue leaders;
    private JsonValue groupAthletes;
    private JsonArray sattempts;
    private JsonArray cattempts;
    private Logger logger = (Logger) LoggerFactory.getLogger(EventForwarder.class);
    private String liftsDone;
    private String attempt;
    private String fullName;
    private String groupName;
    private boolean hidden;
    private Integer startNumber;
    private String teamName;
    private Integer weight;
    private JsonObject translationMap;
    private Integer timeAllowed;
    private int previousHashCode = 0;
    private long previousMillis = 0L;
    private boolean breakMode;

    private String updateKey;
    private String url;
    @SuppressWarnings("unused")
    private Boolean debugMode;

    public EventForwarder(FieldOfPlay emittingFop) {
        this.fop = emittingFop;

        fopEventBus = fop.getFopEventBus();
        fopEventBus.register(this);

        postBus = fop.getPostEventBus();
        postBus.register(this);

        setTranslationMap();

        updateKey = StartupUtils.getStringParam("updateKey");
        url = StartupUtils.getStringParam("remote");
        debugMode = StartupUtils.getBooleanParam("debug");
        if (url == null || updateKey == null || url.trim().isEmpty() || updateKey.trim().isEmpty()) {
            logger.info("Pushing results to remote site not enabled.");
        } else {
            logger.info("Pushing to remote site {}", url);
        }
        pushToRemote();
    }

    @Override
    public void doBreak() {
        OwlcmsSession.withFop(fop -> {
            BreakType breakType = fop.getBreakType();
            Group group = fop.getGroup();
            setFullName((group != null ? (Translator.translate("Group_number", group.getName()) + " &ndash; ") : "")
                    + inferMessage(breakType));
            setTeamName("");
            setAttempt("");
            setBreak(true);
            setHidden(false);
        });
    }

    public Integer getTimeAllowed() {
        return timeAllowed;
    }

    public JsonObject getTranslationMap() {
        return translationMap;
    }

    /**
     * Change the messages because we are not showing live timers
     *
     * @see app.owlcms.displays.attemptboard.BreakDisplay#inferMessage(app.owlcms.fieldofplay.BreakType)
     */
    @Override
    public String inferMessage(BreakType bt) {
        if (bt == null) {
            return Translator.translate("BreakType.TECHNICAL");
        }
        switch (bt) {
        case FIRST_CJ:
            return Translator.translate("BreakType.FIRST_CJ");
        case FIRST_SNATCH:
            return Translator.translate("BreakType.FIRST_SNATCH");
        case BEFORE_INTRODUCTION:
            return Translator.translate("BreakType.BEFORE_INTRODUCTION");
        case DURING_INTRODUCTION:
            return Translator.translate("BreakType.DURING_INTRODUCTION");
        case TECHNICAL:
            return Translator.translate("BreakType.TECHNICAL");
        case JURY:
            return Translator.translate("BreakType.JURY");
        default:
            return "";
        }
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        Athlete a = e.getAthlete();
        setHidden(false);
        doUpdate(a, e);
    }

    @Subscribe
    public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
        Competition competition = Competition.getCurrent();
        computeLeaders(competition);
        computeCurrentGroup(competition);
        pushToRemote();
    }

    @Subscribe
    public void slaveGroupDone(UIEvent.GroupDone e) {
        Group g = e.getGroup();
        if (g == null) {
            setHidden(true);
        } else {
            // done is a special kind of break.
            doBreak();
            setFullName(Translator.translate("Group_number_results", g.toString()));
            pushToRemote();
        }
    }

    @Subscribe
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        Athlete a = e.getAthlete();
        Competition competition = Competition.getCurrent();
        computeCurrentGroup(competition);
        doUpdate(a, e);
        pushToRemote();
    }

    @Subscribe
    public void slaveStartBreak(UIEvent.BreakStarted e) {
        setHidden(false);
        doBreak();
        pushToRemote();
    }

    @Subscribe
    public void slaveStartLifting(UIEvent.StartLifting e) {
        setHidden(false);
        pushToRemote();
    }

    @Subscribe
    public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
        Competition competition = Competition.getCurrent();
        computeLeaders(competition);
        computeCurrentGroup(competition);
        switch (e.getState()) {
        case INACTIVE:
            setHidden(true);
            break;
        case BREAK:
            if (e.getAthlete() == null) {
                setHidden(true);
            } else {
                doUpdate(e.getAthlete(), e);
                doBreak();
            }
            break;
        default:
            doUpdate(e.getAthlete(), e);
        }
        pushToRemote();
    }

    protected void setTranslationMap() {
        JsonObject translations = Json.createObject();
        Enumeration<String> keys = Translator.getKeys();
        while (keys.hasMoreElements()) {
            String curKey = keys.nextElement();
            if (curKey.startsWith("Scoreboard.")) {
                translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
            }
        }
        setTranslationMap(translations);
    }

    void setAttempt(String formattedAttempt) {
        this.attempt = formattedAttempt;
    }

    void setFullName(String fullName) {
        this.fullName = fullName;
    }

    void setGroupName(String name) {
        this.groupName = name;
    }

    void setHidden(boolean b) {
        this.hidden = b;
    }

    void setLiftsDone(String formattedDone) {
        this.liftsDone = formattedDone;
    }

    void setStartNumber(Integer integer) {
        this.startNumber = integer;
    }

    void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    void setWeight(Integer weight) {
        this.weight = weight;
    }

    private void computeCurrentGroup(Competition competition) {
        List<Athlete> globalRankingsForCurrentGroup = competition.getGlobalCategoryRankingsForGroup(fop.getGroup());
        int liftsDone = AthleteSorter.countLiftsDone(globalRankingsForCurrentGroup);
        setLiftsDone(Translator.translate("Scoreboard.AttemptsDone", liftsDone));
        if (globalRankingsForCurrentGroup != null && globalRankingsForCurrentGroup.size() > 0) {
            setGroupAthletes(getAthletesJson(globalRankingsForCurrentGroup, fop.getLiftingOrder()));
        } else {
            setGroupAthletes(null);
        }
    }

    private void computeLeaders(Competition competition) {
        logger.debug("computeLeaders");
        OwlcmsSession.withFop(fop -> {
            Athlete curAthlete = fop.getCurAthlete();
            if (curAthlete != null && curAthlete.getGender() != null) {
                setCategoryName(curAthlete.getCategory().getName());

                categoryRankings = competition.getGlobalTotalRanking(curAthlete.getGender());
                // logger.debug("rankings for current gender {}
                // size={}",curAthlete.getGender(),globalRankingsForCurrentGroup.size());
                categoryRankings = filterToCategory(curAthlete.getCategory(), categoryRankings);
                int size = categoryRankings.size();
                // logger.debug("rankings for current category {}
                // size={}",curAthlete.getCategory(),globalRankingsForCurrentGroup.size());
                categoryRankings = categoryRankings.stream().filter(a -> a.getTotal() > 0).limit(3)
                        .collect(Collectors.toList());

                if (size > 15) {
                    setLeaders(null);
                } else if (categoryRankings.size() > 0) {
                    // null as second argument because we do not highlight current athletes in the leaderboard
                    setLeaders(getAthletesJson(categoryRankings, null));
                } else {
                    // no one has totaled, so we show the snatch leaders
                    if (!fop.isCjStarted()) {
                        categoryRankings = Competition.getCurrent()
                                .getGlobalSnatchRanking(curAthlete.getGender());
                        categoryRankings = filterToCategory(curAthlete.getCategory(),
                                categoryRankings);
                        categoryRankings = categoryRankings.stream()
                                .filter(a -> a.getSnatchTotal() > 0).limit(3).collect(Collectors.toList());
                        if (categoryRankings.size() > 0) {
                            setLeaders(getAthletesJson(categoryRankings, null));
                        } else {
                            // nothing to show
                            setLeaders(null);
                        }
                    } else {
                        // nothing to show
                        setLeaders(null);
                    }
                }
            }
        });

    }

    private Map<String, String> createUpdate() {
        Map<String, String> sb = new HashMap<>();
        mapPut(sb, "updateKey", updateKey);

        // competition state
        mapPut(sb, "competitionName", Competition.getCurrent().getCompetitionName());
        mapPut(sb, "fop", fop.getName());
        FOPState state = fop.getState();
        mapPut(sb, "fopState", state != null ? state.toString() : FOPState.INACTIVE.name());
        mapPut(sb, "break", String.valueOf(breakMode));

        // current athlete & attempt
        mapPut(sb, "startNumber", startNumber != null ? startNumber.toString() : null);
        mapPut(sb, "categoryName", categoryName);
        mapPut(sb, "fullName", fullName);
        mapPut(sb, "teamName", teamName);
        mapPut(sb, "attempt", attempt);
        mapPut(sb, "weight", weight != null ? weight.toString() : null);
        mapPut(sb, "timeAllowed", timeAllowed != null ? timeAllowed.toString() : null);

        // current group
        mapPut(sb, "groupName", groupName);
        mapPut(sb, "liftsDone", liftsDone);

        // bottom tables
        if (groupAthletes != null) {
            mapPut(sb, "groupAthletes", groupAthletes.toJson());
        }
        if (leaders != null) {
            mapPut(sb, "leaders", leaders.toJson());
        }

        // presentation information
        mapPut(sb, "translationMap", translationMap.toJson());
        mapPut(sb, "hidden", String.valueOf(hidden));
        mapPut(sb, "wideTeamNames", String.valueOf(wideTeamNames));

        return sb;
    }

    private void doDone(Group g) {
        logger.debug("forwarding doDone {}", g == null ? null : g.getName());
        if (g == null) {
            setHidden(true);
        } else {
            setFullName(Translator.translate("Group_number_results", g.toString()));
            setGroupName("");
            setLiftsDone("");
        }
    }

    private void doUpdate(Athlete a, UIEvent e) {
        logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
        boolean leaveTopAlone = false;
        if (e instanceof UIEvent.LiftingOrderUpdated) {
            LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
            if (e2.isInBreak()) {
                leaveTopAlone = !e2.isDisplayToggle();
            } else {
                leaveTopAlone = !e2.isCurrentDisplayAffected();
            }
        }
        if (a != null && a.getAttemptsDone() < 6) {
            if (!leaveTopAlone) {
                logger.debug("updating top {}", a.getFullName());
                setFullName(a.getFullName());
                setTeamName(a.getTeam());
                setStartNumber(a.getStartNumber());
                String formattedAttempt = formatAttempt(a.getAttemptsDone());
                setAttempt(formattedAttempt);
                setWeight(a.getNextAttemptRequestedWeight());
                if (e instanceof UIEvent.LiftingOrderUpdated) {
                    setTimeAllowed(((LiftingOrderUpdated) e).getTimeAllowed());
                }
                String computedName = fop.getGroup() != null
                        ? Translator.translate("Scoreboard.GroupLiftType", fop.getGroup().getName(),
                                (a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
                                        : Translator.translate("Snatch")))
                        : "";
                setGroupName(computedName);
            }
        } else {
            if (!leaveTopAlone) {
                logger.debug("ef doUpdate doDone");
                Group g = (a != null ? a.getGroup() : null);
                doDone(g);
            }
            return;
        }
    }

    private List<Athlete> filterToCategory(Category category, List<Athlete> order) {
        return order
                .stream()
                .filter(a -> category != null && category.equals(a.getCategory()))
                .limit(3)
                .collect(Collectors.toList());
    }

    private String formatAttempt(Integer attemptNo) {
        String translate = Translator.translate("AttemptBoard_attempt_number", (attemptNo % 3) + 1);
        return translate;
    }

    private String formatInt(Integer total) {
        if (total == null || total == 0) {
            return "-";
        } else if (total == -1) {
            return "inv.";// invited lifter, not eligible.
        } else if (total < 0) {
            return "(" + Math.abs(total) + ")";
        } else {
            return total.toString();
        }
    }

    private String formatKg(String total) {
        return (total == null || total.trim().isEmpty()) ? "-"
                : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
    }

    private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
        String category;
        category = curCat != null ? curCat.getName() : "";
        ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
        ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
        ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
        Integer startNumber = a.getStartNumber();
        ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
        ja.put("category", category != null ? category : "");
        getAttemptsJson(a, liftOrderRank);
        ja.put("sattempts", sattempts);
        ja.put("cattempts", cattempts);
        ja.put("total", formatInt(a.getTotal()));
        ja.put("snatchRank", formatInt(a.getSnatchRank()));
        ja.put("cleanJerkRank", formatInt(a.getCleanJerkRank()));
        ja.put("totalRank", formatInt(a.getTotalRank()));
        ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
        boolean notDone = a.getAttemptsDone() < 6;
        String blink = (notDone ? " blink" : "");
        if (notDone) {
            ja.put("classname", (liftOrderRank == 1 ? "current" + blink : (liftOrderRank == 2) ? "next" : ""));
        }
    }

    /**
     * @param groupAthletes, List<Athlete> liftOrder
     * @return
     */
    private JsonValue getAthletesJson(List<Athlete> groupAthletes, List<Athlete> liftOrder) {
        JsonArray jath = Json.createArray();
        int athx = 0;
        Category prevCat = null;
        long currentId = (liftOrder != null && liftOrder.size() > 0) ? liftOrder.get(0).getId() : -1L;
        long nextId = (liftOrder != null && liftOrder.size() > 1) ? liftOrder.get(1).getId() : -1L;
        List<Athlete> athletes = groupAthletes != null ? Collections.unmodifiableList(groupAthletes)
                : Collections.emptyList();
        for (Athlete a : athletes) {
            JsonObject ja = Json.createObject();
            Category curCat = a.getCategory();
            if (curCat != null && !curCat.equals(prevCat)) {
                // changing categories, put marker before athlete
                ja.put("isSpacer", true);
                jath.set(athx, ja);
                ja = Json.createObject();
                prevCat = curCat;
                athx++;
            }
            getAthleteJson(a, ja, curCat, (a.getId() == currentId)
                    ? 1
                    : ((a.getId() == nextId)
                            ? 2
                            : 0));
            String team = a.getTeam();
            if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
                logger.trace("long team {}", team);
                setWideTeamNames(true);
            }
            jath.set(athx, ja);
            athx++;
        }
        return jath;
    }

    /**
     * Compute Json string ready to be used by web component template
     *
     * CSS classes are pre-computed and passed along with the values; weights are formatted.
     *
     * @param a
     * @param liftOrderRank2
     * @return json string with nested attempts values
     */
    private void getAttemptsJson(Athlete a, int liftOrderRank) {
        sattempts = Json.createArray();
        cattempts = Json.createArray();
        XAthlete x = new XAthlete(a);
        Integer curLift = x.getAttemptsDone();
        int ix = 0;
        for (LiftInfo i : x.getRequestInfoArray()) {
            JsonObject jri = Json.createObject();
            String stringValue = i.getStringValue();
            boolean notDone = x.getAttemptsDone() < 6;
            String blink = (notDone ? " blink" : "");

            jri.put("goodBadClassName", "narrow empty");
            jri.put("stringValue", "");
            if (i.getChangeNo() >= 0) {
                String trim = stringValue != null ? stringValue.trim() : "";
                switch (Changes.values()[i.getChangeNo()]) {
                case ACTUAL:
                    if (!trim.isEmpty()) {
                        if (trim.contentEquals("-") || trim.contentEquals("0")) {
                            jri.put("goodBadClassName", "narrow fail");
                            jri.put("stringValue", "-");
                        } else {
                            boolean failed = stringValue.startsWith("-");
                            jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
                            jri.put("stringValue", formatKg(stringValue));
                        }
                    }
                    break;
                default:
                    if (stringValue != null && !trim.isEmpty()) {
                        String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
                                : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
                        jri.put("goodBadClassName", "narrow request");
                        if (notDone) {
                            jri.put("className", highlight);
                        }
                        jri.put("stringValue", stringValue);
                    }
                    break;
                }
            }

            if (ix < 3) {
                sattempts.set(ix, jri);
            } else {
                cattempts.set(ix % 3, jri);
            }
            ix++;
        }
    }

//    private void javaNetHttpPost() {
//        // url = "https://httpbin.org/post";
//        HttpURLConnection con = null;
//        if (url == null) {
//            return;
//        }
//        try {
//            Map<String, String> updateString = createUpdate();
//            long deltaMillis = System.currentTimeMillis() - previousMillis;
//            int hashCode = updateString.hashCode();
//            // debounce, sometimes several identical updates in a rapid succession
//            // identical updates are ok after 1 sec.
//            if (hashCode != previousHashCode || (deltaMillis > 1000)) {
//                con = sendUpdate(updateString);
//                previousHashCode = hashCode;
//                previousMillis = System.currentTimeMillis();
//            }
//            // contrary to documentation con can be null with no exception thrown.
//            if (con != null) {
//                readResponse(con);
//            }
//        } finally {
//            if (con != null) {
//                con.disconnect();
//            }
//        }
//    }

    private void mapPut(Map<String, String> wr, String key, String value) {
        if (value == null) {
            return;
        }
        wr.put(key, value);
    }

    private void pushToRemote() {
        if (url == null) {
            return;
        }
        try {
            sendPost(url, createUpdate());
        } catch (IOException e) {
            logger./**/warn("cannot push: {} {}", url, e.getMessage());
        }
        // javaNetHttpPost();
    }

//    private String readResponse(HttpURLConnection con) {
//        StringBuilder content;
//        try {
//            InputStream inputStream = con.getInputStream();
//            if (inputStream == null) {
//                return "";
//            }
//
//            try (BufferedReader br = new BufferedReader(
//                    new InputStreamReader(inputStream))) {
//                String line;
//                content = new StringBuilder();
//                while ((line = br.readLine()) != null) {
//                    content.append(line);
//                    content.append(System.lineSeparator());
//                }
//            }
//            return content.toString();
//        } catch (Exception e) {
//            // logger.error("{} {}", url, e.getCause() != null ? e.getCause().getMessage() : e);
//            return null;
//        }
//    }

    private void sendPost(String url, Map<String, String> parameters) throws IOException {
        HttpPost post = new HttpPost(url);

        long deltaMillis = System.currentTimeMillis() - previousMillis;
        int hashCode = parameters.hashCode();
        // debounce, sometimes several identical updates in a rapid succession
        // identical updates are ok after 1 sec.
        if (hashCode != previousHashCode || (deltaMillis > 1000)) {
            // add request parameters or form parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            parameters.entrySet().stream()
                    .forEach((e) -> urlParameters.add(new BasicNameValuePair(e.getKey(), e.getValue())));

            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                    CloseableHttpResponse response = httpClient.execute(post)) {
                StatusLine statusLine = response.getStatusLine();
                Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
                if (statusCode != null && statusCode != 200) {
                    logger.error("could not post {} ", statusLine);
                }
                EntityUtils.toString(response.getEntity());
            }

            previousHashCode = hashCode;
            previousMillis = System.currentTimeMillis();
        }

    }

//    private HttpURLConnection sendUpdate(Map<String, String> parameters) {
//        HttpURLConnection con = null;
//        try {
//            URL myurl = new URL(url);
//            con = (HttpURLConnection) myurl.openConnection();
//            con.setDoOutput(true);
//            con.setRequestMethod("POST");
//            con.setRequestProperty("User-Agent", "Java client");
//            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//
//            int count = 0;
//            // connection is opened by getOutputStream
//            try (OutputStream outputStream = con.getOutputStream();
//                    OutputStreamWriter wr = new OutputStreamWriter(outputStream)) {
//                for (Entry<String, String> pair : parameters.entrySet()) {
//                    wr.write(URLUtils.urlEncode(pair.getKey()));
//                    wr.write("=");
//                    wr.write(URLUtils.urlEncode(pair.getValue()));
//                    if (debugMode) {
//                        logger./**/warn("{}={}", pair.getKey(), pair.getValue());
//                    }
//                    if (count < parameters.size() - 1) {
//                        wr.write("&");
//                        if (debugMode) {
//                            logger./**/warn("&");
//                        }
//                        count++;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger./**/warn("cannot push: {} {}", url, e.getMessage());
//        }
//        return con;
//
//    }

    private void setBreak(boolean b) {
        this.breakMode = b;
    }

    private void setCategoryName(String name) {
        this.categoryName = name;
    }

    private void setGroupAthletes(JsonValue athletesJson) {
        this.groupAthletes = athletesJson;

    }

    private void setLeaders(JsonValue athletesJson) {
        this.leaders = athletesJson;
    }

    private void setTimeAllowed(Integer timeAllowed) {
        this.timeAllowed = timeAllowed;
    }

    private void setTranslationMap(JsonObject translations) {
        this.translationMap = translations;
    }

    private void setWideTeamNames(boolean b) {
        wideTeamNames = b;
    }

}
