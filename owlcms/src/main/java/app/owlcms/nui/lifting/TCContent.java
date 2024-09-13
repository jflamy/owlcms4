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

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
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

import app.owlcms.components.elements.PlatesElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
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
	private PlatesElement plates;
	private Platform platform;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public TCContent() {
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
		return this.crudFormFactory;
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
		return Translator.translate("PlatesCollarBarbell") + OwlcmsSession.getFopNameIfMultiple();
	}

	public void setCrudFormFactory(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		this.crudFormFactory = crudFormFactory;
	}

	@Override
	@Subscribe
	public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
		OwlcmsSession.withFop((fop) -> UIEventProcessor.uiAccess(this.plates, this.uiEventBus, () -> {
			this.plates.computeImageArea(fop, true);
		}));
	}
	
	@Override
	@Subscribe
	public void slaveUpdateGrid(UIEvent.Decision e) {
		OwlcmsSession.withFop((fop) -> UIEventProcessor.uiAccess(this.plates, this.uiEventBus, () -> {
			this.plates.computeImageArea(fop, true);
		}));
	}
	
	@Subscribe
	public void slaveBarbellChanged(UIEvent.BarbellOrPlatesChanged e) {
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		if (e.getOrigin() == this) {return;}
		if (fop2 != null) {
			this.platform = fop2.getPlatform();
			//logger.debug("slaveBarbellChanged");
			plates.computeImageArea(fop2, true);
		}
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

		this.plates = new PlatesElement(UI.getCurrent());
		this.plates.setId("loadchart");
		OwlcmsSession.withFop((fop) -> {
			this.plates.computeImageArea(fop, true);
			this.platform = fop.getPlatform();
			//logger.debug"init 5kg = {}",this.platform.getNbB_5());
		});
		this.plates.getStyle().set("font-size", "150%");

		FormLayout largePlates = new FormLayout();
		FormLayout smallPlates = new FormLayout();
		FormLayout collar = new FormLayout();
		FormLayout lightBar = new FormLayout();
		StringToIntegerConverter converter = new StringToIntegerConverter(Translator.translate("MustEnterNumber"));

		Binder<Platform> binder = new Binder<>();
		Converter<Boolean, Integer> bc = Converter.from(checked -> Result.ok(checked ? 1 : 0),
		        value -> value > 0);

		TextField nbL25 = new TextField();
		nbL25.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
		nbL25.setWidth("4ch");
		largePlates.addFormItem(nbL25, Translator.translate("Kg", 25));
		binder.forField(nbL25).withConverter(converter).bind(Platform::getNbL_25, Platform::setNbL_25);
		nbL25.setAutoselect(true);

		TextField nbL20 = new TextField();
		nbL20.setWidth("4ch");
		nbL20.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
		largePlates.addFormItem(nbL20, Translator.translate("Kg", 20));
		binder.forField(nbL20).withConverter(converter).bind(Platform::getNbL_20, Platform::setNbL_20);
		nbL20.setAutoselect(true);

		Checkbox nbL15 = new Checkbox();
		largePlates.addFormItem(nbL15, Translator.translate("Kg", 15));
		binder.forField(nbL15).withConverter(bc).bind(Platform::getNbL_15, Platform::setNbL_15);

		Checkbox nbL10 = new Checkbox();
		largePlates.addFormItem(nbL10, Translator.translate("Kg", 10));
		binder.forField(nbL10).withConverter(bc).bind(Platform::getNbL_10, Platform::setNbL_10);

		Checkbox nbL5 = new Checkbox();
		largePlates.addFormItem(nbL5, Translator.translate("Kg", 5));
		binder.forField(nbL5).withConverter(bc).bind(Platform::getNbL_5, Platform::setNbL_5);

		Checkbox nbL2_5 = new Checkbox();
		largePlates.addFormItem(nbL2_5, Translator.translate("Kg", 2.5));
		binder.forField(nbL2_5).withConverter(bc).bind(Platform::getNbL_2_5, Platform::setNbL_2_5);

		Checkbox nbS5 = new Checkbox();
		smallPlates.addFormItem(nbS5, Translator.translate("Kg", 5));
		binder.forField(nbS5).withConverter(bc).bind(Platform::getNbS_5, Platform::setNbS_5);

		Checkbox nbS2_5 = new Checkbox();
		smallPlates.addFormItem(nbS2_5, Translator.translate("Kg", 2.5));
		binder.forField(nbS2_5).withConverter(bc).bind(Platform::getNbS_2_5, Platform::setNbS_2_5);

		Checkbox nbS2 = new Checkbox();
		smallPlates.addFormItem(nbS2, Translator.translate("Kg", 2));
		binder.forField(nbS2).withConverter(bc).bind(Platform::getNbS_2, Platform::setNbS_2);

		Checkbox nbS1_5 = new Checkbox();
		smallPlates.addFormItem(nbS1_5, Translator.translate("Kg", 1.5));
		binder.forField(nbS1_5).withConverter(bc).bind(Platform::getNbS_1_5, Platform::setNbS_1_5);
		nbL20.setAutoselect(true);

		Checkbox nbS1 = new Checkbox();
		smallPlates.addFormItem(nbS1, Translator.translate("Kg", 1));
		binder.forField(nbS1).withConverter(bc).bind(Platform::getNbS_1, Platform::setNbS_1);

		Checkbox nbS0_5 = new Checkbox();
		smallPlates.addFormItem(nbS0_5, Translator.translate("Kg", 0.5));
		binder.forField(nbS0_5).withConverter(bc).bind(Platform::getNbS_0_5, Platform::setNbS_0_5);

		Checkbox nbC2_5 = new Checkbox();
		collar.addFormItem(nbC2_5, Translator.translate("Kg", 2.5));
		binder.forField(nbC2_5).withConverter(bc).bind(Platform::getNbC_2_5, Platform::setNbC_2_5);

		Checkbox nbB5 = new Checkbox();
		lightBar.addFormItem(nbB5, Translator.translate("Kg", 5));
		binder.forField(nbB5).withConverter(bc).bind(Platform::getNbB_5, Platform::setNbB_5);
		
		Checkbox nbB10 = new Checkbox();
		lightBar.addFormItem(nbB10, Translator.translate("Kg", 10));
		binder.forField(nbB10).withConverter(bc).bind(Platform::getNbB_10, Platform::setNbB_10);
		
		Checkbox nbB15 = new Checkbox();
		lightBar.addFormItem(nbB15, Translator.translate("Kg", 15));
		binder.forField(nbB15).withConverter(bc).bind(Platform::getNbB_15, Platform::setNbB_15);
		
		Checkbox nbB20 = new Checkbox();
		lightBar.addFormItem(nbB20, Translator.translate("Kg", 20));
		binder.forField(nbB20).withConverter(bc).bind(Platform::getNbB_20, Platform::setNbB_20);
		
		Checkbox useOtherBar = new Checkbox();
		TextField barWeight = new TextField();
		barWeight.setWidth("5ch");
		lightBar.addFormItem(useOtherBar, Translator.translate("NonStandardBar"));
		useOtherBar.addValueChangeListener((e) -> {
			barWeight.setEnabled(Boolean.TRUE.equals(e.getValue()));
		});
		binder.forField(useOtherBar).bind(Platform::isUseNonStandardBar, Platform::setUseNonStandardBar);

		barWeight.setEnabled(this.platform.isUseNonStandardBar());
		lightBar.addFormItem(barWeight, Translator.translate("BarWeight"));
		int min = 1;
		int max = 20;
		IntegerRangeValidator integerValidator = new IntegerRangeValidator(Translator.translate("BetweenValues", min, max),
		        min, max);
		binder.forField(barWeight).withConverter(converter)
		        .withValidator((v, c) -> BooleanUtils.isTrue(useOtherBar.getValue()) ? integerValidator.apply(v, c)
		                : ValidationResult.ok())
		        .bind(Platform::getNonStandardBarWeight, Platform::setNonStandardBarWeight);

		VerticalLayout platesDisplay = new VerticalLayout(this.plates);
		platesDisplay.setAlignItems(Alignment.CENTER);
		Button applyButton = new Button(Translator.translate("Apply"));
		applyButton.setThemeName("primary");
		applyButton.addClickListener((e) -> {
			try {
				binder.writeBean(this.platform);
				Platform np = PlatformRepository.save(this.platform);
				OwlcmsSession.withFop((fop) -> {
					//logger.debug"after save, platform identity={}",System.identityHashCode(fop.getPlatform()));
					platesDisplay.removeAll();
					fop.setPlatform(np);
					// causes fop to recompute what bar to use.
					fop.fopEventPost(new FOPEvent.BarbellOrPlatesChanged(this));
					this.plates.computeImageArea(fop, true);
					platesDisplay.add(this.plates);
				});
			} catch (ValidationException e1) {
			}

		});

		FlexLayout platesEdit = new FlexLayout(
		        new H3(Translator.translate("BumperPlates")),
		        new NativeLabel(Translator.translate("PlatesPerSide")),
		        largePlates,
		        new H3(Translator.translate("MetalPlates")), smallPlates,
		        new H3(Translator.translate("Collar")), collar,
		        new H3(Translator.translate("Bar")), lightBar,
		        new H3(""), applyButton);
		platesEdit.getStyle().set("flex-direction", "column");
		platesEdit.setWidth("120em");
		HorizontalLayout leftRight = new HorizontalLayout(platesDisplay, platesEdit);
		leftRight.setFlexGrow(2.0D, platesDisplay);
		leftRight.setSizeFull();
		leftRight.setAlignItems(Alignment.CENTER);

		fillH(leftRight, this);
		binder.readBean(this.platform);
	}

}
