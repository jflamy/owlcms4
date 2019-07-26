/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class TCContent
 * 
 * Technical Controller / Plates loading information.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/timekeeper", layout = AthleteGridLayout.class)
public class TCContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TCContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName()); //$NON-NLS-1$
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

    private OwlcmsCrudFormFactory<Athlete> crudFormFactory;
	
	public TCContent() {
		super();
		setTopBarTitle(getTranslation("TechnicalController"));	 //$NON-NLS-1$
	}
	
    protected void init() {
        setCrudFormFactory(createFormFactory());
        // no grid.
        //crudGrid = createCrudGrid(crudFormFactory);
        defineFilters(crudGrid);
        fillHW(crudGrid, this);
    }
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {

		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			(new BreakDialog(this)).open();
		});
		breakButton.getElement().setAttribute("theme", "icon"); //$NON-NLS-1$ //$NON-NLS-2$
		breakButton.getElement().setAttribute("title", getTranslation("BreakTimer")); //$NON-NLS-1$ //$NON-NLS-2$
		HorizontalLayout buttons = new HorizontalLayout(
				breakButton);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}


	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}


	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}


	@Override
	public void delete(Athlete Athlete) {;
		// do nothing;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("TechnicalController"); //$NON-NLS-1$
	}

    public OwlcmsCrudFormFactory<Athlete> getCrudFormFactory() {
        return crudFormFactory;
    }

    public void setCrudFormFactory(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        this.crudFormFactory = crudFormFactory;
    }
	
}
