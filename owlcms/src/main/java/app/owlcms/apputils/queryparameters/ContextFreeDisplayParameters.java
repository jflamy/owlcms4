package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Location;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;

public interface ContextFreeDisplayParameters extends DisplayParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(ContextFreeDisplayParameters.class);
	final static String AGEGROUP = "ag";

	public default boolean isVideo(Location location) {
		return location.getPath().endsWith("video");
	}

	public void setAgeGroup(AgeGroup ag);

	public void setCategory(Category cat);

	@Override
	public void setFop(FieldOfPlay fop);

	@Override
	public void setGroup(Group group);

}
