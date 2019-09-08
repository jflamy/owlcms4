/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package app.owlcms.relay.ui.common;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract base class for dialogs adding, editing or deleting items.
 *
 * Subclasses are expected to
 * <ul>
 * <li>add, during construction, the needed UI components to
 * {@link #getFormLayout()} and bind them using {@link #getBinder()}, as well
 * as</li>
 * <li>override {@link #confirmDelete()} to open the confirmation dialog with
 * the desired message (by calling
 * {@link #openConfirmationDialog(String, String, String)}.</li>
 * </ul>
 *
 * @param <T>
 *            the type of the item to be added, edited or deleted
 */
public abstract class AbstractEditorDialog<T extends Serializable>
        extends Dialog {

    /**
     * The operations supported by this dialog. Delete is enabled when editing
     * an already existing item.
     */
    public enum Operation {
        ADD("New", "add", false), EDIT("Edit", "edit", true);

        private final String nameInTitle;
        private final String nameInText;
        private final boolean deleteEnabled;

        Operation(String nameInTitle, String nameInText,
                boolean deleteEnabled) {
            this.nameInTitle = nameInTitle;
            this.nameInText = nameInText;
            this.deleteEnabled = deleteEnabled;
        }

        public String getNameInTitle() {
            return nameInTitle;
        }

        public String getNameInText() {
            return nameInText;
        }

        public boolean isDeleteEnabled() {
            return deleteEnabled;
        }
    }

    private final H3 titleField = new H3();
    private final Button saveButton = new Button("Save");
    private final Button cancelButton = new Button("Cancel");
    private final Button deleteButton = new Button("Delete");
    private Registration registrationForSave;
    private Registration saveShortcutRegistration;
    private Registration deleteShortcutRegistration;

    private final FormLayout formLayout = new FormLayout();
    private final HorizontalLayout buttonBar = new HorizontalLayout(saveButton,
            cancelButton, deleteButton);

    private Binder<T> binder = new Binder<>();
    private T currentItem;

    private final ConfirmationDialog<T> confirmationDialog = new ConfirmationDialog<>();

    private final String itemType;
    private final BiConsumer<T, Operation> itemSaver;
    private final Consumer<T> itemDeleter;

    /**
     * Constructs a new instance.
     *
     * @param itemType
     *            The readable name of the item type
     * @param itemSaver
     *            Callback to save the edited item
     * @param itemDeleter
     *            Callback to delete the edited item
     */
    protected AbstractEditorDialog(String itemType,
            BiConsumer<T, Operation> itemSaver, Consumer<T> itemDeleter) {
        this.itemType = itemType;
        this.itemSaver = itemSaver;
        this.itemDeleter = itemDeleter;

        initTitle();
        initFormLayout();
        initButtonBar();
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
    }

    private void initTitle() {
        add(titleField);
    }

    private void initFormLayout() {
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("25em", 2));
        Div div = new Div(formLayout);
        div.addClassName("has-padding");
        add(div);
    }

    private void initButtonBar() {
        saveButton.setAutofocus(true);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addClickListener(e -> close());
        deleteButton.addClickListener(e -> deleteClicked());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttonBar.setClassName("buttons");
        buttonBar.setSpacing(true);
        add(buttonBar);
    }

    /**
     * Gets the form layout, where additional components can be added for
     * displaying or editing the item's properties.
     *
     * @return the form layout
     */
    protected final FormLayout getFormLayout() {
        return formLayout;
    }

    /**
     * Gets the binder.
     *
     * @return the binder
     */
    protected final Binder<T> getBinder() {
        return binder;
    }

    /**
     * Gets the item currently being edited.
     *
     * @return the item currently being edited
     */
    protected final T getCurrentItem() {
        return currentItem;
    }

    /**
     * Opens the given item for editing in the dialog.
     *
     * @param item
     *            The item to edit; it may be an existing or a newly created
     *            instance
     * @param operation
     *            The operation being performed on the item
     */
    public final void open(T item, Operation operation) {
        currentItem = item;
        titleField.setText(operation.getNameInTitle() + " " + itemType);
        if (registrationForSave != null) {
            registrationForSave.remove();
        }
        registrationForSave = saveButton
                .addClickListener(e -> saveClicked(operation));
        binder.readBean(currentItem);

        deleteButton.setEnabled(operation.isDeleteEnabled());

        enableShortcuts();

        open();
    }

    private void saveClicked(Operation operation) {
        boolean isValid = binder.writeBeanIfValid(currentItem);

        if (isValid) {
            itemSaver.accept(currentItem, operation);
            close();
        } else {
            BinderValidationStatus<T> status = binder.validate();
        }
    }

    private void deleteClicked() {
        if (confirmationDialog.getElement().getParent() == null) {
            getUI().ifPresent(ui -> ui.add(confirmationDialog));
        }
        confirmDelete();
    }

    protected abstract void confirmDelete();

    /**
     * Opens the confirmation dialog before deleting the current item.
     *
     * The dialog will display the given title and message(s), then call
     * {@link #deleteConfirmed(Serializable)} if the Delete button is clicked.
     *
     * @param title
     *            The title text
     * @param message
     *            Detail message (optional, may be empty)
     * @param additionalMessage
     *            Additional message (optional, may be empty)
     */
    protected final void openConfirmationDialog(String title, String message,
            String additionalMessage) {
        disableShortcuts();
        confirmationDialog.open(title, message, additionalMessage, "Delete",
                true, getCurrentItem(), this::deleteConfirmed,
                this::deleteCancelled);
    }

    /**
     * Removes the {@code item} from the backend and close the dialog.
     *
     * @param item
     *            the item to delete
     */
    protected void doDelete(T item) {
        itemDeleter.accept(item);
        close();
    }

    @Override
    public void close() {
        super.close();
        disableShortcuts();
    }

    private void deleteConfirmed(T item) {
        doDelete(item);
    }

    private void deleteCancelled() {
        enableShortcuts();
    }

    private void enableShortcuts() {
        disableShortcuts();
        saveShortcutRegistration = saveButton.addClickShortcut(Key.ENTER);
        if (deleteButton.isEnabled()) {
            deleteShortcutRegistration =
                    deleteButton.addClickShortcut(Key.DELETE);
        }
    }

    private void disableShortcuts() {
        if (saveShortcutRegistration != null) {
            saveShortcutRegistration.remove();
            saveShortcutRegistration = null;
        }
        if (deleteShortcutRegistration != null) {
            deleteShortcutRegistration.remove();
            deleteShortcutRegistration = null;
        }
    }
}
