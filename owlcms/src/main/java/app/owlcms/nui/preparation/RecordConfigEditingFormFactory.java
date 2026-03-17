/*****************************import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;**********************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.ValidationException;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.fields.GridField;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.utils.URLUtils;

@SuppressWarnings("serial")
public class RecordConfigEditingFormFactory extends OwlcmsCrudFormFactory<RecordConfig> {

	private static final Logger logger = LoggerFactory.getLogger(RecordConfigEditingFormFactory.class);

	private class LoadedRecordsField extends GridField<RecordEvent> {

		private Runnable callback;

		public LoadedRecordsField(Runnable callback) {
			super(false, Translator.translate("Records.NoFiles", Translator.translate("Records.UploadButton")));
			this.callback = callback;
		}

		@Override
		protected void createColumns() {
			this.grid.addColumn(RecordEvent::getRecordName);
			this.grid.addColumn(RecordEvent::getAgeGrp).setTextAlign(ColumnTextAlign.CENTER);
			this.grid.addColumn(RecordEvent::getRecordFederation).setTextAlign(ColumnTextAlign.CENTER);
			this.grid.addColumn(RecordEvent::getFileName).setAutoWidth(true);

			// Header checkbox toggles all rows
			Checkbox headerCheckbox = new Checkbox();
			headerCheckbox.setAriaLabel(Translator.translate("Active"));
			headerCheckbox.addValueChangeListener(e -> {
				if (e.isFromClient()) {
					boolean newVal = e.getValue();
					RecordRepository.setActiveForAll(newVal);
					for (RecordEvent re : LoadedRecordsField.this.getValue()) {
						re.setActive(newVal);
					}
					this.grid.getDataProvider().refreshAll();
					this.callback.run();
				}
			});
			updateHeaderCheckboxState(headerCheckbox);

			NativeLabel activeLabel = new NativeLabel(Translator.translate("Active"));
			activeLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
			Div headerWrapper = new Div(activeLabel, headerCheckbox);
			headerWrapper.getStyle().set("display", "flex");
			headerWrapper.getStyle().set("flex-direction", "column");
			headerWrapper.getStyle().set("align-items", "center");

			this.grid.addComponentColumn(re -> createActiveCheckbox(re, headerCheckbox))
			        .setHeader(headerWrapper)
			        .setTextAlign(ColumnTextAlign.CENTER);
			this.grid.addComponentColumn(re -> createClearButton(re)).setTextAlign(ColumnTextAlign.CENTER);
		}

		private void updateHeaderCheckboxState(Checkbox headerCheckbox) {
			List<RecordEvent> items = LoadedRecordsField.this.getValue();
			if (items == null || items.isEmpty()) {
				headerCheckbox.setValue(false);
				headerCheckbox.setIndeterminate(false);
				return;
			}
			boolean allActive = items.stream().allMatch(RecordEvent::getActive);
			boolean noneActive = items.stream().noneMatch(RecordEvent::getActive);
			if (allActive) {
				headerCheckbox.setValue(true);
				headerCheckbox.setIndeterminate(false);
			} else if (noneActive) {
				headerCheckbox.setValue(false);
				headerCheckbox.setIndeterminate(false);
			} else {
				headerCheckbox.setIndeterminate(true);
			}
		}

		private Checkbox createActiveCheckbox(RecordEvent re, Checkbox headerCheckbox) {
			Checkbox checkbox = new Checkbox();
			checkbox.setValue(re.getActive());
			checkbox.addValueChangeListener(e -> {
				if (e.isFromClient()) {
					RecordRepository.setActiveForRecordSet(
					        re.getRecordFederation(), re.getRecordName(), re.getAgeGrp(), e.getValue());
					re.setActive(e.getValue());
					updateHeaderCheckboxState(headerCheckbox);
					this.callback.run();
				}
			});
			return checkbox;
		}

		private Button createClearButton(RecordEvent re) {
			Button button = new Button(Translator.translate("Clear"));
			button.addClickListener(e -> {
				RecordRepository.clearByExample(re);
				this.setPresentationValue(RecordConfig.getCurrent().getLoadedFiles());
				this.callback.run();
			});
			return button;
		}

	}

	private GridField<String> orderingField;
	private LoadedRecordsField loadedField;
	private Binding<RecordConfig, List<String>> ofBinding;
	private RecordConfig recordConfig;

	public RecordConfigEditingFormFactory(Class<RecordConfig> domainType) {
		super(domainType);
	}

	@Override
	public RecordConfig add(RecordConfig domainObjectToAdd) {
		throw new UnsupportedOperationException("RecordConfig is a Singleton, cannot add");
	}

	@Override
	public Component buildNewForm(CrudOperation operation, RecordConfig comp, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		this.recordConfig = comp;
		setBinder(buildBinder(operation, comp));

		FormLayout officialLayout = officialForm();

		VerticalLayout mainLayout;
		if (Config.getCurrent().isRecordRepository()) {
			officialLayout.getStyle().set("margin-top", "1em");
			mainLayout = new VerticalLayout(officialLayout);
		} else {
			FormLayout recordsOrderLayout = recordOrderForm();
			mainLayout = new VerticalLayout(
			        recordsOrderLayout,
			        separator(),
			        officialLayout);
		}
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		this.binder.readBean(comp);
		return mainLayout;
	}

	@Override
	public void delete(RecordConfig domainObjectToDelete) {
		throw new UnsupportedOperationException("RecordConfig is a Singleton, cannot delete");
	}

	@Override
	public Collection<RecordConfig> findAll() {
		return Arrays.asList(RecordConfig.getCurrent());
	}

	@Override
	public RecordConfig update(RecordConfig domainObjectToUpdate) {
		try {
			this.binder.writeBean(domainObjectToUpdate);
		} catch (ValidationException e) {
			throw new RuntimeException("Cannot update RecordConfig {}", e);
		}
		this.recordConfig = RecordConfig.setCurrent(domainObjectToUpdate);
		return this.recordConfig;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		// layout.setWidth("1024px");
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

	private FormLayout officialForm() {
		Button clearNewRecords = new Button(Translator.translate("RecordConfig.ClearAllRecords"),
		        buttonClickEvent -> {
			        ConfirmationDialog cd = new ConfirmationDialog(
			                Translator.translate("RecordConfig.ClearAllRecords"),
			                Translator.translate("RecordConfig.ClearAllRecordsWarning"),
			                null,
			                () -> {
				                try {
					                // Clear ALL records from the system
					                JPAService.runInTransaction(em -> {
						                int deletedCount = em.createQuery("DELETE FROM RecordEvent").executeUpdate();
						                logger.info("deleted {} record entries", deletedCount);
						                return null;
					                });
					                UI.getCurrent().getPage().reload();
				                } catch (Exception e) {
					                throw new RuntimeException(e);
				                }
			                });
			        cd.open();
		        });

		Button uploadButton = new Button(Translator.translate("Records.UploadButton"));
		uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		Locale capturedLocale = OwlcmsSession.getLocale();
		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
			List<String> errors = new RecordDefinitionReader(capturedLocale).readInputStream(new ByteArrayInputStream(bytes),
			        metadata.fileName());
			if (errors.isEmpty()) {
				UI.getCurrent().getPage().reload();
			} else {
				Pre errorsComponent = new Pre();
				errorsComponent.add(errors.stream().collect(Collectors.joining(System.lineSeparator())));
				Dialog d = new Dialog();
				Button okButton = new Button(Translator.translate("OK"),
				        x -> {
					        d.close();
					        UI.getCurrent().getPage().reload();
				        });
				d.add(errorsComponent);
				d.getFooter().add(okButton);
				d.open();
			}
		});
		
		Upload uploadRecords = new Upload(uploadHandler);
		uploadRecords.setUploadButton(uploadButton);
		uploadRecords.setDropLabel(new NativeLabel(Translator.translate("Records.UploadDropZone")));

		FormLayout recordsAvailableLayout = createLayout();
		Component title = createTitle("RecordConfig.UploadRecords");

		this.loadedField = new LoadedRecordsField(() -> {
			RecordConfig current = RecordConfig.getCurrent();
			current.addMissing(RecordRepository.findAllRecordNames());
			this.ofBinding.read(current);
		});
		this.loadedField.setWidthFull();
		this.binder.forField(this.loadedField).bind(RecordConfig::getLoadedFiles, RecordConfig::setLoadedFiles);

		recordsAvailableLayout.add(title);
		recordsAvailableLayout.setColspan(title, 2);

		FormItem ur = recordsAvailableLayout.addFormItem(uploadRecords,
		        Translator.translate("Records.UploadOfficialFile"));
		recordsAvailableLayout.setColspan(ur, 1);

		HorizontalLayout buttonTitle = new HorizontalLayout();
		Button editExportRecords = openInNewTabNoParam(RecordContent.class,
		        Translator.translate("RecordEvent.EditExportRecords"));
		editExportRecords.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Div div = new Div();
		H4 nativeLabel = new H4(Translator.translate("RecordConfig.LoadedRecords"));
		nativeLabel.getStyle().set("margin", "0");
		nativeLabel.getStyle().set("line-height", "1.2");
		clearNewRecords.getElement().setAttribute("title",
			Translator.translate("RecordConfig.ClearAllRecordsExplanation"));
		buttonTitle.add(nativeLabel,
		        div, clearNewRecords);
		buttonTitle.setSpacing(true);
		buttonTitle.setFlexGrow(1, div);
		buttonTitle.setAlignItems(Alignment.START);
		buttonTitle.setAlignSelf(Alignment.START, nativeLabel);
		// visual kludge
		buttonTitle.getElement().getStyle().set("margin-top", "2em");
		buttonTitle.getElement().getStyle().set("margin-right", "1em");
		buttonTitle.setWidthFull();

		recordsAvailableLayout.add(buttonTitle);
		recordsAvailableLayout.setColspan(buttonTitle, 2);
		recordsAvailableLayout.add(this.loadedField);
		recordsAvailableLayout.setColspan(this.loadedField, 2);
		return recordsAvailableLayout;
	}

	private FormLayout recordOrderForm() {
		Button update = new Button(Translator.translate("Records.UpdateDisplayOptions"));
		update.addClickListener((e) -> this.update(this.recordConfig));
		update.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		VerticalLayout updateContainer = new VerticalLayout(update);
		updateContainer.setAlignSelf(Alignment.END, update);

		FormLayout recordsOrderLayout = createLayout();
		Component title = createTitle("Records.DisplayOptions");
		recordsOrderLayout.add(title);
		recordsOrderLayout.setColspan(title, 1);

		recordsOrderLayout.add(updateContainer);

		this.orderingField = new GridField<>(true,
		        Translator.translate("Records.NoRecords", Translator.translate("Records.UploadButton")));
		this.ofBinding = this.binder.forField(this.orderingField).bind(RecordConfig::getRecordOrder,
		        RecordConfig::setRecordOrder);

		HorizontalLayout ordering = new HorizontalLayout(this.orderingField);
		ordering.setSizeUndefined();
		recordsOrderLayout.addFormItem(ordering, Translator.translate("Records.OrderingField"));

		recordsOrderLayout.add(new Paragraph());

		Checkbox showAllCategoriesField = new Checkbox();
		this.binder.forField(showAllCategoriesField).bind(RecordConfig::getShowAllCategoryRecords,
		        RecordConfig::setShowAllCategoryRecords);
		recordsOrderLayout.addFormItem(showAllCategoriesField, Translator.translate("Records.AllCategories"));

		Checkbox showAllFederationsField = new Checkbox();
		this.binder.forField(showAllFederationsField).bind(RecordConfig::getShowAllFederations,
		        RecordConfig::setShowAllFederations);
		recordsOrderLayout.addFormItem(showAllFederationsField, Translator.translate("Records.AllFederations"));

		return recordsOrderLayout;
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}

	private void setBinder(Binder<RecordConfig> buildBinder) {
		this.binder = buildBinder;
	}

	private <T extends Component> Button openInNewTabNoParam(Class<T> targetClass,
	        String label, Component... icon) {
		Button button = new Button(label);
		if (icon.length > 0 && icon[0] != null) {
			button.setIcon(icon[0]);
		}
		button.getElement().setAttribute("onClick", getWindowOpenerFromClassNoParam(targetClass));
		return button;
	}

	private <T extends Component> String getWindowOpenerFromClassNoParam(Class<T> targetClass) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		String name = fop == null ? "" : "_" + fop.getName();
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "','"
		        + targetClass.getSimpleName() + name + "')";
	}
}
