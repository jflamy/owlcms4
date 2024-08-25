package app.owlcms.apputils.queryparameters;

import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BaseContent extends VerticalLayout implements FOPParametersReader, SoundParameters {

	final static Logger logger = (Logger) LoggerFactory.getLogger(BaseContent.class);
	
	protected FieldOfPlay fop;
	private Group group;
	private Location location;
	private UI locationUI;
	private Map<String, List<String>> urlParameterMap;
	private QueryParameters defaultParameters;
	private String routeParameter;
	private boolean silenced;
	private boolean downSilenced;
	
	public BaseContent() {
		OwlcmsFactory.waitDBInitialized();
	}

	@Override
	public final QueryParameters getDefaultParameters() {
		return this.defaultParameters;
	}

	@Override
	final public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	final public Group getGroup() {
		return this.group;
	}

	@Override
	final public Location getLocation() {
		return this.location;
	}

	@Override
	final public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public final String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	final public Map<String, List<String>> getUrlParameterMap() {
		return this.urlParameterMap;
	}

	@Override
	public final boolean isDownSilenced() {
		return this.downSilenced;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public final boolean isSilenced() {
		return this.silenced;
	}

	@Override
	public final void setDefaultParameters(QueryParameters qp) {
		this.defaultParameters = qp;
	}

	@Override
	public final void setDownSilenced(boolean silent) {
		this.downSilenced = silent;
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	final public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	final public void setLocation(Location location) {
		this.location = location;

	}

	@Override
	final public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	@Override
	public final void setSilenced(boolean silent) {
		this.silenced = silent;
	}

	@Override
	final public void setUrlParameterMap(Map<String, List<String>> parametersMap) {
		this.urlParameterMap = removeDefaultValues(parametersMap);
	}
}
