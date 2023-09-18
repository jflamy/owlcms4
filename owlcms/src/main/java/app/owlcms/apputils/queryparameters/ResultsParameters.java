package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Location;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

public interface ResultsParameters extends FOPParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(ResultsParameters.class);
	final static String AGEGROUP = "ag";

	public default boolean isVideo(Location location) {
		return location.getPath().endsWith("video");
	}

	public void setAgeGroup(AgeGroup ag);
	public AgeGroup getAgeGroup();

	public void setCategory(Category cat);
	public Category getCategory();
	
	public void setAgeDivision(AgeDivision ad);
	public AgeDivision getAgeDivision();
	
	public void setAgeGroupPrefix(String agp);
	public String getAgeGroupPrefix();

}
