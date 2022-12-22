/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import static com.github.appreciated.app.layout.entity.Section.FOOTER;
import static com.github.appreciated.app.layout.entity.Section.HEADER;

import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.component.appbar.AppBarBuilder;
import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.github.appreciated.app.layout.component.builder.AppLayoutBuilder;
import com.github.appreciated.app.layout.component.menu.left.builder.LeftAppMenuBuilder;
import com.github.appreciated.app.layout.component.menu.left.items.LeftClickableItem;
import com.github.appreciated.app.layout.component.menu.left.items.LeftHeaderItem;
import com.github.appreciated.app.layout.component.menu.left.items.LeftNavigationItem;
import com.github.appreciated.app.layout.component.router.AppLayoutRouterLayout;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
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
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.displayselection.DisplayNavigationContent;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.home.InfoNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import app.owlcms.uievents.AppEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * OwlcmsLayout.
 */
@SuppressWarnings({ "serial", "rawtypes", "deprecation" })
@Push
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@CssImport(value = "./styles/shared-styles.css")
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@JsModule("@polymer/iron-icon/iron-icon.js")
@JsModule("@polymer/iron-icons/iron-icons.js")
@JsModule("@polymer/iron-icons/av-icons.js")
@JsModule("@polymer/iron-icons/hardware-icons.js")
@JsModule("@polymer/iron-icons/maps-icons.js")
@JsModule("@polymer/iron-icons/social-icons.js")
@JsModule("@polymer/iron-icons/places-icons.js")

public class OwlcmsRouterLayoutX extends AppLayoutRouterLayout implements PageConfigurator {

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
        @SuppressWarnings("unchecked")
        public BehaviourSelector(Class<? extends AppLayout> current, Consumer<Class<? extends AppLayout>> consumer) {
            VerticalLayout layout = new VerticalLayout();
            add(layout);
            RadioButtonGroup<Class<? extends AppLayout>> group = new RadioButtonGroup<>();
            group.getStyle().set("display", "flex");
            group.getStyle().set("flexDirection", "column");
            group.setItems(LeftLayouts.Left.class, LeftLayouts.LeftOverlay.class, LeftLayouts.LeftResponsive.class,
                    LeftLayouts.LeftHybrid.class, LeftLayouts.LeftHybridSmall.class,
                    LeftLayouts.LeftResponsiveHybrid.class, LeftLayouts.LeftResponsiveOverlay.class,
                    LeftLayouts.LeftResponsiveSmall.class);
            group.setValue(current);
            layout.add(group);
            group.addValueChangeListener(singleSelectionEvent -> {
                consumer.accept(singleSelectionEvent.getValue());
                close();
            });
        }
    }

    String DOCUMENTATION = Translator.translate("Documentation_Menu");

    String INFO = Translator.translate("About");

    String PREFERENCES = Translator.translate("Preferences");

    String PREPARE_COMPETITION = Translator.translate("PrepareCompetition");
    String RESULT_DOCUMENTS = Translator.translate("Results");
    String RUN_LIFTING_GROUP = Translator.translate("RunLiftingGroup");
    String START_DISPLAYS = Translator.translate("StartDisplays");
    private HasElement layoutComponentContent;
    final private Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsLayout.class);
    private Class<? extends AppLayout> variant;

    private UI ui;

    {
        logger.setLevel(Level.INFO);
    }

    @SuppressWarnings("unchecked")
    public OwlcmsRouterLayoutX() {
        try {
            OwlcmsFactory.getInitializationLatch().await();
            OwlcmsFactory.getAppUIBus().register(this);
        } catch (InterruptedException e) {
        }
        init(getLayoutConfiguration(variant));
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addInlineWithContents("<link rel=\"icon\" href=\"./frontend/images/owlcms.ico\">",
                InitialPageSettings.WrapMode.NONE);
    }

    /**
     * @return the layoutComponentContent
     */
    public HasElement getLayoutComponentContent() {
        return layoutComponentContent;
    }

    @Subscribe
    public void slaveAppClose(AppEvent.CloseUI e) {
        if (ui == null) {
            return;
        }
        ui.access(() -> {
            OwlcmsFactory.getAppUIBus().unregister(this);
            e.closeUI();
        });
    }

    @Subscribe
    public void slaveAppNotification(AppEvent.AppNotification e) {
        if (ui == null) {
            return;
        }
        ui.access(() -> {
            e.doNotification();
        });
    }

