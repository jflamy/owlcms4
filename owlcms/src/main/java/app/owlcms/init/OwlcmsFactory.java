/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.ProxyAthleteTimer;
import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Singleton, one per running JVM (i.e. one instance of owlcms, or one unit test)
 *
 * This class allows a web session to locate the event bus on which information will be broacast. All web pages talk to one another via the event bus. The
 * {@link OwlcmsSession} class is used to remember the current field of play for the user.
 *
 * @author owlcms
 */
public class OwlcmsFactory {

	/** The fop by name. */
	private static Map<String, FieldOfPlay> fopByName = null;
	private static FieldOfPlay defaultFOP;
	private static final RequestGate requestGate = new RequestGate();
	private static EventBus appEventBus;
	private static volatile boolean importInProgress = false;
	final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsFactory.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public static void awaitLatch() throws InterruptedException {
		requestGate.await();
	}

	/** Startup has completed: requests are let through, unless an import is running. */
	public static void countDownLatch() throws InterruptedException {
		requestGate.startupDone();
	}

	public static EventBus getAppUIBus() {
		if (appEventBus == null) {
			appEventBus = new EventBus();
		}
		return appEventBus;
	}

	public static boolean isImportInProgress() {
		return importInProgress;
	}

	public static synchronized void setImportInProgress(boolean importInProgress) {
		if (importInProgress && !OwlcmsFactory.importInProgress) {
			// HTTP request processing blocks until the import is done. This is separate from the startup
			// condition: the end of an import that runs during startup does not let requests through.
			requestGate.importStarted();
		} else if (!importInProgress && OwlcmsFactory.importInProgress) {
			requestGate.importEnded();
		}
		OwlcmsFactory.importInProgress = importInProgress;
	}

	public static String getBuildTimestamp() {
		return StartupUtils.getBuildTimestamp();
	}

	/**
	 * @return first field of play, sorted alphabetically
	 */
	public static FieldOfPlay getDefaultFOP() {
		return defaultFOP;
	}

	public static FieldOfPlay getFOPByGroupName(String name) {
		if (getFopByName() == null) {
			return null; // no group is lifting yet.
		}
		Collection<FieldOfPlay> values = getFopByName().values();
		for (FieldOfPlay v : values) {
			if (v.getGroup().getName().equals(name)) {
				return v;
			}
		}
		return null;
	}

	public static Map<String, FieldOfPlay> getFopByName() {
		return fopByName;
	}

	/**
	 * Gets the FOP by name.
	 *
	 * @param key the key
	 * @return the FOP by name
	 */
	public static FieldOfPlay getFOPByName(String key) {
		return getFopByName().get(key);
	}

	public static void refreshActiveFOPGroup(Group refreshedGroup) {
		if (refreshedGroup == null || refreshedGroup.getId() == null || getFopByName() == null) {
			return;
		}
		for (FieldOfPlay fop : getFOPs()) {
			Group activeGroup = fop.getGroup();
			if (activeGroup != null && refreshedGroup.getId().equals(activeGroup.getId())) {
				refreshActiveFOPSafeSessionFields(activeGroup, refreshedGroup);
			}
		}
	}

	public static void refreshActiveFOPGroups() {
		if (getFopByName() == null) {
			return;
		}
		for (FieldOfPlay fop : getFOPs()) {
			Group activeGroup = fop.getGroup();
			if (activeGroup == null || activeGroup.getId() == null) {
				continue;
			}
			Group refreshedGroup = GroupRepository.getById(activeGroup.getId());
			if (refreshedGroup != null) {
				refreshActiveFOPSafeSessionFields(activeGroup, refreshedGroup);
			}
		}
	}

	private static void refreshActiveFOPSafeSessionFields(Group activeGroup, Group refreshedGroup) {
		activeGroup.setName(refreshedGroup.getName());
		activeGroup.setDescription(refreshedGroup.getDescription());
		activeGroup.setAnnouncer(refreshedGroup.getAnnouncer());
		activeGroup.setCompetitionDirector(refreshedGroup.getCompetitionDirector());
		activeGroup.setCompetitionSecretary(refreshedGroup.getCompetitionSecretary());
		activeGroup.setCompetitionSecretary2(refreshedGroup.getCompetitionSecretary2());
		activeGroup.setJury1(refreshedGroup.getJury1());
		activeGroup.setJury2(refreshedGroup.getJury2());
		activeGroup.setJury3(refreshedGroup.getJury3());
		activeGroup.setJury4(refreshedGroup.getJury4());
		activeGroup.setJury5(refreshedGroup.getJury5());
		activeGroup.setReserveJury(refreshedGroup.getReserveJury());
		activeGroup.setMarshall(refreshedGroup.getMarshall());
		activeGroup.setMarshal2(refreshedGroup.getMarshal2());
		activeGroup.setReferee1(refreshedGroup.getReferee1());
		activeGroup.setReferee2(refreshedGroup.getReferee2());
		activeGroup.setReferee3(refreshedGroup.getReferee3());
		activeGroup.setReserve(refreshedGroup.getReserve());
		activeGroup.setTechnicalController(refreshedGroup.getTechnicalController());
		activeGroup.setTechnicalController2(refreshedGroup.getTechnicalController2());
		activeGroup.setTechnicalController3(refreshedGroup.getTechnicalController3());
		activeGroup.setTimeKeeper(refreshedGroup.getTimeKeeper());
		activeGroup.setWeighIn1(refreshedGroup.getWeighIn1());
		activeGroup.setWeighIn2(refreshedGroup.getWeighIn2());
		activeGroup.setDoctor(refreshedGroup.getDoctor());
		activeGroup.setDoctor2(refreshedGroup.getDoctor2());
		activeGroup.setDoctor3(refreshedGroup.getDoctor3());
		activeGroup.setTis1(refreshedGroup.getTis1());
		activeGroup.setTis2(refreshedGroup.getTis2());
	}

