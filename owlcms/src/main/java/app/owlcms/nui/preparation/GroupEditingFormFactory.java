/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.StringLengthValidator;

import app.owlcms.components.fields.LocalDateTimePicker;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GroupEditingFormFactory
        extends OwlcmsCrudFormFactory<Group>
        implements CustomFormFactory<Group> {

	private static final String HEIGHT = "37rem";
	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(GroupEditingFormFactory.class);
	private GroupContent origin;
	ComboBox<Platform> platformField;

	GroupEditingFormFactory(Class<Group> domainType, GroupContent origin) {
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

		FormLayout groupLayout = groupLayout(allPlatforms);
		FormLayout officialsLayout = officialsLayout();
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
		        juryLayout);
		content3.setHeight(HEIGHT);
		ts.add(Translator.translate("Jury"),
		        content3);

		FlexLayout mainLayout = new FlexLayout(ts, footer);
		mainLayout.setFlexDirection(FlexDirection.COLUMN);
		mainLayout.setWidth("60rem");

		mainLayout.setFlexGrow(1.0D, ts);

		return mainLayout;
	}

	private FormLayout groupLayout(List<Platform> allPlatforms) {
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

		return formLayout;
	}

	private FormLayout juryLayout() {
		FormLayout juryLayout = new FormLayout();
		TextField jury1 = new TextField(Translator.translate("JuryPresident"));
		juryLayout.add(jury1);
		this.binder.forField(jury1)
		        .withNullRepresentation("")
		        .bind(Group::getJury1, Group::setJury1);

		TextField jury2 = new TextField(Translator.translate("Jury2"));
		juryLayout.add(jury2);
		this.binder.forField(jury2)
		        .withNullRepresentation("")
		        .bind(Group::getJury2, Group::setJury2);

		TextField jury3 = new TextField(Translator.translate("Jury3"));
		juryLayout.add(jury3);
		this.binder.forField(jury3)
		        .withNullRepresentation("")
		        .bind(Group::getJury3, Group::setJury3);

		TextField jury4 = new TextField(Translator.translate("Jury4"));
		juryLayout.add(jury4);
		this.binder.forField(jury4)
		        .withNullRepresentation("")
		        .bind(Group::getJury4, Group::setJury4);

		TextField jury5 = new TextField(Translator.translate("Jury5"));
		juryLayout.add(jury5);
		this.binder.forField(jury5)
		        .withNullRepresentation("")
		        .bind(Group::getJury5, Group::setJury5);
		return juryLayout;
	}

	private FormLayout officialsLayout() {
		FormLayout officialsLayout = new FormLayout();

		TextField announcer = new TextField(Translator.translate("Announcer"));
		officialsLayout.add(announcer);
		this.binder.forField(announcer)
		        .withNullRepresentation("")
		        .bind(Group::getAnnouncer, Group::setAnnouncer);

		TextField timeKeeper = new TextField(Translator.translate("Timekeeper"));
		officialsLayout.add(timeKeeper);
		this.binder.forField(timeKeeper)
		        .withNullRepresentation("")
		        .bind(Group::getTimeKeeper, Group::setTimeKeeper);

		TextField marshall = new TextField(Translator.translate("Marshall"));
		officialsLayout.add(marshall);
		this.binder.forField(marshall)
		        .withNullRepresentation("")
		        .bind(Group::getMarshall, Group::setMarshall);

		TextField marshal2 = new TextField(Translator.translate("Marshal2"));
		officialsLayout.add(marshal2);
		this.binder.forField(marshal2)
		        .withNullRepresentation("")
		        .bind(Group::getMarshal2, Group::setMarshal2);

		TextField technicalController = new TextField(Translator.translate("TechnicalController"));
		officialsLayout.add(technicalController);
		this.binder.forField(technicalController)
		        .withNullRepresentation("")
		        .bind(Group::getTechnicalController, Group::setTechnicalController);

		TextField technicalController2 = new TextField(Translator.translate("TechnicalController2"));
		officialsLayout.add(technicalController2);
		this.binder.forField(technicalController2)
		        .withNullRepresentation("")
		        .bind(Group::getTechnicalController2, Group::setTechnicalController2);

		addRuler(officialsLayout);

		TextField weighIn1 = new TextField(Translator.translate("Weighin1"));
		officialsLayout.add(weighIn1);
		this.binder.forField(weighIn1)
		        .withNullRepresentation("")
		        .bind(Group::getWeighIn1, Group::setWeighIn1);

		TextField weighIn2 = new TextField(Translator.translate("Weighin2"));
		officialsLayout.add(weighIn2);
		this.binder.forField(weighIn2)
		        .withNullRepresentation("")
		        .bind(Group::getWeighIn2, Group::setWeighIn2);

		addRuler(officialsLayout);

		TextField referee1 = new TextField(Translator.translate("Referee1"));
		officialsLayout.add(referee1);
		this.binder.forField(referee1)
		        .withNullRepresentation("")
		        .bind(Group::getReferee1, Group::setReferee1);

		TextField referee2 = new TextField(Translator.translate("Referee2"));
		officialsLayout.add(referee2);
		this.binder.forField(referee2)
		        .withNullRepresentation("")
		        .bind(Group::getReferee2, Group::setReferee2);

		TextField referee3 = new TextField(Translator.translate("Referee3"));
		officialsLayout.add(referee3);
		this.binder.forField(referee3)
		        .withNullRepresentation("")
		        .bind(Group::getReferee3, Group::setReferee3);

		addRuler(officialsLayout);
		return officialsLayout;

	}

}