package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;

import ch.qos.logback.classic.Logger;

public interface ContentParameters extends FOPParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(ContentParameters.class);
	public static final String SILENT = "silent";
	public static final String SINGLEREF = "singleRef";
	public static final String IMMEDIATE = "immediate";
	public static final String DOWNSILENT = "downSilent";

	public default void buildDialog(Component target) {
	}

	public default QueryParameters getDefaultParameters() {
		return null;
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

	public default void setDefaultParameters(QueryParameters qp) {
	}

	public default void setDownSilenced(boolean silent) {
	}

	public void setSilenced(boolean silent);

	public default void setSingleReferee(boolean b) {
	}

}
