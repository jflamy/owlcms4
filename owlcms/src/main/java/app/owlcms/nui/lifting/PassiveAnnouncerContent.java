package app.owlcms.nui.lifting;

import com.vaadin.flow.router.Route;

import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresAccess(value = { Access.SPEAKER, Access.ANNOUNCER }, platformBound = true)
@Route(value = "lifting/announcer/passive", layout = OwlcmsLayout.class)
public class PassiveAnnouncerContent extends AnnouncerContent {

	@Override
	public boolean isPassiveSpeaker() {
		return true;
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Announcer.PassiveTitle") + FieldOfPlay.getFopNameIfMultiple(getFop());
	}
}