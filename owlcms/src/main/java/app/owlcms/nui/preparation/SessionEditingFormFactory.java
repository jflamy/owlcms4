/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.StringLengthValidator;

import app.owlcms.components.fields.LocalDateTimePicker;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class SessionEditingFormFactory
        extends OwlcmsCrudFormFactory<Group>
        implements CustomFormFactory<Group> {

	private static final String HEIGHT = "32rem";
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(SessionEditingFormFactory.class);
	private SessionContent origin;
	ComboBox<Platform> platformField;
	private List<String> officials;

	SessionEditingFormFactory(Class<Group> domainType, SessionContent origin) {
		super(domainType);
		this.origin = origin;
	}

	@Override
	public Group add(Group Group) {
		GroupRepository.add(Group);
		return Group;
	}

	@Override
	public Binder<Group> buildBinder(CrudOperation operation, Group domainObject) {
		return super.buildBinder(operation, domainObject);
	}

	@Override
	public String buildCaption(CrudOperation operation, Group domainObject) {
		String name = domainObject.getName();
		if (name == null || name.isEmpty()) {
			return Translator.translate("Group");
		} else {
			return Translator.translate("Group") + " " + domainObject.getName();
		}
	}

	@Override
	public Component buildFooter(CrudOperation operation, Group domainObject,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
	        Button... buttons) {
		return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
		        deleteButtonClickListener, false, buttons);
	}

	@Override
	public Component buildNewForm(CrudOperation operation, Group aFromDb, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		this.binder = buildBinder(null, aFromDb);
		Platform platform = aFromDb.getPlatform();
		List<Platform> allPlatforms = PlatformRepository.findAll();

		if (allPlatforms != null && allPlatforms.size() > 0) {
			aFromDb.setPlatform(allPlatforms.get(0));
		}

		Component footerLayout = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
		        updateButtonClickListener, deleteButtonClickListener, false);
		FlexLayout mainLayout = createTabSheets(footerLayout, allPlatforms);
		this.binder.readBean(aFromDb);

		this.platformField.setValue(platform);

		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, Group domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		return super.buildOperationButton(operation, domainObject, gridCallBackAction);
	}

	@Override
	public void delete(Group ageGroup) {
		GroupRepository.delete(ageGroup);
	}

	@Override
	public Collection<Group> findAll() {
		// will not be called, handled by the grid.
		return null;
	}

	@Override
	public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
		return super.setErrorLabel(validationStatus, showErrorOnFields);
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Group update(Group ageGroup) {
		Group saved = GroupRepository.save(ageGroup);
		// logger.trace("saved {}", saved.getCategories().get(0).longDump());
		this.origin.closeDialog();
		// origin.highlightResetButton();
		return saved;
	}

	// @Override
	// public TextField defineOperationTrigger(CrudOperation operation, Group domainObject,
	// ComponentEventListener<ClickEvent<Button>> action) {
	// return super.defineOperationTrigger(operation, domainObject, action);
	// }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		this.binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

	private void addRuler(FormLayout formLayout) {
		Paragraph hr11 = new Paragraph();
		hr11.add("\u0020");
		hr11.add(new Hr());
		formLayout.add(hr11);
		formLayout.setColspan(hr11, 2);
	}

	private FlexLayout createTabSheets(Component footer, List<Platform> allPlatforms) {
		TabSheet ts = new TabSheet();
		
		// Map TechnicalOfficial fields correctly
		officials = TechnicalOfficialRepository.findAll().stream()
		        .map(to -> (to.getLastName() != null ? to.getLastName() : "")
		                + (to.getFirstName() != null ? ", " + to.getFirstName() : ""))
		        .filter(name -> !name.isBlank())
		        .sorted()
		        .toList();

		FormLayout groupLayout = sessionLayout(allPlatforms);
		FormLayout officialsLayout = officialsLayout();
		FormLayout supportLayout = supportLayout();
		FormLayout juryLayout = juryLayout();

		VerticalLayout content = new VerticalLayout(new Div(),
		        groupLayout);
		content.setHeight(HEIGHT);
		ts.add(Translator.translate("Group"),
		        content);

		VerticalLayout content2 = new VerticalLayout(new Div(),
		        officialsLayout);
		content2.setHeight(HEIGHT);
		ts.add(Translator.translate("Officials"),
		        content2);

		VerticalLayout content3 = new VerticalLayout(new Div(),
		        supportLayout);
		content3.setHeight(HEIGHT);
		ts.add(Translator.translate("Support"),
		        content3);

		VerticalLayout content4 = new VerticalLayout(new Div(),
		        juryLayout);
		content4.setHeight(HEIGHT);
		ts.add(Translator.translate("Jury"),
		        content4);

		FlexLayout mainLayout = new FlexLayout(ts, footer);
		mainLayout.setFlexDirection(FlexDirection.COLUMN);
		mainLayout.setWidth("60rem");

		mainLayout.setFlexGrow(1.0D, ts);

		return mainLayout;
	}

	private FormLayout sessionLayout(List<Platform> allPlatforms) {
		FormLayout formLayout = new FormLayout();
		TextField nameField = new TextField(Translator.translate("Name"));
		formLayout.add(nameField);
		int maxLength = 16;
		this.binder.forField(nameField)
		        .withValidator(
		                new StringLengthValidator(Translator.translate("CodeMustBeShort", maxLength), 1, maxLength))
		        .bind(Group::getName, Group::setName);

		this.platformField = new ComboBox<>(Translator.translate("Platform"));
		this.platformField.setSizeUndefined();

		ListDataProvider<Platform> dataProvider = new ListDataProvider<>(allPlatforms);
		this.platformField.setItems(dataProvider);

		this.platformField.setItemLabelGenerator(Platform::getName);
		this.platformField.setClearButtonVisible(true);
		formLayout.add(this.platformField);
		this.binder.forField(this.platformField).bind(Group::getPlatform, Group::setPlatform);

		TextField descriptionField = new TextField(Translator.translate("Group.Description"));
		descriptionField.setSizeFull();
		formLayout.add(descriptionField);
		formLayout.setColspan(descriptionField, 2);
		this.binder.forField(descriptionField)
		        .withNullRepresentation("")
		        .bind(Group::getDescription, Group::setDescription);

		addRuler(formLayout);

		LocalDateTimePicker weighInTimeField = new LocalDateTimePicker();
		weighInTimeField.setLabel(Translator.translate("WeighInTime"));
		formLayout.add(weighInTimeField);
		this.binder.forField(weighInTimeField)
		        .bind(Group::getWeighInTime, Group::setWeighInTime);

		LocalDateTimePicker competitionTimeField = new LocalDateTimePicker();
		competitionTimeField.setLabel(Translator.translate("StartTime"));
		formLayout.add(competitionTimeField);
		this.binder.forField(competitionTimeField)
		        .bind(Group::getCompetitionTime, Group::setCompetitionTime);

		weighInTimeField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			if (competitionTimeField.getValue() == null) {
				competitionTimeField.setValue(e.getValue().plusHours(2));
			}
		});
		competitionTimeField.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			if (weighInTimeField.getValue() == null) {
				weighInTimeField.setValue(e.getValue().minusHours(2));
			}
		});

		addRuler(formLayout);
		Checkbox mastersCheckbox = new Checkbox();
		mastersCheckbox.setLabel(Translator.translate("Competition.mastersStartOrder"));
		this.binder.forField(mastersCheckbox)
		        .bind(Group::isMasters, Group::setMasters);
		formLayout.addFormItem(mastersCheckbox,Translator.translate("Competition.masters"));

		addRuler(formLayout);
		NumberField breakDurationField = new NumberField(Translator.translate("CJ_BreakDuration"));
		breakDurationField.setPlaceholder(Translator.translate("CJ_BreakDurationPlaceHolder"));
		breakDurationField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
		formLayout.add(breakDurationField);

		// Add a custom converter to handle Integer values and null representation
		binder.forField(breakDurationField).withConverter(
		        new Converter<Double, Integer>() {
			        @Override
			        public Result<Integer> convertToModel(Double value, ValueContext context) {
				        if (value == null) {
					        return Result.ok(null);
				        }
				        return Result.ok(value.intValue());
			        }

			        @Override
			        public Double convertToPresentation(Integer value, ValueContext context) {
				        return value == null ? null : value.doubleValue();
			        }
		        }).bind(Group::getCleanJerkBreakDuration, Group::setCleanJerkBreakDuration);

		return formLayout;
	}

	private FormLayout juryLayout() {
		FormLayout juryLayout = new FormLayout();

		ComboBox<String> jury1 = createOfficialComboBox("JuryPresident");
		juryLayout.add(jury1);
		this.binder.forField(jury1)
		        .withNullRepresentation("")
		        .bind(Group::getJury1, Group::setJury1);

		ComboBox<String> jury2 = createOfficialComboBox("Jury2");
		juryLayout.add(jury2);
		this.binder.forField(jury2)
		        .withNullRepresentation("")
		        .bind(Group::getJury2, Group::setJury2);

		ComboBox<String> jury3 = createOfficialComboBox("Jury3");
		juryLayout.add(jury3);
		this.binder.forField(jury3)
		        .withNullRepresentation("")
		        .bind(Group::getJury3, Group::setJury3);

		ComboBox<String> jury4 = createOfficialComboBox("Jury4");
		juryLayout.add(jury4);
		this.binder.forField(jury4)
		        .withNullRepresentation("")
		        .bind(Group::getJury4, Group::setJury4);

		ComboBox<String> jury5 = createOfficialComboBox("Jury5");
		juryLayout.add(jury5);
		this.binder.forField(jury5)
		        .withNullRepresentation("")
		        .bind(Group::getJury5, Group::setJury5);
		
		ComboBox<String> reserveJury = createOfficialComboBox("ReserveJury");
		juryLayout.add(reserveJury);
		this.binder.forField(reserveJury)
		        .withNullRepresentation("")
		        .bind(Group::getReserveJury, Group::setReserveJury);

		addRuler(juryLayout);
		ComboBox<String> doctor = createOfficialComboBox("Doctor");
		juryLayout.add(doctor);
		this.binder.forField(doctor)
		        .withNullRepresentation("")
		        .bind(Group::getDoctor, Group::setDoctor);

		ComboBox<String> doctor2 = createOfficialComboBox("Doctor2", "Doctor");
		juryLayout.add(doctor2);
		this.binder.forField(doctor2)
		        .withNullRepresentation("")
		        .bind(Group::getDoctor2, Group::setDoctor2);

		return juryLayout;
	}

	// private Stream<String> queryTechnicalOfficials(Optional<String> filter, long limit, long offset) {
	// return TechnicalOfficialRepository.findAll().stream().map(to -> to.getLastName() + " " + to.getFirstName())
	// .filter(item -> !filter.isPresent() || item.contains(filter.get())).skip(offset).limit(limit);
	// }

	ComboBox<String> createOfficialComboBox(String label) {
		return createOfficialComboBox(label, null);
	}

	ComboBox<String> createOfficialComboBox(String label, String fallbackLabel) {
		String translated = Translator.translate(label);
		if ((translated == null || translated.isBlank() || translated.equals(label)) && fallbackLabel != null) {
			String fallbackTranslated = Translator.translate(fallbackLabel);
			if (fallbackTranslated != null && !fallbackTranslated.isBlank()) {
				translated = fallbackTranslated;
			}
		}
		if (translated == null || translated.isBlank()) {
			translated = label;
		}
		ComboBox<String> box = new ComboBox<>(translated);
		box.setAllowCustomValue(true);
		box.addCustomValueSetListener(e -> box.setValue(e.getDetail()));
		box.setItems(officials);
		box.setClearButtonVisible(true);
		return box;
	}

	private FormLayout officialsLayout() {
		FormLayout officialsLayout = new FormLayout();

		ComboBox<String> announcer = createOfficialComboBox("Announcer");
		officialsLayout.add(announcer);
		this.binder.forField(announcer)
		        .withNullRepresentation("")
		        .bind(Group::getAnnouncer, Group::setAnnouncer);

		ComboBox<String> timeKeeper = createOfficialComboBox("Timekeeper");
		officialsLayout.add(timeKeeper);
		this.binder.forField(timeKeeper)
		        .withNullRepresentation("")
		        .bind(Group::getTimeKeeper, Group::setTimeKeeper);

		ComboBox<String> marshall = createOfficialComboBox("Marshall");
		officialsLayout.add(marshall);
		this.binder.forField(marshall)
		        .withNullRepresentation("")
		        .bind(Group::getMarshall, Group::setMarshall);

		ComboBox<String> marshal2 = createOfficialComboBox("Marshal2");
		officialsLayout.add(marshal2);
		this.binder.forField(marshal2)
		        .withNullRepresentation("")
		        .bind(Group::getMarshal2, Group::setMarshal2);

		ComboBox<String> technicalController = createOfficialComboBox("TechnicalController");
		officialsLayout.add(technicalController);
		this.binder.forField(technicalController)
		        .withNullRepresentation("")
		        .bind(Group::getTechnicalController, Group::setTechnicalController);

		ComboBox<String> technicalController2 = createOfficialComboBox("TechnicalController2");
		officialsLayout.add(technicalController2);
		this.binder.forField(technicalController2)
		        .withNullRepresentation("")
		        .bind(Group::getTechnicalController2, Group::setTechnicalController2);

		addRuler(officialsLayout);

		ComboBox<String> referee1 = createOfficialComboBox("Referee1");
		officialsLayout.add(referee1);
		this.binder.forField(referee1)
		        .withNullRepresentation("")
		        .bind(Group::getReferee1, Group::setReferee1);

		ComboBox<String> referee2 = createOfficialComboBox("Referee2");
		officialsLayout.add(referee2);
		this.binder.forField(referee2)
		        .withNullRepresentation("")
		        .bind(Group::getReferee2, Group::setReferee2);

		ComboBox<String> referee3 = createOfficialComboBox("Referee3");
		officialsLayout.add(referee3);
		this.binder.forField(referee3)
		        .withNullRepresentation("")
		        .bind(Group::getReferee3, Group::setReferee3);
		
		ComboBox<String> reserveReferee = createOfficialComboBox("ReserveReferee");
		officialsLayout.add(reserveReferee);
		this.binder.forField(reserveReferee)
		        .withNullRepresentation("")
		        .bind(Group::getReserve, Group::setReserve);

		addRuler(officialsLayout);
		return officialsLayout;
	}

	private FormLayout supportLayout() {
		FormLayout supportLayout = new FormLayout();

		ComboBox<String> weighIn1 = createOfficialComboBox("Weighin1");
		supportLayout.add(weighIn1);
		this.binder.forField(weighIn1)
		        .withNullRepresentation("")
		        .bind(Group::getWeighIn1, Group::setWeighIn1);

		ComboBox<String> weighIn2 = createOfficialComboBox("Weighin2");
		supportLayout.add(weighIn2);
		this.binder.forField(weighIn2)
		        .withNullRepresentation("")
		        .bind(Group::getWeighIn2, Group::setWeighIn2);

		addRuler(supportLayout);

		ComboBox<String> competitionSecretary = createOfficialComboBox("CompetitionSecretary");
		supportLayout.add(competitionSecretary);
		this.binder.forField(competitionSecretary)
		        .withNullRepresentation("")
		        .bind(Group::getCompetitionSecretary, Group::setCompetitionSecretary);

		ComboBox<String> competitionSecretary2 = createOfficialComboBox("CompetitionSecretary2", "CompetitionSecretary");
		supportLayout.add(competitionSecretary2);
		this.binder.forField(competitionSecretary2)
		        .withNullRepresentation("")
		        .bind(Group::getCompetitionSecretary2, Group::setCompetitionSecretary2);

		addRuler(supportLayout);

		ComboBox<String> tis1 = createOfficialComboBox("TIS1");
		supportLayout.add(tis1);
		this.binder.forField(tis1)
		        .withNullRepresentation("")
		        .bind(Group::getTis1, Group::setTis1);

		ComboBox<String> tis2 = createOfficialComboBox("TIS2");
		supportLayout.add(tis2);
		this.binder.forField(tis2)
		        .withNullRepresentation("")
		        .bind(Group::getTis2, Group::setTis2);

		addRuler(supportLayout);
		return supportLayout;
	}

}