//    /*
//     * (non-Javadoc)
//     *
//     * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#
//     * showRouterLayoutContent(com.vaadin.flow.component.HasElement)
//     */
//    @Override
//    public void showRouterLayoutContent(HasElement content) {
//        logger.debug("showRouterLayoutContent {}", content.getClass().getSimpleName());
//        ((OwlcmsLayoutAware) content).setRouterLayout(this);
//        super.showRouterLayoutContent(content);
//        this.setLayoutComponentContent(content);
//    }

    /**
     * @param variant
     * @return
     */
    protected AppLayout getLayoutConfiguration(Class<? extends AppLayout> variant) {
        if (variant == null) {
            variant = LeftLayouts.LeftResponsive.class;
        }

        LeftNavigationItem home = new LeftNavigationItem(getTranslation("Home"), VaadinIcon.HOME.create(),
                HomeNavigationContent.class);

        AppLayout appLayout = AppLayoutBuilder.get(variant).withTitle(getTranslation("OWLCMS_Top"))
                .withIcon("/frontend/images/logo.png").withAppBar(AppBarBuilder.get().build())
                .withAppMenu(LeftAppMenuBuilder.get().addToSection(HEADER, new LeftHeaderItem(null, "", null)).add(home)
                        .add(new LeftNavigationItem(PREPARE_COMPETITION, new Icon("social:group-add"),
                                PreparationNavigationContent.class))
                        .add(new LeftNavigationItem(RUN_LIFTING_GROUP, new Icon("places:fitness-center"),
                                LiftingNavigationContent.class))
                        .add(new LeftNavigationItem(START_DISPLAYS, new Icon("hardware:desktop-windows"),
                                DisplayNavigationContent.class))
                        .add(new LeftNavigationItem(RESULT_DOCUMENTS, new Icon("maps:local-printshop"),
                                ResultsNavigationContent.class))
                        .add(new LeftClickableItem(DOCUMENTATION, new Icon("icons:help"),
                                clickEvent -> UI.getCurrent().getPage()
                                        .executeJs("window.open('https://jflamy.github.io/owlcms4/#/index','_blank')")))
                        // .add(new LeftNavigationItem(RESULT_DOCUMENTS, new Icon("image", "brightness-2"),
                        // ResultsNavigationContent.class))
                        .addToSection(FOOTER, new LeftNavigationItem(INFO, new Icon("icons:info-outline"),
                                InfoNavigationContent.class))
                        .build())
                .build();

        return appLayout;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = UI.getCurrent();
        // crude workaround -- randomly getting "dark" due to multiple themes detected in app.
        getElement().executeJs("document.querySelector('html').setAttribute('theme', 'light');");
        super.onAttach(attachEvent);
    }

    @SuppressWarnings("unused")
    private void openModeSelector(Class<? extends AppLayout> variant) {
        new BehaviourSelector(variant, this::setDrawerVariant).open();
    }

    // .add(new LeftClickableItem(DOCUMENTATION, VaadinIcon.COG.create(),
    // clickEvent -> openModeSelector(this.variant))
    @SuppressWarnings("unchecked")
    private void setDrawerVariant(Class<? extends AppLayout> variant) {
        this.variant = variant;
        init(getLayoutConfiguration(variant));
    }

//    private void setLayoutComponentContent(HasElement layoutContent) {
//        this.layoutComponentContent = layoutContent;
//    }
}
