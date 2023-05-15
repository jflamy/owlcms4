package app.owlcms.nui.preparation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;

import app.owlcms.components.fields.GridField;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.spreadsheet.JXLSExportRecords;

@SuppressWarnings("serial")
public class RecordConfigEditingFormFactory extends OwlcmsCrudFormFactory<RecordConfig> {

	private class LoadedRecordsField extends GridField<RecordEvent> {

		private Runnable callback;

		public LoadedRecordsField(Runnable callback) {
			super(false);
			this.callback = callback;
		}

		@Override
		protected void createColumns() {
			grid.addColumn(RecordEvent::getRecordName);
			grid.addColumn(RecordEvent::getAgeGrp).setTextAlign(ColumnTextAlign.CENTER);
			grid.addColumn(RecordEvent::getRecordFederation).setTextAlign(ColumnTextAlign.CENTER);
			grid.addColumn(RecordEvent::getFileName).setAutoWidth(true);
			grid.addComponentColumn(re -> createClearButton(re)).setTextAlign(ColumnTextAlign.CENTER);
		}

		private Button createClearButton(RecordEvent re) {
			Button button = new Button("Clear");
			button.addClickListener(e -> {
				RecordRepository.clearByExample(re);
				this.setPresentationValue(RecordConfig.getCurrent().getLoadedFiles());
				callback.run();
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
	public Collection<RecordConfig> findAll() {
		return Arrays.asList(RecordConfig.getCurrent());
	}

	@Override
	public RecordConfig add(RecordConfig domainObjectToAdd) {
		throw new UnsupportedOperationException("RecordConfig is a Singleton, cannot add");
	}

	@Override
	public RecordConfig update(RecordConfig domainObjectToUpdate) {
		return JPAService.runInTransaction(em -> em.merge(domainObjectToUpdate));
	}

	@Override
	public void delete(RecordConfig domainObjectToDelete) {
		throw new UnsupportedOperationException("RecordConfig is a Singleton, cannot delete");
	}

	public Component buildNewForm(CrudOperation operation, RecordConfig comp, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		this.recordConfig = comp;
		setBinder(buildBinder(operation, comp));

		FormLayout recordsOrderLayout = recordOrderForm();
		FormLayout provisionalLayout = provisionalForm();
		FormLayout officialLayout = officialForm();


		TabSheet ts = new TabSheet();
		ts.setWidthFull();
		ts.add(Translator.translate("Records.ConfigurationTab"),
		        new VerticalLayout(
		                recordsOrderLayout,
		                separator(),
		                officialLayout));
		ts.add(Translator.translate("Records.ProvisionalSection"),
		        new VerticalLayout(
		                provisionalLayout));
		

		VerticalLayout mainLayout = new VerticalLayout(
		        ts);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		binder.readBean(comp);
		return mainLayout;
	}

	private FormLayout provisionalForm() {
		Button clearNewRecords = new Button(Translator.translate("Preparation.ClearNewRecords"),
		        buttonClickEvent -> {
			        try {
				        RecordRepository.clearNewRecords();
			        } catch (IOException e) {
				        throw new RuntimeException(e);
			        }
		        });

		FormLayout recordsAvailableLayout = createLayout();
		Component title = createTitle("Records.ProvisionalSection");

		recordsAvailableLayout.add(title);
		recordsAvailableLayout.setColspan(title, 2);
		Div newRecords = DownloadButtonFactory.createDynamicXLSDownloadButton("records",
		        Translator.translate("Results.NewRecords"), new JXLSExportRecords(UI.getCurrent()));
		recordsAvailableLayout.addFormItem(newRecords, Translator.translate("Results.NewRecords"));
		recordsAvailableLayout.addFormItem(clearNewRecords,
		        Translator.translate("Preparation.ClearNewRecordsExplanation"));

		return recordsAvailableLayout;
	}

	private FormLayout officialForm() {
		Button clearNewRecords = new Button(Translator.translate("Records.ClearOfficialRecords"),
		        buttonClickEvent -> {
			        try {
				        RecordRepository.clearOfficialRecords();
				        UI.getCurrent().getPage().reload();
			        } catch (IOException e) {
				        throw new RuntimeException(e);
			        }
		        });
		MemoryBuffer receiver = new MemoryBuffer();
		Upload uploadRecords = new Upload(receiver);
		uploadRecords.addSucceededListener(e -> {
			RecordDefinitionReader.readInputStream(receiver.getInputStream(), receiver.getFileName());
			UI.getCurrent().getPage().reload();
		});

		FormLayout recordsAvailableLayout = createLayout();
		Component title = createTitle("Records.OfficialSection");

		loadedField = new LoadedRecordsField(() -> {
			RecordConfig current = RecordConfig.getCurrent();
			current.addMissing(RecordRepository.findAllRecordNames());
			ofBinding.read(current);
		});
		loadedField.setWidthFull();
		binder.forField(loadedField).bind(RecordConfig::getLoadedFiles, RecordConfig::setLoadedFiles);

		recordsAvailableLayout.add(title);
		recordsAvailableLayout.setColspan(title, 2);
		
		FormItem ur = recordsAvailableLayout.addFormItem(uploadRecords,
		        Translator.translate("Records.UploadOfficialFile"));
		recordsAvailableLayout.setColspan(ur, 1);
		FormItem cni = recordsAvailableLayout.addFormItem(clearNewRecords,
		        Translator.translate("Records.ClearOfficialRecordsExplanation"));
		recordsAvailableLayout.setColspan(cni, 1);

		
		FormItem lfi = recordsAvailableLayout.addFormItem(loadedField,
		        Translator.translate("Records.LoadedOfficialFiles"));
		recordsAvailableLayout.setColspan(lfi, 2);
		return recordsAvailableLayout;
	}

	private FormLayout recordOrderForm() {
		Button update = new Button(Translator.translate("Update"));
		update.addClickListener((e) -> this.update(recordConfig));
		update.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		FormLayout recordsOrderLayout = createLayout();
		Component title = createTitle("Records.OrderingSection");
		recordsOrderLayout.add(title);
		recordsOrderLayout.setColspan(title, 2);

		orderingField = new GridField<String>(true);
		orderingField.setWidthFull();
		ofBinding = binder.forField(orderingField).bind(RecordConfig::getRecordOrder, RecordConfig::setRecordOrder);
		
		HorizontalLayout ordering = new HorizontalLayout(orderingField, update);

		recordsOrderLayout.addFormItem(ordering, Translator.translate("Records.OrderingField"));
		return recordsOrderLayout;
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0");
		return title;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
//        layout.setWidth("1024px");
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", 2, LabelsPosition.TOP));
		return layout;
	}

	private void setBinder(Binder<RecordConfig> buildBinder) {
		binder = buildBinder;
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.5em");
		hr.getStyle().set("margin-bottom", "1.0em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}
}
