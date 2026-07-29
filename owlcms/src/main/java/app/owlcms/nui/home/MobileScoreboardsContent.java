/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
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
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.scoreboards.WarmupLiftingOrderPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.WarmupScoreboardPage;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.OwlcmsLayout;

/**
 * Mobile/tablet launcher for warmup scoreboards and attempt/decision boards.
 */
@SuppressWarnings("serial")
@Route(value = "mobile/scoreboards", layout = OwlcmsLayout.class)
public class MobileScoreboardsContent extends BaseNavigationContent implements HasDynamicTitle {

	private final List<Button> displayActions = new ArrayList<>();
	private boolean platformSelected;

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
		displayActions.clear();
		setMargin(false);
		setPadding(false);
		setSpacing(true);
		setWidthFull();
		addClassName("referee-jury-home");

		FieldOfPlay selectedFop = platformSelected ? getFop() : null;
		ComboBox<FieldOfPlay> fopSelector = createFopSelector(selectedFop);

		Div scoreboardActions = new Div();
		scoreboardActions.addClassNames("referee-jury-home-actions", "mobile-scoreboards-group");
		Button simple = createDisplayButton("Scoreboard", VaadinIcon.LIST_OL, WarmupNoLeadersPage.class);
		Button leaders = createDisplayButton("ScoreboardWLeadersButton", VaadinIcon.TROPHY,
		        WarmupScoreboardPage.class);
		leaders.addClassName("mobile-scoreboards-leaders");
		Button liftingOrder = createDisplayButton("Scoreboard.LiftingOrder", VaadinIcon.SORT,
		        WarmupLiftingOrderPage.class);
		scoreboardActions.add(simple, leaders, liftingOrder);

		Div boardActions = new Div();
		boardActions.addClassNames("referee-jury-home-actions", "mobile-scoreboards-group");
		boardActions.add(
		        createDisplayButton("AttemptBoard", VaadinIcon.SCALE, PublicFacingAttemptBoardPage.class),
		        createDisplayButton("Athlete_Decisions", VaadinIcon.CHECK_SQUARE_O,
		                AthleteFacingDecisionBoardPage.class));

		Button back = new Button(Translator.translate("Referees"),
		        event -> navigateToReferees());
		back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Button managementHome = new Button(Translator.translate("OWLCMS_Home"),
		        event -> UI.getCurrent().navigate(HomeNavigationContent.class));
		managementHome.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		Div navigationActions = new Div(back, managementHome);
		navigationActions.addClassName("referee-jury-home-navigation");

		add(fopSelector, scoreboardActions, boardActions, navigationActions);

		FieldOfPlay fop = selectedFop;
		if (fop == null && OwlcmsFactory.getFOPs().size() == 1) {
			fop = OwlcmsFactory.getFOPs().iterator().next();
		}
		if (fop != null) {
			fopSelector.setValue(fop);
		} else {
			updateActions(false);
		}
	}

	private ComboBox<FieldOfPlay> createFopSelector(FieldOfPlay selectedFop) {
		ComboBox<FieldOfPlay> fopSelector = new ComboBox<>(Translator.translate("CompetitionPlatform"));
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		fopSelector.setItems(fops);
		fopSelector.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelector.setPlaceholder(Translator.translate("SelectPlatform"));
		fopSelector.setRequiredIndicatorVisible(fops.size() > 1);
		fopSelector.setValue(selectedFop);
		fopSelector.setWidthFull();
		fopSelector.addClassName("referee-jury-home-platform-selector");
		fopSelector.addValueChangeListener(event -> {
			if (event.isFromClient() && event.getValue() != null) {
				UI.getCurrent().navigate(MobileScoreboardsContent.class,
				        QueryParameters.simple(Map.of("fop", event.getValue().getName())));
			}
		});
		return fopSelector;
	}

	private Button createDisplayButton(String labelKey, VaadinIcon icon,
	        Class<? extends Component> target) {
		Button button = new Button(Translator.translate(labelKey),
		        event -> navigateTo(target));
		button.setIcon(icon.create());
		button.addThemeVariants(ButtonVariant.LUMO_LARGE);
		button.setWidthFull();
		button.getStyle().set("min-height", "4.5rem");
		displayActions.add(button);
		return button;
	}

	private void navigateTo(Class<? extends Component> target) {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			return;
		}
		Map<String, String> parameters = new HashMap<>();
		parameters.put("fop", fop.getName());
		parameters.put("currentAttempt", "true");
		UI.getCurrent().navigate(target, QueryParameters.simple(parameters));
	}

	private void navigateToReferees() {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			UI.getCurrent().navigate(RefereeHomeContent.class);
		} else {
			UI.getCurrent().navigate(RefereeHomeContent.class,
			        QueryParameters.simple(Map.of("fop", fop.getName())));
		}
	}

	private void updateActions(boolean enabled) {
		displayActions.forEach(button -> button.setEnabled(enabled));
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Scoreboards");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Scoreboards");
	}

	@Override
	public void setHeaderContent() {
		super.setHeaderContent();
		getRouterLayout().setMenuVisible(false);
		getRouterLayout().setDrawerOpened(false);
	}
}