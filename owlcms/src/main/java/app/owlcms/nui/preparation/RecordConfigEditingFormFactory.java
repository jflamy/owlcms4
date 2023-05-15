package app.owlcms.nui.preparation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.binder.Binder;

import app.owlcms.components.fields.StringGridField;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
public class RecordConfigEditingFormFactory extends OwlcmsCrudFormFactory<RecordConfig> {

	private StringGridField orderingField;

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

		setBinder(buildBinder(operation, comp));

		FormLayout recordsOrderLayout = recordOrderForm();
		FormLayout recordsAvailableLayout = recordAvailableForm();

		Component footer = this.buildFooter(operation, comp, cancelButtonClickListener,
		        c -> {
			        this.update(comp);
		        }, deleteButtonClickListener, false);

		TabSheet ts = new TabSheet();
		ts.setWidthFull();
		ts.add(Translator.translate("Records.ConfigurationTab"),
		        new VerticalLayout(
		                recordsOrderLayout,
		                separator(),
		                recordsAvailableLayout));

		VerticalLayout mainLayout = new VerticalLayout(
		        footer,
		        ts);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);

		binder.readBean(comp);
		return mainLayout;
	}

	private FormLayout recordAvailableForm() {
		Button clearNewRecords = new Button(Translator.translate("Preparation.ClearNewRecords"),
		        buttonClickEvent -> {
			        try {
				        RecordRepository.clearNewRecords();
			        } catch (IOException e) {
				        throw new RuntimeException(e);
			        }
		        });
		clearNewRecords.getElement().setProperty("title",
		        Translator.translate("Preparation.ClearNewRecordsExplanation"));
		
		FormLayout recordsAvailableLayout = createLayout();
		Component title = createTitle("Records.ClearAndLoadSection");
		recordsAvailableLayout.add(title);
		recordsAvailableLayout.setColspan(title, 2);
		recordsAvailableLayout.addFormItem(clearNewRecords, Translator.translate("Preparation.ClearNewRecords"));
		return recordsAvailableLayout;
	}

	private FormLayout recordOrderForm() {
		orderingField = new StringGridField();
		orderingField.setWidthFull();
		binder.forField(orderingField).bind(RecordConfig::getRecordOrder, RecordConfig::setRecordOrder);
		FormLayout recordsOrderLayout = createLayout();
		Component title = createTitle("Records.OrderingSection");
		recordsOrderLayout.add(title);
		recordsOrderLayout.setColspan(title, 2);
		recordsOrderLayout.addFormItem(orderingField, Translator.translate("Records.OrderingField"));
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
