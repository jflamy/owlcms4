package app.owlcms.athlete.views.addressform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.athlete.data.entity.Address;
import app.owlcms.athlete.data.service.AddressService;
import app.owlcms.athlete.views.main.MainView;

@Route(value = "address-form", layout = MainView.class)
@PageTitle("Address Form")
@CssImport("./styles/views/addressform/address-form-view.css")
public class AddressFormView extends Div {

    private TextField street = new TextField("Street address");
    private TextField postalCode = new TextField("Postal code");
    private TextField city = new TextField("City");
    private ComboBox<String> state = new ComboBox<>("State");
    private ComboBox<String> country = new ComboBox<>("Country");

    private Button cancel = new Button("Cancel");
    private Button save = new Button("Save");

    private Binder<Address> binder = new Binder<>(Address.class);

    public AddressFormView(AddressService addressService) {
        addClassName("address-form-view");

        add(createTitle());
        add(createFormLayout());
        add(createButtonLayout());

        binder.bindInstanceFields(this);

        clearForm();

        cancel.addClickListener(e -> clearForm());
        save.addClickListener(e -> {
            addressService.update(binder.getBean());
            Notification.show("Address stored.");
            clearForm();
        });
    }

    private Component createTitle() {
        return new H3("Address");
    }

    private Component createFormLayout() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(street, 2);
        postalCode.setPattern("\\d*");
        postalCode.setPreventInvalidInput(true);
        country.setItems("Country 1", "Country 2", "Country 3");
        state.setItems("State A", "State B", "State C", "State D");
        formLayout.add(postalCode, city, state, country);
        return formLayout;
    }

    private Component createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addClassName("button-layout");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save);
        buttonLayout.add(cancel);
        return buttonLayout;
    }

    private void clearForm() {
        this.binder.setBean(new Address());
    }

}
