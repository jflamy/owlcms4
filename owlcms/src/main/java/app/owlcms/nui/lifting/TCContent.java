/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.Converter;
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
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class TCContent
 *
 * Technical Controller / Plates loading information.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/tc", layout = OwlcmsLayout.class)
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
    }

    @Override
    public Athlete add(Athlete athlete) {
        // do nothing
        return athlete;
    }

    /**
     * @see app.owlcms.nui.shared.AthleteGridContent#createTopBar()
     */
    @Override
    public FlexLayout createMenuArea() {
        FlexLayout fl = super.createMenuArea();
        // this hides the back arrow
        getAppLayout().setMenuVisible(true);
        return fl;
    }

    @Override
    public void delete(Athlete Athlete) {

        // do nothing;
    }

    public OwlcmsCrudFormFactory<Athlete> getCrudFormFactory() {
        return crudFormFactory;
    }

    @Override
    public String getMenuTitle() {
        return getPageTitle();
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
        Converter<Boolean, Integer> bc = Converter.from(checked -> Result.ok(checked ? 1 : 0),
                value -> value > 0);

        TextField nbL25 = new TextField();
        nbL25.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        nbL25.setWidth("4ch");
        largePlates.addFormItem(nbL25, getTranslation("Kg", 25));
        binder.forField(nbL25).withConverter(converter).bind(Platform::getNbL_25, Platform::setNbL_25);
        nbL25.setAutoselect(true);

        TextField nbL20 = new TextField();
        nbL20.setWidth("4ch");
        nbL20.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        largePlates.addFormItem(nbL20, getTranslation("Kg", 20));
        binder.forField(nbL20).withConverter(converter).bind(Platform::getNbL_20, Platform::setNbL_20);
        nbL20.setAutoselect(true);

        Checkbox nbL15 = new Checkbox();
        largePlates.addFormItem(nbL15, getTranslation("Kg", 15));
        binder.forField(nbL15).withConverter(bc).bind(Platform::getNbL_15, Platform::setNbL_15);

        Checkbox nbL10 = new Checkbox();
        largePlates.addFormItem(nbL10, getTranslation("Kg", 10));
        binder.forField(nbL10).withConverter(bc).bind(Platform::getNbL_10, Platform::setNbL_10);

        Checkbox nbL5 = new Checkbox();
        largePlates.addFormItem(nbL5, getTranslation("Kg", 5));
        binder.forField(nbL5).withConverter(bc).bind(Platform::getNbL_5, Platform::setNbL_5);

        Checkbox nbL2_5 = new Checkbox();
        largePlates.addFormItem(nbL2_5, getTranslation("Kg", 2.5));
        binder.forField(nbL2_5).withConverter(bc).bind(Platform::getNbL_2_5, Platform::setNbL_2_5);

        Checkbox nbS5 = new Checkbox();
        smallPlates.addFormItem(nbS5, getTranslation("Kg", 5));
        binder.forField(nbS5).withConverter(bc).bind(Platform::getNbS_5, Platform::setNbS_5);

        Checkbox nbS2_5 = new Checkbox();
        smallPlates.addFormItem(nbS2_5, getTranslation("Kg", 2.5));
        binder.forField(nbS2_5).withConverter(bc).bind(Platform::getNbS_2_5, Platform::setNbS_2_5);

        Checkbox nbS2 = new Checkbox();
        smallPlates.addFormItem(nbS2, getTranslation("Kg", 2));
        binder.forField(nbS2).withConverter(bc).bind(Platform::getNbS_2, Platform::setNbS_2);

        Checkbox nbS1_5 = new Checkbox();
        smallPlates.addFormItem(nbS1_5, getTranslation("Kg", 1.5));
        binder.forField(nbS1_5).withConverter(bc).bind(Platform::getNbS_1_5, Platform::setNbS_1_5);
        nbL20.setAutoselect(true);

        Checkbox nbS1 = new Checkbox();
        smallPlates.addFormItem(nbS1, getTranslation("Kg", 1));
        binder.forField(nbS1).withConverter(bc).bind(Platform::getNbS_1, Platform::setNbS_1);

        Checkbox nbS0_5 = new Checkbox();
        smallPlates.addFormItem(nbS0_5, getTranslation("Kg", 0.5));
        binder.forField(nbS0_5).withConverter(bc).bind(Platform::getNbS_0_5, Platform::setNbS_0_5);

        Checkbox nbC2_5 = new Checkbox();
        collar.addFormItem(nbC2_5, getTranslation("Kg", 2.5));
        binder.forField(nbC2_5).withConverter(bc).bind(Platform::getNbC_2_5, Platform::setNbC_2_5);

        Checkbox useOtherBar = new Checkbox();
        TextField barWeight = new TextField();
        barWeight.setWidth("5ch");
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

        FlexLayout platesEdit = new FlexLayout(
                new H3(getTranslation("BumperPlates")),
                new Label(getTranslation("PlatesPerSide")),
                largePlates,
                new H3(getTranslation("MetalPlates")), smallPlates,
                new H3(getTranslation("Collar")), collar,
                new H3(getTranslation("Bar")), lightBar,
                new H3(""), applyButton);
        platesEdit.getStyle().set("flex-direction", "column");
        platesEdit.setWidth("120em");
        HorizontalLayout leftRight = new HorizontalLayout(platesDisplay, platesEdit);
        leftRight.setFlexGrow(2.0D, platesDisplay);
        leftRight.setSizeFull();
        leftRight.setAlignItems(Alignment.CENTER);

        fillH(leftRight, this);
        binder.readBean(platform);
    }

}
