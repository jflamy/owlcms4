package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.SoundEntries;
import ch.qos.logback.classic.Logger;

public interface SoundParameters extends FOPParameters, SoundEntries {

	final Logger logger = (Logger) LoggerFactory.getLogger(SoundParameters.class);
	public static final String SILENT = "silent";
	public static final String SINGLEREF = "singleRef";
	public static final String IMMEDIATE = "immediate";
	public static final String DOWNSILENT = "downSilent";

	public default void buildDialog(Component target) {
	}

	public default boolean isDownSilenced() {
		return true;
	}

	public default boolean isSilenced() {
		return true;
	}

	public default boolean isSingleReferee() {
		return false;
	}

	public default void setDownSilenced(boolean silent) {
	}

	public void setSilenced(boolean silent);

	public default void setSingleReferee(boolean b) {
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		if (fop2 != null) {
			fop2.setSingleReferee(b);
		}
	}

}
