/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class RecordEditingFormFactory
        extends OwlcmsCrudFormFactory<RecordEvent>
        implements CustomFormFactory<RecordEvent> {

	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(RecordEditingFormFactory.class);
	private RecordContent origin;
	private TextField recordFederationField;
	private TextField recordNameField;
	private TextField ageGrpField;
	private ComboBox<Gender> genderField;
	private ComboBox<Ranking> recordLiftField;
	private IntegerField ageGrpLowerField;
	private IntegerField ageGrpUpperField;
	private IntegerField bwCatLowerField;
	private IntegerField bwCatUpperField;
	private Paragraph categoryChangeWarning;
	private CrudOperation currentOperation;
	private RecordEvent originalRecordSnapshot;
	ComboBox<Platform> platformField;

	public RecordEditingFormFactory(Class<RecordEvent> domainType, RecordContent recordContent) {
		super(domainType);
		this.origin = recordContent;
	}

	@Override
	public RecordEvent add(RecordEvent recordEvent) {
		recordEvent.syncBodyWeightCategoryString();
		return RecordRepository.save(recordEvent);
	}

	@Override
	public Binder<RecordEvent> buildBinder(CrudOperation operation, RecordEvent domainObject) {
		return super.buildBinder(operation, domainObject);
	}

	@Override
	public String buildCaption(CrudOperation operation, RecordEvent domainObject) {
		if (operation.equals(CrudOperation.ADD)) {
			return Translator.translate("Add") + " " + Translator.translate("RecordEvent.Title");
		} else if (operation.equals(CrudOperation.UPDATE)) {
			String name = domainObject.getName();
			if (name != null && !name.isEmpty()) {
				return Translator.translate("Update") + " " + Translator.translate("RecordEvent.Title") + " " + name;
			} else {
				return Translator.translate("Update") + " " + Translator.translate("RecordEvent.Title");
			}
		} else if (operation.equals(CrudOperation.DELETE)) {
			String name = domainObject.getName();
			if (name != null && !name.isEmpty()) {
				return Translator.translate("Delete") + " " + Translator.translate("RecordEvent.Title") + " " + name;
			} else {
				return Translator.translate("Delete") + " " + Translator.translate("RecordEvent.Title");
			}
		}
		return super.buildCaption(operation, domainObject);
	}

	@Override
	public Component buildFooter(CrudOperation operation, RecordEvent domainObject,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
	        Button... buttons) {
		return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
		        deleteButtonClickListener, false, buttons);
	}

	@Override
	public Component buildNewForm(CrudOperation operation, RecordEvent aFromDb, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		this.currentOperation = operation;
		this.originalRecordSnapshot = operation == CrudOperation.UPDATE ? copyRecordDefinition(aFromDb) : null;

		this.binder = buildBinder(operation, aFromDb);

		Component footer = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
		        updateButtonClickListener, deleteButtonClickListener, true);

		this.categoryChangeWarning = createCategoryChangeWarning();
		Component warningArea = createWarningArea();
		Component form = recordEventLayout();
		var mainLayout = new VerticalLayout(warningArea, form, footer);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		this.binder.readBean(aFromDb);
		refreshPreviewState();
		return mainLayout;
	}

	@Override
	public Button buildOperationButton(CrudOperation operation, RecordEvent domainObject,
	        ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
		return super.buildOperationButton(operation, domainObject, gridCallBackAction);
	}

	@Override
	public void delete(RecordEvent ageGroup) {
		RecordRepository.delete(ageGroup);
	}

	@Override
	public Collection<RecordEvent> findAll() {
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
	public RecordEvent update(RecordEvent ageGroup) {
		ageGroup.syncBodyWeightCategoryString();
		RecordEvent saved = RecordRepository.save(ageGroup, this.originalRecordSnapshot);
		// logger.trace("saved {}", saved.getCategories().get(0).longDump());
		this.origin.closeDialog();
		// origin.highlightResetButton();
		return saved;
	}

	public Component recordEventLayout() {
		FormLayout recordIdentificationLayout = recordIdentificationForm();
		FormLayout recordDetailsLayout = recordDetailsForm();
		FormLayout eventLayout = eventForm();

		VerticalLayout formLayout = new VerticalLayout(
		        recordIdentificationLayout, separator(),
		        recordDetailsLayout, separator(),
		        eventLayout);
		formLayout.setMargin(false);
		formLayout.setPadding(false);

		return formLayout;
	}

	private FormLayout recordIdentificationForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("RecordEvent.IdentificationTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		TextField recordFederationField = new TextField(Translator.translate("RecordEvent.Federation"));
		this.recordFederationField = recordFederationField;
		layout.add(recordFederationField);
		this.binder.forField(recordFederationField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getRecordFederation, RecordEvent::setRecordFederation);

		TextField recordNameField = new TextField(Translator.translate("RecordEvent.Name"));
		this.recordNameField = recordNameField;
		layout.add(recordNameField);
		this.binder.forField(recordNameField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getRecordName, RecordEvent::setRecordName);

		TextField ageGrpField = new TextField(Translator.translate("AgeGroup"));
		this.ageGrpField = ageGrpField;
		layout.add(ageGrpField);
		this.binder.forField(ageGrpField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getAgeGrp, RecordEvent::setAgeGrp);

		ComboBox<Gender> genderField = new ComboBox<>(Translator.translate("Gender"));
		this.genderField = genderField;
		genderField.setItems(Gender.values());
		layout.add(genderField);
		this.binder.forField(genderField)
		        .bind(RecordEvent::getGender, RecordEvent::setGender);

		// Create collapsible section for age group and bodyweight detailed information
		FormLayout collapsibleLayout = createLayout();

		IntegerField ageGrpLowerField = new IntegerField(Translator.translate("RecordEvent.AgeLower"));
		this.ageGrpLowerField = ageGrpLowerField;
		collapsibleLayout.add(ageGrpLowerField);
		this.binder.forField(ageGrpLowerField)
		        .bind(RecordEvent::getAgeGrpLower, RecordEvent::setAgeGrpLower);

		IntegerField ageGrpUpperField = new IntegerField(Translator.translate("RecordEvent.AgeUpper"));
		this.ageGrpUpperField = ageGrpUpperField;
		collapsibleLayout.add(ageGrpUpperField);
		this.binder.forField(ageGrpUpperField)
		        .bind(RecordEvent::getAgeGrpUpper, RecordEvent::setAgeGrpUpper);

		IntegerField bwCatLowerField = new IntegerField(Translator.translate("RecordEvent.BWLower"));
		this.bwCatLowerField = bwCatLowerField;
		collapsibleLayout.add(bwCatLowerField);
		this.binder.forField(bwCatLowerField)
		        .bind(RecordEvent::getBwCatLower, RecordEvent::setBwCatLower);

		IntegerField bwCatUpperField = new IntegerField(Translator.translate("RecordEvent.BWUpper"));
		this.bwCatUpperField = bwCatUpperField;
		collapsibleLayout.add(bwCatUpperField);
		this.binder.forField(bwCatUpperField)
		        .bind(RecordEvent::getBwCatUpper, RecordEvent::setBwCatUpper);

		registerPreviewRefresh(recordFederationField);
		registerPreviewRefresh(recordNameField);
		registerPreviewRefresh(ageGrpField);
		registerPreviewRefresh(genderField);
		registerPreviewRefresh(ageGrpLowerField);
		registerPreviewRefresh(ageGrpUpperField);
		registerPreviewRefresh(bwCatLowerField);
		registerPreviewRefresh(bwCatUpperField);

		Details ageGroupDetails = new Details(Translator.translate("RecordEvent.AgeBodyweightDetails"), collapsibleLayout);
		ageGroupDetails.getStyle().set("margin-right", "-1em");
		ageGroupDetails.getStyle().set("padding-right", "0");
		ageGroupDetails.setWidthFull();

		layout.add(ageGroupDetails);
		layout.setColspan(ageGroupDetails, 2);

		return layout;
	}

	private FormLayout recordDetailsForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("RecordEvent.DetailsTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		ComboBox<Ranking> recordLiftField = new ComboBox<>(Translator.translate("RecordEvent.Lift"));
		this.recordLiftField = recordLiftField;
		recordLiftField.setItems(Ranking.SNATCH, Ranking.CLEANJERK, Ranking.TOTAL);
		recordLiftField.setItemLabelGenerator(ranking -> Translator.translate("Record." + ranking));
		layout.add(recordLiftField);
		this.binder.forField(recordLiftField)
		        .bind(RecordEvent::getRecordLift, RecordEvent::setRecordLift);
		registerPreviewRefresh(recordLiftField);

		NumberField recordValueField = new NumberField(Translator.translate("RecordEvent.Value"));
		layout.add(recordValueField);
		this.binder.forField(recordValueField)
		        .bind(RecordEvent::getRecordValue, RecordEvent::setRecordValue);

		TextField athleteNameField = new TextField(Translator.translate("RecordEvent.AthleteName"));
		layout.add(athleteNameField);
		this.binder.forField(athleteNameField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getAthleteName, RecordEvent::setAthleteName);

		TextField nationField = new TextField(Translator.translate("RecordEvent.Nation"));
		layout.add(nationField);
		this.binder.forField(nationField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getNation, RecordEvent::setNation);

		// Create collapsible section for athlete birth information
		FormLayout birthInfoLayout = createLayout();

		DatePicker athleteBirthDateField = new DatePicker(Translator.translate("RecordEvent.AthleteBirthDate"));
		birthInfoLayout.add(athleteBirthDateField);
		this.binder.forField(athleteBirthDateField)
		        .bind(RecordEvent::getBirthDate, RecordEvent::setBirthDate);

		IntegerField athleteBirthYearField = new IntegerField(Translator.translate("RecordEvent.AthleteBirthYear"));
		birthInfoLayout.add(athleteBirthYearField);
		this.binder.forField(athleteBirthYearField)
		        .bind(RecordEvent::getBirthYear, RecordEvent::setBirthYear);

		Details birthDetails = new Details(Translator.translate("RecordEvent.AthleteBirthDetails"), birthInfoLayout);
		birthDetails.getStyle().set("margin-right", "-1em");
		birthDetails.getStyle().set("padding-right", "0");
		birthDetails.setWidthFull();

		layout.add(birthDetails);
		layout.setColspan(birthDetails, 2);

		return layout;
	}

	private FormLayout eventForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("RecordEvent.EventTitle");
		layout.add(title);
		layout.setColspan(title, 2);

		DatePicker recordDateField = new DatePicker(Translator.translate("RecordEvent.Date"));
		layout.add(recordDateField);
		this.binder.forField(recordDateField)
		        .bind(RecordEvent::getRecordDate, RecordEvent::setRecordDate);

		IntegerField recordYearField = new IntegerField(Translator.translate("RecordEvent.Year"));
		layout.add(recordYearField);
		this.binder.forField(recordYearField)
		        .bind(RecordEvent::getRecordYear, RecordEvent::setRecordYear);

		TextField eventField = new TextField(Translator.translate("RecordEvent.Event"));
		layout.add(eventField);
		this.binder.forField(eventField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getEvent, RecordEvent::setEvent);

		TextField eventLocationField = new TextField(Translator.translate("RecordEvent.EventLocation"));
		layout.add(eventLocationField);
		this.binder.forField(eventLocationField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getEventLocation, RecordEvent::setEventLocation);

		TextField groupNameStringField = new TextField(Translator.translate("RecordEvent.Group"));
		layout.add(groupNameStringField);
		this.binder.forField(groupNameStringField)
		        .withNullRepresentation("")
		        .bind(RecordEvent::getGroupNameString, RecordEvent::setGroupNameString);

		return layout;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", 2, LabelsPosition.TOP));
		return layout;
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0");
		return title;
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}

	private Paragraph createCategoryChangeWarning() {
		Paragraph warning = new Paragraph();
		warning.setVisible(false);
		warning.getStyle().set("margin", "0");
		warning.getStyle().set("padding", "0.75em 1em");
		warning.getStyle().set("background-color", "var(--lumo-warning-color-10pct)");
		warning.getStyle().set("border-left", "4px solid var(--lumo-warning-color)");
		warning.getStyle().set("color", "var(--lumo-body-text-color)");
		warning.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
		warning.getStyle().set("box-sizing", "border-box");
		warning.setWidthFull();
		return warning;
	}

	private Component createWarningArea() {
		FormLayout layout = createLayout();
		layout.add(this.categoryChangeWarning);
		layout.setColspan(this.categoryChangeWarning, 2);
		return layout;
	}

	private RecordEvent copyRecordDefinition(RecordEvent source) {
		if (source == null) {
			return null;
		}
		RecordEvent copy = new RecordEvent();
		copy.setId(source.getId());
		copy.setRecordFederation(source.getRecordFederation());
		copy.setRecordName(source.getRecordName());
		copy.setAgeGrp(source.getAgeGrp());
		copy.setGender(source.getGender());
		copy.setRecordLift(source.getRecordLift());
		copy.setAgeGrpLower(source.getAgeGrpLower());
		copy.setAgeGrpUpper(source.getAgeGrpUpper());
		copy.setBwCatLower(source.getBwCatLower());
		copy.setBwCatUpper(source.getBwCatUpper());
		copy.setGroupNameString(source.getGroupNameString());
		return copy;
	}

	private void registerPreviewRefresh(HasValue<?, ?> field) {
		field.addValueChangeListener(event -> refreshPreviewState());
	}

	private void refreshPreviewState() {
		refreshDialogCaption();
		refreshCategoryChangeWarning();
	}

	private void refreshDialogCaption() {
		if (this.origin == null || this.currentOperation == null || this.currentOperation == CrudOperation.DELETE) {
			return;
		}
		this.origin.updateDialogCaption(buildPreviewCaption());
	}

	private String buildPreviewCaption() {
		String titlePrefix = this.currentOperation == CrudOperation.ADD ? Translator.translate("Add") : Translator.translate("Update");
		String previewName = buildPreviewName();
		if (previewName == null || previewName.isBlank()) {
			return titlePrefix + " " + Translator.translate("RecordEvent.Title");
		}
		return titlePrefix + " " + Translator.translate("RecordEvent.Title") + " " + previewName;
	}

	private void refreshCategoryChangeWarning() {
		if (this.categoryChangeWarning == null) {
			return;
		}
		boolean showWarning = shouldShowCategoryChangeWarning();
		this.categoryChangeWarning.setVisible(showWarning);
		if (showWarning) {
			this.categoryChangeWarning.setText(Translator.translate("RecordEvent.WarningCurrentCategoryChange"));
		}
	}

	private boolean shouldShowCategoryChangeWarning() {
		if (this.currentOperation != CrudOperation.UPDATE || this.originalRecordSnapshot == null) {
			return false;
		}
		return RecordRepository.wouldRedefineCurrentOfficialRecord(this.originalRecordSnapshot, buildPreviewDefinition());
	}

	private RecordEvent buildPreviewDefinition() {
		RecordEvent preview = copyRecordDefinition(this.originalRecordSnapshot);
		if (preview == null) {
			return null;
		}
		if (this.recordFederationField != null) {
			preview.setRecordFederation(this.recordFederationField.getValue());
		}
		if (this.recordNameField != null) {
			preview.setRecordName(this.recordNameField.getValue());
		}
		if (this.ageGrpField != null) {
			preview.setAgeGrp(this.ageGrpField.getValue());
		}
		if (this.genderField != null) {
			preview.setGender(this.genderField.getValue());
		}
		if (this.recordLiftField != null) {
			preview.setRecordLift(this.recordLiftField.getValue());
		}
		if (this.ageGrpLowerField != null && this.ageGrpLowerField.getValue() != null) {
			preview.setAgeGrpLower(this.ageGrpLowerField.getValue());
		}
		if (this.ageGrpUpperField != null && this.ageGrpUpperField.getValue() != null) {
			preview.setAgeGrpUpper(this.ageGrpUpperField.getValue());
		}
		if (this.bwCatLowerField != null && this.bwCatLowerField.getValue() != null) {
			preview.setBwCatLower(this.bwCatLowerField.getValue());
		}
		if (this.bwCatUpperField != null && this.bwCatUpperField.getValue() != null) {
			preview.setBwCatUpper(this.bwCatUpperField.getValue());
		}
		return preview;
	}

	private String buildPreviewName() {
		StringBuilder preview = new StringBuilder();

		if (this.recordNameField != null && this.recordNameField.getValue() != null && !this.recordNameField.getValue().isBlank()) {
			preview.append(this.recordNameField.getValue().trim());
		}
		if (this.recordLiftField != null && this.recordLiftField.getValue() != null) {
			appendPreviewPart(preview, Translator.translate("Record." + this.recordLiftField.getValue()));
		}
		if (this.ageGrpField != null && this.ageGrpField.getValue() != null && !this.ageGrpField.getValue().isBlank()) {
			appendPreviewPart(preview, this.ageGrpField.getValue().trim());
		}
		if (this.bwCatLowerField != null && this.bwCatUpperField != null && this.bwCatUpperField.getValue() != null) {
			RecordEvent previewRecord = new RecordEvent();
			previewRecord.setBwCatLower(this.bwCatLowerField.getValue() != null ? this.bwCatLowerField.getValue() : 0);
			previewRecord.setBwCatUpper(this.bwCatUpperField.getValue());
			appendPreviewPart(preview, previewRecord.getBwCatString());
		}

		return preview.toString();
	}

	private void appendPreviewPart(StringBuilder preview, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		if (preview.length() > 0) {
			preview.append(' ');
		}
		preview.append(value);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		this.binder.forField(field);
		super.bindField(field, property, propertyType, c);
	}

}