/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.referee.JuryKeypadContent;
import app.owlcms.nui.referee.JuryMobileContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.OwlcmsLayout;

/**
 * Selects a jury member device or the jury keypad for one field of play.
 */
@SuppressWarnings("serial")
@Route(value = "mobile/juryhome", layout = OwlcmsLayout.class)
public class JuryHomeContent extends BaseNavigationContent implements HasDynamicTitle {
	private boolean platformSelected;

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		RefereeJuryStyles.ensureLoaded(attachEvent.getUI());
		RefereeJuryStyles.applyLightTheme(attachEvent.getUI());
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Jury");
	}

	@Override
	public String getPageTitle() {
		FieldOfPlay fop = getFop();
		return Translator.translate("Jury") + FieldOfPlay.getFopNameIfMultiple(fop);
	}

	@Override
	public void setHeaderContent() {
		super.setHeaderContent();
		getRouterLayout().setMenuVisible(false);
		getRouterLayout().setDrawerOpened(false);
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		platformSelected = event.getLocation().getQueryParameters().getParameters().containsKey(FOPParameters.FOP)
		        || OwlcmsFactory.getFOPs().size() == 1;
		super.setParameter(event, parameter);
		buildContent();
	}

	private void buildContent() {
		removeAll();
		setMargin(false);
		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName("referee-jury-home");

		FieldOfPlay fop = platformSelected ? getFop() : null;
		ComboBox<FieldOfPlay> fopSelector = createFopSelector(fop);
		Div juryMemberActions = new Div();
		juryMemberActions.addClassNames("referee-jury-home-actions", "referee-jury-home-referees");
		Div juryPresidentActions = new Div();
		juryPresidentActions.addClassNames("referee-jury-home-actions", "referee-jury-home-single-action",
		        "referee-jury-home-president");
		int jurySize = jurySize(fop);
		if (jurySize > 0) {
			for (int juryMember = 1; juryMember <= jurySize; juryMember++) {
				int selectedJuryMember = juryMember;
				Button juryMemberButton = createActionButton("Jury" + juryMember, VaadinIcon.GAVEL,
				        () -> navigateTo(JuryMobileContent.class, "num", Integer.toString(selectedJuryMember)));
				juryMemberButton.setEnabled(platformSelected);
				juryMemberActions.add(juryMemberButton);
			}
			Button juryPresidentButton = createActionButton("JuryPresident", VaadinIcon.KEYBOARD,
			        () -> navigateTo(JuryKeypadContent.class));
			juryPresidentButton.setEnabled(platformSelected);
			juryPresidentActions.add(juryPresidentButton);
		}

		Button back = new Button(Translator.translate("Referees"),
		        event -> UI.getCurrent().navigate(RefereeHomeContent.class));
		back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button managementHome = new Button(Translator.translate("OWLCMS_Home"),
		        event -> UI.getCurrent().navigate(HomeNavigationContent.class));
		managementHome.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Div navigationActions = new Div(back, managementHome);
		navigationActions.addClassName("referee-jury-home-navigation");
		add(fopSelector, juryMemberActions, juryPresidentActions, navigationActions);
	}

	private ComboBox<FieldOfPlay> createFopSelector(FieldOfPlay selectedFop) {
		ComboBox<FieldOfPlay> fopSelector = new ComboBox<>(Translator.translate("CompetitionPlatform"));
		fopSelector.setItems(OwlcmsFactory.getFOPs());
		fopSelector.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelector.setPlaceholder(Translator.translate("SelectPlatform"));
		fopSelector.setValue(selectedFop);
		fopSelector.setWidthFull();
		fopSelector.addClassName("referee-jury-home-platform-selector");
		fopSelector.addValueChangeListener(event -> {
			if (event.isFromClient() && event.getValue() != null) {
				UI.getCurrent().navigate(JuryHomeContent.class,
				        QueryParameters.simple(Map.of("fop", event.getValue().getName())));
			}
		});
		return fopSelector;
	}

	private int jurySize(FieldOfPlay fop) {
		if (fop != null) {
			return fop.getJurySize();
		}
		return OwlcmsFactory.getFOPs().stream().mapToInt(FieldOfPlay::getJurySize).max().orElse(0);
	}

	private Button createActionButton(String labelKey, VaadinIcon icon, Runnable action) {
		Button button = new Button(Translator.translate(labelKey), event -> action.run());
		button.setIcon(icon.create());
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
		button.setWidthFull();
		button.getStyle().set("min-height", "4.5rem");
		return button;
	}

	private void navigateTo(Class<? extends com.vaadin.flow.component.Component> target, String... additionalParameters) {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			return;
		}
		Map<String, String> parameters = new java.util.HashMap<>();
		parameters.put("fop", fop.getName());
		for (int index = 0; index < additionalParameters.length; index += 2) {
			parameters.put(additionalParameters[index], additionalParameters[index + 1]);
		}
		UI.getCurrent().navigate(target, QueryParameters.simple(parameters));
	}
}