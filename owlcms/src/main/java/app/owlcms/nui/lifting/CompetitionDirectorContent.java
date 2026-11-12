package app.owlcms.nui.lifting;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresFeature;
import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.RegistrationTabPolicy.CardEntry;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.IwfComplianceDisplay;
import app.owlcms.nui.shared.NAthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresAccess(value = Access.SUPERVISOR, platformBound = true)
@RequiresFeature(FeatureSwitch.COMPETITION_DIRECTOR_PAGE)
@Route(value = "lifting/competition-director", layout = OwlcmsLayout.class)
public class CompetitionDirectorContent extends AnnouncerContent {

	@Override
	protected void addSpeakerModeSwitch(SubMenu settings) {
	}

	/**
	 * The supervisor corrects athletes through the registration form, with the card as a tab (first for athletes that
	 * have lifted). During the session the registration side is restricted to athlete information; the independent
	 * Card remains available.
	 */
	@Override
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		return new NAthleteRegistrationFormFactory(Athlete.class, null, null)
		        .setCardEntry(CardEntry.CARD_FIRST, this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		IwfComplianceDisplay.notifyIfNeeded();
	}

	@Override
	protected boolean isClockStartDecider() {
		return true;
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("CompetitionDirector") + FieldOfPlay.getFopNameIfMultiple(getFop());
	}
}