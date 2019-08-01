/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import static com.github.appreciated.app.layout.entity.Section.FOOTER;
import static com.github.appreciated.app.layout.entity.Section.HEADER;

import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.github.appreciated.app.layout.builder.AppLayoutBuilder;
import com.github.appreciated.app.layout.component.appbar.AppBarBuilder;
import com.github.appreciated.app.layout.component.menu.left.builder.LeftAppMenuBuilder;
import com.github.appreciated.app.layout.component.menu.left.items.LeftClickableItem;
import com.github.appreciated.app.layout.component.menu.left.items.LeftHeaderItem;
import com.github.appreciated.app.layout.component.menu.left.items.LeftNavigationItem;
import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;

import app.owlcms.i18n.Translator;
import app.owlcms.ui.displayselection.DisplayNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.InfoNavigationContent;
import app.owlcms.ui.lifting.LiftingNavigationContent;
import app.owlcms.ui.preparation.PreparationNavigationContent;
import app.owlcms.ui.results.ResultsNavigationContent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * OwlcmsRouterLayout.
 */
@SuppressWarnings("serial")
@Push
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@StyleSheet("frontend://styles/owlcms.css")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/av-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/hardware-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/social-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/places-icons.html")
public class OwlcmsRouterLayout extends AppLayoutRouterLayout implements PageConfigurator {

    final private Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsRouterLayout.class);
    {
        logger.setLevel(Level.INFO);
    }

    private Behaviour variant;

    private HasElement layoutComponentContent;

    String PREPARE_COMPETITION = Translator.translate("PrepareCompetition"); //$NON-NLS-1$
    String RUN_LIFTING_GROUP = Translator.translate("RunLiftingGroup"); //$NON-NLS-1$
    String START_DISPLAYS = Translator.translate("StartDisplays"); //$NON-NLS-1$
    String RESULT_DOCUMENTS = Translator.translate("ResultDocuments"); //$NON-NLS-1$
    String INFO = Translator.translate("About"); //$NON-NLS-1$
    String PREFERENCES = Translator.translate("Preferences"); //$NON-NLS-1$
    String DOCUMENTATION = Translator.translate("Documentation_Menu"); //$NON-NLS-1$

    public OwlcmsRouterLayout() {
        init(getLayoutConfiguration(variant));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#
     * showRouterLayoutContent(com.vaadin.flow.component.HasElement)
     */
    @Override
    public void showRouterLayoutContent(HasElement content) {
        logger.debug("showRouterLayoutContent {}", content.getClass().getSimpleName()); //$NON-NLS-1$
        ((AppLayoutAware) content).setRouterLayout(this);
        super.showRouterLayoutContent(content);
        this.setLayoutComponentContent(content);
    }

    /**
     * @return the layoutComponentContent
     */
    public HasElement getLayoutComponentContent() {
        return layoutComponentContent;
    }

    protected AppLayout getLayoutConfiguration(Behaviour variant) {
        if (variant == null) {
            variant = Behaviour.LEFT_RESPONSIVE;
        }

        LeftNavigationItem home = new LeftNavigationItem(getTranslation("Home"), //$NON-NLS-1$
                VaadinIcon.HOME.create(), HomeNavigationContent.class);

        AppLayout appLayout = AppLayoutBuilder.get(variant).withTitle(getTranslation("OWLCMS_Top")) //$NON-NLS-1$
                .withIcon("/frontend/images/logo.png") //$NON-NLS-1$
                .withAppBar(AppBarBuilder.get().build()).withAppMenu(LeftAppMenuBuilder.get()
//				.addToSection(new LeftHeaderItem(null, OwlcmsFactory.getVersion(), null), HEADER)
                        .addToSection(new LeftHeaderItem(null, "", null), HEADER).add(home)
                        .add(new LeftNavigationItem(PREPARE_COMPETITION, new Icon("social", "group-add"), //$NON-NLS-1$ //$NON-NLS-2$
                                PreparationNavigationContent.class))
                        .add(new LeftNavigationItem(RUN_LIFTING_GROUP, new Icon("places", "fitness-center"), //$NON-NLS-1$ //$NON-NLS-2$
                                LiftingNavigationContent.class))
                        .add(new LeftNavigationItem(START_DISPLAYS, new Icon("hardware", "desktop-windows"), //$NON-NLS-1$ //$NON-NLS-2$
                                DisplayNavigationContent.class))
                        .add(new LeftNavigationItem(RESULT_DOCUMENTS, new Icon("maps", "local-printshop"), //$NON-NLS-1$ //$NON-NLS-2$
                                ResultsNavigationContent.class))
                        .add(new LeftClickableItem(DOCUMENTATION, new Icon("icons", "help"),
                                clickEvent -> UI.getCurrent().getPage().executeJavaScript("window.open('https://jflamy.github.io/owlcms4/#/','_blank')")))
                        .addToSection(new LeftNavigationItem(INFO, new Icon("icons", "info-outline"), //$NON-NLS-1$ //$NON-NLS-2$
                                InfoNavigationContent.class), FOOTER)
                        .build())
                .build();

        return appLayout;
    }

//    .add(new LeftClickableItem(DOCUMENTATION, VaadinIcon.COG.create(),
//            clickEvent -> openModeSelector(this.variant))
    private void setDrawerVariant(Behaviour variant) {
        this.variant = variant;
        init(getLayoutConfiguration(variant));
    }

    private void openModeSelector(Behaviour variant) {
        new BehaviourSelector(variant, this::setDrawerVariant).open();
    }

    private void setLayoutComponentContent(HasElement layoutContent) {
        this.layoutComponentContent = layoutContent;
    }

    /**
     * The Class BehaviourSelector.
     */
    class BehaviourSelector extends Dialog {
        /**
         * Instantiates a new behaviour selector.
         *
         * @param current  the current
         * @param consumer the consumer
         */
        public BehaviourSelector(Behaviour current, Consumer<Behaviour> consumer) {
            VerticalLayout layout = new VerticalLayout();
            add(layout);
            RadioButtonGroup<Behaviour> group = new RadioButtonGroup<>();
            group.getElement().getStyle().set("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
            group.getElement().getStyle().set("flexDirection", "column"); //$NON-NLS-1$ //$NON-NLS-2$
            group.setItems(Behaviour.LEFT, Behaviour.LEFT_OVERLAY, Behaviour.LEFT_RESPONSIVE, Behaviour.LEFT_HYBRID,
                    Behaviour.LEFT_HYBRID_SMALL, Behaviour.LEFT_RESPONSIVE_HYBRID,
//				Behaviour.LEFT_RESPONSIVE_HYBRID_NO_APP_BAR,
//				Behaviour.LEFT_RESPONSIVE_HYBRID_OVERLAY_NO_APP_BAR,
                    Behaviour.LEFT_RESPONSIVE_OVERLAY,
//				Behaviour.LEFT_RESPONSIVE_OVERLAY_NO_APP_BAR,
                    Behaviour.LEFT_RESPONSIVE_SMALL
//				Behaviour.LEFT_RESPONSIVE_SMALL_NO_APP_BAR
//				Behaviour.TOP,
//				Behaviour.TOP_LARGE
            );
            group.setValue(current);
            layout.add(group);
            group.addValueChangeListener(singleSelectionEvent -> {
                consumer.accept(singleSelectionEvent.getValue());
                close();
            });
        }
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addInlineWithContents("<link rel=\"icon\" href=\"./frontend/images/owlcms.ico\">", //$NON-NLS-1$
                InitialPageSettings.WrapMode.NONE);
    }
}
