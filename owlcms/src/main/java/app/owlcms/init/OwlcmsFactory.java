/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
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
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.state.RelayTimer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Singleton, one per running JVM (i.e. one instance of owlcms, or one unit
 * test)
 * 
 * This class allows a web session to locate the event bus on which information
 * will be broacast. All web pages talk to one another via the event bus. The
 * {@link OwlcmsSession} class is used to remember the current field of play for
 * the user.
 * 
 * @author owlcms
 */
public class OwlcmsFactory {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsFactory.class);
	static {logger.setLevel(Level.INFO);}

	/** The fop by name. */
	static Map<String, FieldOfPlayState> fopByName = null;
	

	/**
	 * Gets the FOP by name.
	 *
	 * @param key the key
	 * @return the FOP by name
	 */
	public static FieldOfPlayState getFOPByName(String key) {
		if (fopByName == null) {
			initFOPByName();
		}
		return fopByName.get(key);
	}

	private static void initFOPByName() {
		fopByName = new HashMap<>();
		for (Platform platform : PlatformRepository.findAll()) {
			String name = platform.getName();
			FieldOfPlayState fop = new FieldOfPlayState(null, platform);
			logger.trace("fop {}",fop.getName());
			// no group selected, no athletes, announcer will need to pick a group.
			fop.init(new LinkedList<Athlete>(), new RelayTimer(fop));
			fopByName.put(name, fop);
		}
	}

	/**
	 * @return first field of play, sorted alphabetically
	 */
	public static FieldOfPlayState getDefaultFOP() {
		if (fopByName == null) {
			initFOPByName();
		}
		Optional<FieldOfPlayState> fop = fopByName.entrySet()
			.stream()
			.sorted(Comparator.comparing(x -> x.getKey()))
			.map(x -> x.getValue())
			.findFirst();
		return fop.orElseThrow(() -> new RuntimeException("no default platform"));
	}

	public static FieldOfPlayState getFOPByGroupName(String name) {
		if (fopByName == null) {
			return null; // no group is lifting yet.
		}
		Collection<FieldOfPlayState> values = fopByName.values();
		for (FieldOfPlayState v: values) {
			if (v.getGroup().getName().equals(name)) return v;
		}
		return null;
	}

	public static Collection<FieldOfPlayState> getFOPs() {
		if (fopByName == null) {
			initFOPByName();
		}
		Collection<FieldOfPlayState> values = fopByName.values();
		return values;
	}
}
