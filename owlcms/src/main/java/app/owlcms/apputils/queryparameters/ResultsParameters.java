/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

public interface ResultsParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(ResultsParameters.class);
	DecimalFormatSymbols symbolsEN_US = DecimalFormatSymbols.getInstance(Locale.US);
	DecimalFormat formatEN_US = new DecimalFormat("0.000", symbolsEN_US);

	public AgeGroup getAgeGroup();

	public String getAgeGroupPrefix();

	public Category getCategory();

	public Championship getChampionship();

	public boolean isVideo();

	public void setAgeGroup(AgeGroup ag);

	public void setAgeGroupPrefix(String agp);

	public void setCategory(Category cat);

	public void setChampionship(Championship ad);

	public void setVideo(boolean video);
	
	public default void setGender(Gender gender) {}
	
	public default Gender getGender() {return null;}
	
	public default boolean isDisplayLifts() { return false; }
	public default void setDisplayLifts(boolean displayLifts) {}
}