	public static Collection<FieldOfPlay> getFOPs() {
		Collection<FieldOfPlay> values = getFopByName().values();
		return values;
	}

	/** The platform holding this session's athlete copies, including before the first clock start. */
	public static FieldOfPlay getFOPWithGroup(Group group) {
		if (group == null || group.getId() == null || getFopByName() == null) {
			return null;
		}
		for (FieldOfPlay fop : getFOPs()) {
			Group selected = fop.getGroup();
			if (selected != null && group.getId().equals(selected.getId())) {
				return fop;
			}
		}
		return null;
	}

	/**
	 * @return the platform on which the session is selected and in progress (see
	 *         {@link FieldOfPlay#isSessionInProgress()}), null if none. Athletes of such a session must not be edited
	 *         outside the platform, since the platform keeps its own copies of them. This is a status check: once the
	 *         session is unselected, editing is possible again.
	 */
	public static FieldOfPlay getFOPSessionInProgress(Group group) {
		if (group == null || group.getId() == null || getFopByName() == null) {
			return null;
		}
		for (FieldOfPlay fop : getFOPs()) {
			Group fopGroup = fop.getGroup();
			if (fopGroup != null && group.getId().equals(fopGroup.getId()) && fop.isSessionInProgress()) {
				return fop;
			}
		}
		return null;
	}

	public static FieldOfPlay getFOPSessionInProgress(Group proposed, Group stored) {
		FieldOfPlay fop = getFOPSessionInProgress(proposed);
		return fop != null ? fop : getFOPSessionInProgress(stored);
	}

	/** @return every platform whose selected session is in progress (see {@link FieldOfPlay#isSessionInProgress()}). */
	public static List<FieldOfPlay> getFOPsWithSessionInProgress() {
		if (getFopByName() == null) {
			return List.of();
		}
		return getFOPs().stream().filter(FieldOfPlay::isSessionInProgress).toList();
	}

	public static String getVersion() {
		return StartupUtils.getVersion();
	}

	/**
	 * @return first field of play, sorted alphabetically
	 */
	public static synchronized FieldOfPlay initDefaultFOP() {
		initFOPByName();
		setFirstFOPAsDefault();
		FieldOfPlay fop = getDefaultFOP();
		MQTTMonitor mm = fop.getMqttMonitor();
		if (mm != null) {
			mm.publishMqttConfig();
		}
		return fop;
	}

	public static synchronized void initFOPByName() {
		resetFOPByName();
		for (Platform platform : PlatformRepository.findAll()) {
			registerEmptyFOP(platform);
		}
		logger.trace("after initFOPByName {}", getFopByName() != null ? getFopByName().size() : null);
	}

	public static FieldOfPlay registerEmptyFOP(Platform platform) {
		String name = platform.getName();
		FieldOfPlay fop = new FieldOfPlay(null, platform);
		logger.info("{}Initialized", FieldOfPlay.getLoggingName(fop));
		// no group selected, no athletes, announcer will need to pick a group.
		fop.init(new LinkedList<>(), new ProxyAthleteTimer(fop), new ProxyBreakTimer(fop), true);
		getFopByName().put(name, fop);
		return fop;
	}

	public static void resetFOPByName() {
		if (getFopByName() != null) {
			for (Entry<String, FieldOfPlay> f : getFopByName().entrySet()) {
				FieldOfPlay fop = f.getValue();
				fop.unregister();
			}
		}
		setFopByName(new HashMap<>());
	}

	public static void setFirstFOPAsDefault() {
		Optional<FieldOfPlay> fop = getFopByName().entrySet().stream()
		        .sorted(Comparator.comparing(x -> x.getKey()))
		        .map(x -> x.getValue())
		        .findFirst();
		if (fop.isPresent()) {
			setDefaultFOP(fop.get());
		} else {
			Platform platform = new Platform(Translator.translate("Default"));
			PlatformRepository.save(platform);
			initDefaultFOP();
		}

	}

	public static FieldOfPlay unregisterFOP(Platform platform) {
		if (getFopByName() == null) {
			return null;
		}
		FieldOfPlay fop = null;
		String name = platform.getName();
		if (name == null) {
			throw new RuntimeException("can't happen, platform with no name");
		}
		try {
			fop = getFopByName().get(name);
			if (fop != null) {
				fop.getFopEventBus().unregister(fop);
			}
		} catch (IllegalArgumentException e) {
		}
		logger.trace("unregistering and unmapping fop {}", name);
		getFopByName().remove(name);
		return fop;
	}

	public static void waitDBInitialized() {
		try {
			requestGate.await();
		} catch (InterruptedException e) {
		}
	}

	static void setFopByName(Map<String, FieldOfPlay> fopByName) {
		OwlcmsFactory.fopByName = fopByName;
	}

	/**
	 * @param defaultFOP the defaultFOP to set
	 */
	private static void setDefaultFOP(FieldOfPlay defaultFOP) {
		OwlcmsFactory.defaultFOP = defaultFOP;
	}

}
