package app.owlcms.fieldofplay;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.uievents.CeremonyType;

public record CeremonyScope(CeremonyType type, Group session, Championship championship,
        AgeGroup ageGroup, Category category) {

	public boolean isMedals() {
		return this.type == CeremonyType.MEDALS;
	}
}