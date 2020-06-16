/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.init;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.ProxyAthleteTimer;
import app.owlcms.fieldofplay.ProxyBreakTimer;
import app.owlcms.uievents.UIEvent;
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

    final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsFactory.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /** The fop by name. */
    static Map<String, FieldOfPlay> fopByName = null;
    private static String version;
    private static String buildTimestamp;
    private static FieldOfPlay defaultFOP;

    public static String getBuildTimestamp() {
        return buildTimestamp;
    }

    /**
     * @return first field of play, sorted alphabetically
     */
    public static FieldOfPlay getDefaultFOP() {
        if (defaultFOP != null) {
            return defaultFOP;
        } else {
            if (fopByName == null) {
                initFOPByName();
            }
            Optional<FieldOfPlay> fop = fopByName.entrySet().stream().sorted(Comparator.comparing(x -> x.getKey()))
                    .map(x -> x.getValue())
                    .findFirst();
            defaultFOP = fop.orElse(null);
            // it is possible to have default FOP being null because getDefaultFop is called recursively
            // during the init of the FOPs. This is innocuous.
            if (defaultFOP != null) {
                // force a wake up on user interfaces
                defaultFOP.pushOut(new UIEvent.SwitchGroup(defaultFOP.getGroup(), defaultFOP.getState(),
                        defaultFOP.getCurAthlete(), null));
            }
            return defaultFOP;
        }

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
        if (fopByName == null) {
            initFOPByName();
        }
        return fopByName.get(key);
    }

    public static Collection<FieldOfPlay> getFOPs() {
        if (fopByName == null) {
            initFOPByName();
        }
        Collection<FieldOfPlay> values = fopByName.values();
        return values;
    }

    public static String getVersion() {
        return version;
    }

    public static void setBuildTimestamp(String sBuildTimestamp) {
        buildTimestamp = sBuildTimestamp;
    }

    public static void setVersion(String sVersion) {
        version = sVersion;
    }

    private static void initFOPByName() {
        fopByName = new HashMap<>();
        for (Platform platform : PlatformRepository.findAll()) {
            String name = platform.getName();
            FieldOfPlay fop = new FieldOfPlay(null, platform);
            logger.debug("fop {}", fop.getName());
            // no group selected, no athletes, announcer will need to pick a group.
            fop.init(new LinkedList<Athlete>(), new ProxyAthleteTimer(fop), new ProxyBreakTimer(fop), false);
            fopByName.put(name, fop);
        }
    }
}
