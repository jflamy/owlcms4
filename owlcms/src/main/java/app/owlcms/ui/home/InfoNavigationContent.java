/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.home;

import java.time.LocalDate;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
/**
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Route(value = "info", layout = OwlcmsRouterLayout.class)
public class InfoNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(InfoNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	
	/**
	 * Instantiates a new main navigation content.
	 */
	public InfoNavigationContent() {
		VerticalLayout license = buildLicense();
		fillH(license, this);
	}

	private VerticalLayout buildLicense() {
		VerticalLayout license = new VerticalLayout();
		license.add(new H3(getTranslation("CopyrightLicense"))); //$NON-NLS-1$
		addP(license,
				getTranslation("Copyright2009")+LocalDate.now().getYear()+getTranslation("JFL") //$NON-NLS-1$ //$NON-NLS-2$
				);
		addP(license,
				getTranslation("LicenseUsed") //$NON-NLS-1$
				);
		license.add(new H3(getTranslation("SourceDocumentation"))); //$NON-NLS-1$
		addP(license,
				getTranslation("ProjectRepository")+ //$NON-NLS-1$
				getTranslation("Documentation")+ //$NON-NLS-1$
				getTranslation("InstallationConfiguration") //$NON-NLS-1$
				);
		
		license.add(new H3(getTranslation("Notes"))); //$NON-NLS-1$
		addP(license,
				getTranslation("TCRRCompliance")+ //$NON-NLS-1$
				getTranslation("AtTimeOfRelease")+ //$NON-NLS-1$
				getTranslation("UseAtYourOwnRisk") //$NON-NLS-1$
				);
		
		license.add(new H3(getTranslation("Credits"))); //$NON-NLS-1$
		addP(license,
				getTranslation("WrittenJFL")+ //$NON-NLS-1$
				getTranslation("ThanksToAll")+ //$NON-NLS-1$
				getTranslation("ThanksToFHQ") //$NON-NLS-1$
				);
		
		return license;
	}

	/**
	 * Navigation crudGrid.
	 *
	 * @param items the items
	 * @return the flexible crudGrid layout
	 */
	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FILL, new MinMax(new Length("300px"), new Flex(1))) //$NON-NLS-1$
			.withAutoRows(new Length("1fr")) //$NON-NLS-1$
			.withItems(items)
			.withGap(new Length("2vmin")) //$NON-NLS-1$
			.withOverflow(Overflow.AUTO)
			.withAutoFlow(AutoFlow.ROW)
			.withMargin(false)
			.withPadding(true)
			.withSpacing(false);
		layout.setSizeUndefined();
		layout.setWidth("80%"); //$NON-NLS-1$
		layout.setBoxSizing(BoxSizing.BORDER_BOX);
		return layout;
	}

	/**
	 * The left part of the top bar.
	 * @see app.owlcms.ui.shared.BaseNavigationContent#configureTopBarTitle(java.lang.String)
	 */
	@Override
	protected void configureTopBarTitle(String topBarTitle) {
		AbstractLeftAppLayoutBase appLayout = getAppLayout();
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 40em"); //$NON-NLS-1$ //$NON-NLS-2$
		Label label = new Label(getTitle());
		appLayout.setTitleComponent(label);
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarFopField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarGroupField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}

	/**
	 * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#getTitle()
	 */
	@Override
	protected String getTitle() {
		return getTranslation("OWLCMS_Top"); //$NON-NLS-1$
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("OWLCMS_Info"); //$NON-NLS-1$
	}

}