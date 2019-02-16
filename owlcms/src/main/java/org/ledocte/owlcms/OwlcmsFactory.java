package org.ledocte.owlcms;

import java.util.Map;

import org.ledocte.owlcms.data.platform.Platform;
import org.ledocte.owlcms.data.platform.PlatformRepository;
import org.ledocte.owlcms.state.FieldOfPlayState;

/**
 * Singleton, one per running JVM (i.e. one instance of owlcms, or one unit test)
 * 
 * This class allows a web session to locate the event bus on which information will be broacast.
 * All web pages talk to one another via the event bus.  The {@link OwlcmsSession} class is used
 * to remember the current field of play for the user.
 * 
 * @author owlcms
 */
public class OwlcmsFactory {
	
	static Map<String, FieldOfPlayState>fopByName = null;
	
	public static FieldOfPlayState getFOPByName(String key) {
		if (fopByName == null) {
			initFOPByName();
		}
		return fopByName.get(key);
	}

	private static void initFOPByName() {
		for (Platform platform : PlatformRepository.findAll()) {
			fopByName.put(platform.getName(), new FieldOfPlayState(null, platform));
		}
	}
	
	
}
