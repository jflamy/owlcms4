/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.ProxyAthleteTimer;
import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Singleton, one per running JVM (i.e. one instance of owlcms, or one unit test)
 *
 * This class allows a web session to locate the event bus on which information will be broacast. All web pages talk to
 * one another via the event bus. The {@link OwlcmsSession} class is used to remember the current field of play for the
 * user.
 *
 * @author owlcms
 */
public class OwlcmsFactory {

    private static CountDownLatch latch = new CountDownLatch(1);

    final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsFactory.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /** The fop by name. */
    static Map<String, FieldOfPlay> fopByName = null;
    private static FieldOfPlay defaultFOP;

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
        if (fopByName == null) {
            return null; // no group is lifting yet.
        }
        Collection<FieldOfPlay> values = fopByName.values();
        for (FieldOfPlay v : values) {
            if (v.getGroup().getName().equals(name)) {
                return v;
            }
        }
        return null;
    }

    /**
     * Gets the FOP by name.
     *
     * @param key the key
     * @return the FOP by name
     */
    public static FieldOfPlay getFOPByName(String key) {
        return fopByName.get(key);
    }

    public static Collection<FieldOfPlay> getFOPs() {
        Collection<FieldOfPlay> values = fopByName.values();
        return values;
    }

    public static CountDownLatch getInitializationLatch() {
        return latch;
    }

    public static String getVersion() {
        return StartupUtils.getVersion();
    }

    /**
     * @return first field of play, sorted alphabetically
     */
    public static synchronized FieldOfPlay initDefaultFOP() {
        logger.trace("initDefaultFOP {} {}", fopByName != null ? fopByName.size() : null, LoggerUtils.stackTrace());
        initFOPByName();
        setFirstFOPAsDefault();
        return getDefaultFOP();
    }

    public static FieldOfPlay registerEmptyFOP(Platform platform) {
        String name = platform.getName();
        FieldOfPlay fop = new FieldOfPlay(null, platform);
        logger.trace("{} Initialized", fop.getLoggingName());
        // no group selected, no athletes, announcer will need to pick a group.
        fop.init(new LinkedList<Athlete>(), new ProxyAthleteTimer(fop), new ProxyBreakTimer(fop), true);
        fopByName.put(name, fop);
        return fop;
    }

    public static void unregisterFOP(Platform platform) {
        if (fopByName == null) {
            return;
        }
        String name = platform.getName();
        if (name == null) {
            throw new RuntimeException("can't happen, platform with no name");
        }
        try {
            FieldOfPlay fop = fopByName.get(name);
            fop.getFopEventBus().unregister(fop);
        } catch (IllegalArgumentException e) {
        }
        logger.trace("unregistering and unmapping fop {}", name);
        fopByName.remove(name);
    }

    public static void resetFOPByName() {
        if (fopByName != null) {
            Iterator<Entry<String, FieldOfPlay>> it = fopByName.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, FieldOfPlay> f = it.next();

                FieldOfPlay fop = f.getValue();
                EventBus fopEventBus = fop.getFopEventBus();
                if (fopEventBus != null) {
                    try {
                        fopEventBus.unregister(fop);
                    } catch (IllegalArgumentException e) {
                        // not registered, or already unregistered
                    }
                }
            }
        }
        fopByName = new HashMap<>();
        logger.trace("fopByName reset done.");
    }

    public static void waitDBInitialized() {
        try {
            OwlcmsFactory.getInitializationLatch().await();
        } catch (InterruptedException e) {
        }
    }

    public static void setFirstFOPAsDefault() {
        Optional<FieldOfPlay> fop = fopByName.entrySet().stream()
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

    private static synchronized void initFOPByName() {
        resetFOPByName();
        for (Platform platform : PlatformRepository.findAll()) {
            logger.trace("registering fop for {}", platform);
            registerEmptyFOP(platform);
        }
        logger.trace("after initFOPByName {}", fopByName != null ? fopByName.size() : null);
    }

    /**
     * @param defaultFOP the defaultFOP to set
     */
    private static void setDefaultFOP(FieldOfPlay defaultFOP) {
        OwlcmsFactory.defaultFOP = defaultFOP;
    }

}
