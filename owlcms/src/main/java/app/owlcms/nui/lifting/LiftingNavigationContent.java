/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.referee.RefContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting", layout = OwlcmsLayout.class)
public class LiftingNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(LiftingNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Competition Group Navigation
	 */
	public LiftingNavigationContent() {
		DebugUtils.gc();
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {

		super.onAttach(attachEvent);
		
		logger.trace("LiftingNavigationContent onAttach start");

		Button weighIn = openInNewTabNoParam(WeighinContent.class, Translator.translate("WeighIn_Title"));
		weighIn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
		weighIn.setIcon(new Icon(VaadinIcon.SCALE));
		// weighIn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(weighIn);
		doGroup(Translator.translate("WeighIn_Title"), grid3, this);

		Button announcer = openInNewTab(AnnouncerContent.class, Translator.translate("Announcer"));
		announcer.setIcon(new Icon(VaadinIcon.MICROPHONE));
		announcer.setTabIndex(2);
		announcer.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
		announcer.setThemeName(FOP, isAttached());
		Button marshall = openInNewTab(MarshallContent.class, Translator.translate("Marshall"));
		Button timekeeper = openInNewTab(TimekeeperContent.class, Translator.translate("Timekeeper"));
		Button technical = openInNewTab(TCContent.class, Translator.translate("PlatesCollarBarbell"));

		VerticalLayout intro = new VerticalLayout();
		addP(intro, Translator.translate("AnnouncerSelectsGroup") + Translator.translate("ChangesGroupEverywhere")
		        + Translator.translate("AnnouncerEtc"));
		intro.getStyle().set("margin-bottom", "0");

		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(announcer, marshall, timekeeper, technical);
		doGroup(Translator.translate("Scoreboard.LiftingOrder"), intro, grid1, this);

		Button referee = openInNewTab(RefContent.class, Translator.translate("Referee_Mobile_Device"));
		Button jury = openInNewTab(JuryContent.class, Translator.translate("Jury_Console"));
		FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(referee, jury);
		doGroup(Translator.translate("Referees_Jury"), grid2, this);
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("RunLiftingGroup");
	}

	@Override
	public String getPageTitle() {
		String fopNameIfMultiple = OwlcmsSession.getFopNameIfMultiple();
		return Translator.translate("ShortTitle.Lifting") + (!fopNameIfMultiple.isBlank() ? (" - " + fopNameIfMultiple) : "");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		NativeLabel fopLabel = new NativeLabel(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}
}
