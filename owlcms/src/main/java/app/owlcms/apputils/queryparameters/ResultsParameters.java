package app.owlcms.apputils.queryparameters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

public interface ResultsParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(ResultsParameters.class);
	DecimalFormatSymbols symbolsEN_US = DecimalFormatSymbols.getInstance(Locale.US);
	DecimalFormat formatEN_US = new DecimalFormat("0.000", symbolsEN_US);

	public boolean isVideo();
	public void setVideo(boolean video);

	public void setAgeGroup(AgeGroup ag);
	public AgeGroup getAgeGroup();

	public void setCategory(Category cat);
	public Category getCategory();
	
	public void setAgeDivision(AgeDivision ad);
	public AgeDivision getAgeDivision();
	
	public void setAgeGroupPrefix(String agp);
	public String getAgeGroupPrefix();
}
