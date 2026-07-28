/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import app.owlcms.nui.lifting.TimekeeperContent;
import app.owlcms.nui.referee.RefContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.OwlcmsLayout;

/**
 * Mobile-first launcher for referee, timekeeper, and jury devices.
 */
@SuppressWarnings("serial")
@Route(value = "refjury", layout = OwlcmsLayout.class)
public class RefereeHomeContent extends BaseNavigationContent implements HasDynamicTitle {

	private final List<Button> fopActions = new ArrayList<>();
	private ComboBox<FieldOfPlay> fopSelector;
	private boolean platformSelected;

	public RefereeHomeContent() {
		setMargin(false);
		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName("referee-jury-home");
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		RefereeJuryStyles.ensureLoaded(attachEvent.getUI());
		RefereeJuryStyles.applyLightTheme(attachEvent.getUI());
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
		fopActions.clear();

		fopSelector = createFopSelector();
		Div timekeeperActions = new Div();
		timekeeperActions.addClassNames("referee-jury-home-actions", "referee-jury-home-single-action",
		        "referee-jury-home-timekeeper");
		Div refereeActions = new Div();
		refereeActions.addClassNames("referee-jury-home-actions", "referee-jury-home-referees");

		Button timekeeper = createActionButton("Timekeeper", VaadinIcon.CLOCK,
		        () -> navigateTo(TimekeeperContent.class));
		timekeeper.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		timekeeperActions.add(timekeeper);
		for (int refereeNumber = 1; refereeNumber <= 3; refereeNumber++) {
			int selectedReferee = refereeNumber;
			refereeActions.add(createActionButton("Referee" + refereeNumber, VaadinIcon.GAVEL,
			        () -> navigateTo(RefContent.class, "num", Integer.toString(selectedReferee))));
		}

		Button juryButton = new Button(Translator.translate("Jury"), event -> navigateToJuryHome());
		juryButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		Button managementHome = new Button(Translator.translate("OWLCMS_Home"),
		        event -> UI.getCurrent().navigate(HomeNavigationContent.class));
		managementHome.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Div navigationActions = new Div(juryButton, managementHome);
		navigationActions.addClassName("referee-jury-home-navigation");

		add(fopSelector, refereeActions, timekeeperActions, navigationActions);

		fopSelector.addValueChangeListener(event -> updateActions(event.getValue()));
		FieldOfPlay fop = platformSelected ? getFop() : null;
		if (fop == null && OwlcmsFactory.getFOPs().size() == 1) {
			fop = OwlcmsFactory.getFOPs().iterator().next();
		}
		if (fop != null) {
			fopSelector.setValue(fop);
		} else {
			updateActions(null);
		}
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Referees");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Referees");
	}

	@Override
	public void setHeaderContent() {
		super.setHeaderContent();
		getRouterLayout().setMenuVisible(false);
		getRouterLayout().setDrawerOpened(false);
	}

	private Button createActionButton(String labelKey, VaadinIcon icon, Runnable action) {
		Button button = new Button(Translator.translate(labelKey), event -> action.run());
		button.setIcon(icon.create());
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
		button.setWidthFull();
		button.getStyle().set("min-height", "4.5rem");
		fopActions.add(button);
		return button;
	}

	private ComboBox<FieldOfPlay> createFopSelector() {
		ComboBox<FieldOfPlay> fopSelector = new ComboBox<>(Translator.translate("CompetitionPlatform"));
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		fopSelector.setItems(fops);
		fopSelector.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelector.setPlaceholder(Translator.translate("SelectPlatform"));
		fopSelector.setRequiredIndicatorVisible(fops.size() > 1);
		fopSelector.setWidthFull();
		fopSelector.addClassName("referee-jury-home-platform-selector");
		return fopSelector;
	}

	private void navigateTo(Class<? extends com.vaadin.flow.component.Component> target, String... additionalParameters) {
		FieldOfPlay fop = selectedFop();
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

	private void navigateToJuryHome() {
		FieldOfPlay fop = selectedFop();
		if (fop == null) {
			UI.getCurrent().navigate(JuryHomeContent.class);
		} else {
			navigateTo(JuryHomeContent.class);
		}
	}

	private FieldOfPlay selectedFop() {
		return fopSelector.getValue();
	}

	private void updateActions(FieldOfPlay fop) {
		boolean enabled = fop != null;
		for (Button action : fopActions) {
			action.setEnabled(enabled);
		}
	}
}