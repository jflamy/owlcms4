package app.owlcms.displays.top;

import app.owlcms.apputils.queryparameters.TopParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.displays.scoreboard.Results;

@SuppressWarnings("serial")
public class AbstractTop extends Results implements TopParameters {

	private AgeDivision ageDivision;
	private AgeGroup ageGroup;
	private String ageGroupPrefix;
	private Category category;

	@Override
	final public AgeDivision getAgeDivision() {
		return ageDivision;
	}

	@Override
	final public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	@Override
	final public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@Override
	final public Category getCategory() {
		return this.category;
	}

	@Override
	final public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	@Override
	final public void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
	}

	@Override
	final public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	@Override
	final public void setCategory(Category cat) {
		this.category = cat;
	}

}
