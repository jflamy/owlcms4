package app.owlcms.nui.lifting;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.contextmenu.SubMenu;

import app.owlcms.access.RequiresFeature;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresRole(value = Role.COMPETITION_DIRECTOR, platformBound = true)
@RequiresFeature(FeatureSwitch.COMPETITION_DIRECTOR_PAGE)
@Route(value = "lifting/competition-director", layout = OwlcmsLayout.class)
public class CompetitionDirectorContent extends AnnouncerContent {

	@Override
	protected void addSpeakerModeSwitch(SubMenu settings) {
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("CompetitionDirector") + FieldOfPlay.getFopNameIfMultiple(getFop());
	}
}