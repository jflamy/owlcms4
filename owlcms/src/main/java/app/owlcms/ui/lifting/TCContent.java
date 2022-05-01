/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.lifting;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.Plates;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class TCContent
 *
 * Technical Controller / Plates loading information.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/tc", layout = AthleteGridLayout.class)
@CssImport(value = "./styles/plates.css")
public class TCContent extends AthleteGridContent implements HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(TCContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private OwlcmsCrudFormFactory<Athlete> crudFormFactory;
    private Plates plates;
    private Platform platform;

    public TCContent() {
        super();
        setTopBarTitle(getTranslation("PlatesCollarBarbell"));
    }

    @Override
    public Athlete add(Athlete athlete) {
        // do nothing
        return athlete;
    }

    @Override
    public void delete(Athlete Athlete) {

        // do nothing;
    }

    public OwlcmsCrudFormFactory<Athlete> getCrudFormFactory() {
        return crudFormFactory;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("PlatesCollarBarbell") + OwlcmsSession.getFopNameIfMultiple();
    }

    public void setCrudFormFactory(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        this.crudFormFactory = crudFormFactory;
    }

    @Override
    @Subscribe
    public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
        OwlcmsSession.withFop((fop) -> UIEventProcessor.uiAccess(plates, uiEventBus, () -> {
            plates.computeImageArea(fop, false);
        }));
    }

    @Override
    public Athlete update(Athlete athlete) {
        // do nothing
        return athlete;
    }

    @Override
    protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
     */
    @Override
    protected void createTopBar() {
        super.createTopBar();
        // this hides the back arrow
        getAppLayout().setMenuVisible(false);
    }

    @Override
    protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
        HorizontalLayout decisions = new HorizontalLayout();
        return decisions;
    }

    @Override
    protected void init() {
        setCrudFormFactory(createFormFactory());

        plates = new Plates();
        plates.setId("loadchart");
        OwlcmsSession.withFop((fop) -> {
            plates.computeImageArea(fop, false);
            platform = fop.getPlatform();
        });
        plates.getStyle().set("font-size", "150%");

        FormLayout largePlates = new FormLayout();
        FormLayout smallPlates = new FormLayout();
        FormLayout collar = new FormLayout();
        FormLayout lightBar = new FormLayout();
        StringToIntegerConverter converter = new StringToIntegerConverter(getTranslation("MustEnterNumber"));

        Binder<Platform> binder = new Binder<>();

        TextField nbL25 = new TextField();
        largePlates.addFormItem(nbL25, getTranslation("Kg", 25));
        binder.forField(nbL25).withConverter(converter).bind(Platform::getNbL_25, Platform::setNbL_25);
        nbL25.setAutoselect(true);

        TextField nbL20 = new TextField();
        largePlates.addFormItem(nbL20, getTranslation("Kg", 20));
        binder.forField(nbL20).withConverter(converter).bind(Platform::getNbL_20, Platform::setNbL_20);
        nbL20.setAutoselect(true);

        TextField nbL15 = new TextField();
        largePlates.addFormItem(nbL15, getTranslation("Kg", 15));
        binder.forField(nbL15).withConverter(converter).bind(Platform::getNbL_15, Platform::setNbL_15);
        nbL15.setAutoselect(true);

        TextField nbL10 = new TextField();
        largePlates.addFormItem(nbL10, getTranslation("Kg", 10));
        binder.forField(nbL10).withConverter(converter).bind(Platform::getNbL_10, Platform::setNbL_10);
        nbL10.setAutoselect(true);

        TextField nbL5 = new TextField();
        largePlates.addFormItem(nbL5, getTranslation("Kg", 5));
        binder.forField(nbL5).withConverter(converter).bind(Platform::getNbL_5, Platform::setNbL_5);
        nbL5.setAutoselect(true);

        TextField nbL2_5 = new TextField();
        largePlates.addFormItem(nbL2_5, getTranslation("Kg", 2.5));
        binder.forField(nbL2_5).withConverter(converter).bind(Platform::getNbL_2_5, Platform::setNbL_2_5);
        nbL2_5.setAutoselect(true);

        TextField nbS5 = new TextField();
        smallPlates.addFormItem(nbS5, getTranslation("Kg", 5));
        binder.forField(nbS5).withConverter(converter).bind(Platform::getNbS_5, Platform::setNbS_5);
        nbS5.setAutoselect(true);

        TextField nbS2_5 = new TextField();
        smallPlates.addFormItem(nbS2_5, getTranslation("Kg", 2.5));
        binder.forField(nbS2_5).withConverter(converter).bind(Platform::getNbS_2_5, Platform::setNbS_2_5);
        nbS2_5.setAutoselect(true);

        TextField nbS2 = new TextField();
        smallPlates.addFormItem(nbS2, getTranslation("Kg", 2));
        binder.forField(nbS2).withConverter(converter).bind(Platform::getNbS_2, Platform::setNbS_2);
        nbS2.setAutoselect(true);

        TextField nbS1_5 = new TextField();
        smallPlates.addFormItem(nbS1_5, getTranslation("Kg", 1.5));
        binder.forField(nbS1_5).withConverter(converter).bind(Platform::getNbS_1_5, Platform::setNbS_1_5);
        nbL20.setAutoselect(true);

        TextField nbS1 = new TextField();
        smallPlates.addFormItem(nbS1, getTranslation("Kg", 1));
        binder.forField(nbS1).withConverter(converter).bind(Platform::getNbS_1, Platform::setNbS_1);
        nbS1.setAutoselect(true);

        TextField nbS0_5 = new TextField();
        smallPlates.addFormItem(nbS0_5, getTranslation("Kg", 0.5));
        binder.forField(nbS0_5).withConverter(converter).bind(Platform::getNbS_0_5, Platform::setNbS_0_5);
        nbS0_5.setAutoselect(true);

        TextField nbC2_5 = new TextField();
        collar.addFormItem(nbC2_5, getTranslation("Kg", 2.5));
        binder.forField(nbC2_5).withConverter(converter).bind(Platform::getNbC_2_5, Platform::setNbC_2_5);
        nbC2_5.setAutoselect(true);

        Checkbox useOtherBar = new Checkbox();
        TextField barWeight = new TextField();
        lightBar.addFormItem(useOtherBar, getTranslation("NonStandardBar"));
        useOtherBar.addValueChangeListener((e) -> {
            barWeight.setEnabled(Boolean.TRUE.equals(e.getValue()));
        });
        binder.forField(useOtherBar).bind(Platform::isNonStandardBar, Platform::setNonStandardBar);

        barWeight.setEnabled(platform.isNonStandardBar());
        lightBar.addFormItem(barWeight, getTranslation("BarWeight"));
        int min = 1;
        int max = 20;
        IntegerRangeValidator integerValidator = new IntegerRangeValidator(getTranslation("BetweenValues", min, max),
                min, max);
        binder.forField(barWeight).withConverter(converter)
                .withValidator((v, c) -> BooleanUtils.isTrue(useOtherBar.getValue()) ? integerValidator.apply(v, c)
                        : ValidationResult.ok())
                .bind(Platform::getLightBar, Platform::setLightBar);

        VerticalLayout platesDisplay = new VerticalLayout(plates);
        platesDisplay.setAlignItems(Alignment.CENTER);
        Button applyButton = new Button(getTranslation("Apply"));
        applyButton.setThemeName("primary");
        applyButton.addClickListener((e) -> {
            try {
                binder.writeBean(platform);
                PlatformRepository.save(platform);
                OwlcmsSession.withFop((fop) -> {
                    platesDisplay.removeAll();
                    plates.computeImageArea(fop, false);
                    platesDisplay.add(plates);
                    fop.fopEventPost(new FOPEvent.BarbellOrPlatesChanged(this));
                });
            } catch (ValidationException e1) {
            }

        });
        FlexLayout platesEdit = new FlexLayout(new H3(getTranslation("BumperPlates")), largePlates,
                new H3(getTranslation("MetalPlates")), smallPlates, new H3(getTranslation("Collar")), collar,
                new H3(getTranslation("Bar")), lightBar, new H3(""), applyButton);
        platesEdit.getStyle().set("flex-direction", "column");
        HorizontalLayout leftRight = new HorizontalLayout(platesDisplay, platesEdit);
        leftRight.setFlexGrow(1.0D, platesDisplay, platesEdit);
        leftRight.setSizeFull();
        leftRight.setAlignItems(Alignment.CENTER);

        fillHW(leftRight, this);
        binder.readBean(platform);
    }

}
