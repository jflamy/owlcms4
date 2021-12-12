/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.crudui;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormFactory;
import org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.dom.ClassList;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

/**
 * A factory for creating OwlcmsCrudForm objects.
 *
 * @param <T> the generic type
 */
@SuppressWarnings("serial")
public abstract class OwlcmsCrudFormFactory<T> extends DefaultCrudFormFactory<T>
        implements CrudFormFactory<T>, CrudListener<T> {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsCrudFormFactory.class);
    static {
        logger.setLevel(Level.INFO);
    }

    protected ResponsiveStep[] responsiveSteps;
    protected Label errorLabel;
    private boolean valid = false;
    protected TextField operationTrigger;
    protected ClickEvent<Button> operationTriggerEvent;

    /**
     * Instantiates a new Form Factory
     *
     * We add a delete button capability to the CrudUI forms.
     *
     * @param domainType the domain type
     */
    public OwlcmsCrudFormFactory(Class<T> domainType) {
        super(domainType);
        init();
    }

    /**
     * Instantiates a new owlcms crudGrid form factory.
     *
     * @param domainType      the domain type
     * @param responsiveSteps the responsive steps
     */
    public OwlcmsCrudFormFactory(Class<T> domainType, ResponsiveStep... responsiveSteps) {
        super(domainType, responsiveSteps);
        this.responsiveSteps = responsiveSteps;
        init();
    }

    /**
     * @see org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, boolean, com.vaadin.flow.component.ComponentEventListener,
     *      com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    public Component buildNewForm(CrudOperation operation, T domainObject, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener) {
        return buildNewForm(operation, domainObject, readOnly, cancelButtonClickListener, operationButtonClickListener,
                null);
    }

    /**
     * Form with a Delete button
     *
     * @param operation                    the operation
     * @param domainObject                 the domain object
     * @param readOnly                     the read only
     * @param cancelButtonClickListener    the cancel button click listener
     * @param operationButtonClickListener the update button click listener
     * @param deleteButtonClickListener    the delete button click listener
     * @return the component
     */
    @SuppressWarnings("rawtypes")
    public Component buildNewForm(CrudOperation operation, T domainObject, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
        FormLayout formLayout = new FormLayout();
        formLayout.setSizeFull();
        if (this.responsiveSteps != null) {
            formLayout.setResponsiveSteps(this.responsiveSteps);
        }

        List<HasValueAndElement> fields = buildFields(operation, domainObject, readOnly);
        fields.stream().forEach(field -> formLayout.getElement().appendChild(field.getElement()));

        Component footerLayout = this.buildFooter(operation, domainObject, cancelButtonClickListener,
                operationButtonClickListener, deleteButtonClickListener, true, buttons);

        errorLabel = new Label();
        HorizontalLayout labelWrapper = new HorizontalLayout(errorLabel);
        labelWrapper.addClassName("errorMessage");
        labelWrapper.setWidthFull();
        labelWrapper.setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout mainLayout = new VerticalLayout(formLayout, labelWrapper, footerLayout);
        mainLayout.setFlexGrow(1, formLayout);
        mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        return mainLayout;
    }

    public Button doBuildButton(CrudOperation operation) {
        String caption = buttonCaptions.get(operation);
        Icon icon = buttonIcons.get(operation);
        Button button = icon != null ? new Button(caption, icon) : new Button(caption);
        if (buttonStyleNames.containsKey(operation)) {
            buttonStyleNames.get(operation).stream().filter(styleName -> styleName != null)
                    .forEach(styleName -> button.addClassName(styleName));
        }
        if (buttonThemes.containsKey(operation)) {
            button.getElement().setAttribute("theme", buttonThemes.get(operation));
        }
        return button;
    }

    /**
     * Utility method to avoid unreadable cast (Class<? extends HasValueAndElement<?, ?>>) when using WrappedTextField
     * subclasses
     *
     * @see org.vaadin.crudui.form.AbstractCrudFormFactory#setFieldType(java.lang.String, java.lang.Class)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setFieldType(String property, Class class1) {
        super.setFieldType(property, class1);
    }

    public void setValidationStatusHandler(boolean showErrorsOnFields) {
        binder.setValidationStatusHandler((s) -> {
            setValid(!s.hasErrors());
            if (showErrorsOnFields) {
                s.notifyBindingValidationStatusHandlers();
            }
            if (!isValid()) {
                logger.debug("validationStatusHandler updateFieldErrors={} {}", showErrorsOnFields,
                        LoggerUtils.whereFrom());
                if (errorLabel != null) {
                    setErrorLabel(s, showErrorsOnFields);
                }
            } else {
                if (errorLabel != null) {
                    setErrorLabel(s, showErrorsOnFields);
                    errorLabel.setVisible(false);
                }
            }
        });
    }

    /**
     * Creates a new dialog that executes the original listener after asking for confirmation
     *
     * @param operation
     * @param domainObject
     * @param clickListener
     * @return
     */
    protected Dialog buildConfirmDialog(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> clickListener) {
        Dialog dialog = new Dialog();

        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        H3 messageLabel = new H3(
                Translator.translate("Delete") + " " + domainObject.toString() + Translator.translate("Question"));

        // create a new delete button for the confirm dialog
        Button confirmButton = doBuildButton(operation);
        confirmButton.addClickListener(click -> {
            try {
                clickListener.onComponentEvent(click);
            } catch (Exception e) {
                showError(operation, e);
            }
            dialog.close();
        });
        Button cancelButton = new Button(Translator.translate("Cancel"), event -> {
            dialog.close();
        });
        dialog.add(new VerticalLayout(messageLabel, new HorizontalLayout(confirmButton, cancelButton)));
        return dialog;
    }

    /**
     * Added support for a delete button
     *
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildOperationButton(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener)
     */
    protected Button buildDeleteButton(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        if (gridCallBackAction == null) {
            return null;
        }
        Button button = doBuildButton(operation);
        button.addClickListener(e -> buildConfirmDialog(operation, domainObject, gridCallBackAction).open());
        return button;
    }

    /**
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildFooter(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener,
     *      com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    protected Component buildFooter(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack) {

        return this.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack);
    }

    /**
     * Footer with a Delete button and optional additional buttons
     *
     * Also adds optionnaly adds a shortcut so enter submits.
     *
     * @param operation                 the operation
     * @param cancelButtonClickListener the cancel button click listener
     * @param shortcutEnter             true if ENTER will trigger shortcut
     * @param domainObject              the domain object
     * @param postOperationCallBack     what to do after the object is created/updated
     * @param deleteButtonClickListener the delete button click listener
     * @return the component
     */
    protected Component buildFooter(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
            Button... buttons) {

        Button operationButton = null;
        if (operation == CrudOperation.UPDATE) {
            operationButton = buildOperationButton(CrudOperation.UPDATE, domainObject, postOperationCallBack);
        } else if (operation == CrudOperation.ADD) {
            operationButton = buildOperationButton(CrudOperation.ADD, domainObject, postOperationCallBack);
        }
        Button deleteButton = buildDeleteButton(CrudOperation.DELETE, domainObject, deleteButtonClickListener);
        Button cancelButton = buildCancelButton(cancelButtonClickListener);

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidth("100%");
        footerLayout.setSpacing(true);
        footerLayout.setPadding(false);

        if (deleteButton != null && operation != CrudOperation.ADD) {
            footerLayout.add(deleteButton);
        }

        Label spacer = new Label();

        footerLayout.add(spacer, operationTrigger);

        if (buttons != null) {
            for (Button b : buttons) {
                footerLayout.add(b);
            }
        }

        if (cancelButton != null) {
            footerLayout.add(cancelButton);
        }

        if (operationButton != null) {
            footerLayout.add(operationButton);
            if (operation == CrudOperation.UPDATE && shortcutEnter) {
                ShortcutRegistration reg = operationButton.addClickShortcut(Key.ENTER);
                reg.allowBrowserDefault();
            }
        }
        footerLayout.setFlexGrow(1.0, spacer);
        return footerLayout;
    }

    /**
     * Special button that uses an auxilliary field focus trigger to perform the operation.
     *
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildOperationButton(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener)
     */
    @Override
    protected Button buildOperationButton(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        if (gridCallBackAction == null) {
            return null;
        }
        Button button = doBuildButton(operation);

        // workaround for keyboard shortcut bug; the add/update takes place in the
        // trigger callback. The trigger then calls the gridCallBackAction
        operationTrigger = defineOperationTrigger(operation, domainObject, gridCallBackAction);
        ComponentEventListener<ClickEvent<Button>> listener = event -> {
            logger.debug("{} clicked", operation);
            operationTriggerEvent = event;
            operationTrigger.focus();
        };
        button.addClickListener(listener);

        return button;
    }

    /**
     * Workaround for the fact that ENTER as keyboard shortcut prevents the value being typed from being set in the
     * underlying object.
     *
     * i.e. Typing TAB followed by ENTER works (because tab causes ON_BLUR), but ENTER alone doesn't.
     *
     * So we cause ENTER to move focus, which forces an ON_BLUR, and we do the processing in the focus handler.
     *
     * @param operation
     * @param action
     *
     * @param gridLayout
     */
    protected TextField defineOperationTrigger(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        TextField updateTrigger = new TextField();
        updateTrigger.setReadOnly(true);
        updateTrigger.setTabIndex(-1);
        updateTrigger.addFocusListener((f) -> {
            performOperationAndCallback(operation, domainObject, action);
        });
        // field must visible and added to the layout for focus() to work, so we hide it
        // brutally
        updateTrigger.getStyle().set("z-index", "-10");
        return updateTrigger;
    }

    protected boolean isValid() {
        return valid;
    }

    protected void performOperationAndCallback(CrudOperation operation, T domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallback) {
        setValid(binder.writeBeanIfValid(domainObject));
        if (isValid()) {
            if (operation == CrudOperation.ADD) {
                logger.debug("adding {} {}", System.identityHashCode(domainObject), domainObject);
                this.add(domainObject);
                gridCallback.onComponentEvent(operationTriggerEvent);
            } else if (operation == CrudOperation.UPDATE) {
                logger.debug("updating 	{}", domainObject);
                this.update(domainObject);
                gridCallback.onComponentEvent(operationTriggerEvent);
            } else if (operation == CrudOperation.DELETE) {
                logger.debug("deleting 	{}", domainObject);
                this.delete(domainObject);
                gridCallback.onComponentEvent(operationTriggerEvent);
            }
        } else {
            logger.debug("not valid {}", domainObject);
        }
    }

    /**
     * Force correcting one error at a time
     *
     * @param validationStatus
     * @param showErrorOnFields if true, vaadin displays the errors
     * @return
     */
    protected boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
        logger.debug("{} validations", this.getClass().getSimpleName());
        boolean hasErrors = validationStatus.getFieldValidationErrors().size() > 0;
        boolean showInLabel = !showErrorOnFields;

        StringBuilder sb = new StringBuilder();
        for (BindingValidationStatus<?> ve : validationStatus.getFieldValidationErrors()) {
            HasValue<?, ?> field = ve.getField();
            if (showInLabel) {
                // error message is only shown on label, we highlight the classes ourselves
                ClassList fieldClasses = ((Component) field).getElement().getClassList();
                fieldClasses.clear();
                fieldClasses.set("error", true);
            }
            if (field instanceof TextField) {
                ((TextField) field).setAutoselect(true);
            }
            if (field instanceof Focusable) {
                ((Focusable<?>) field).focus();
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            String message = ve.getMessage().orElse(Translator.translate("Error"));
            sb.append(message);
        }
        for (ValidationResult ve : validationStatus.getBeanValidationErrors()) {
            showInLabel = true;
            if (sb.length() > 0) {
                sb.append(Translator.translate("Semicolon"));
            }
            String message = ve.getErrorMessage();
            sb.append(message);
        }
        if (showInLabel) {
            String message = sb.toString();
            errorLabel.setVisible(true);
            errorLabel.getElement().setProperty("innerHTML", message);
            errorLabel.getClassNames().set("errorMessage", true);
        } else {
            errorLabel.setVisible(false);
        }
        return hasErrors;
    }

    protected void setValid(boolean valid) {
        this.valid = valid;
    }

    private void init() {
        setButtonCaption(CrudOperation.READ, Translator.translate("Ok"));
        setButtonCaption(CrudOperation.ADD, Translator.translate("Add"));
        setButtonCaption(CrudOperation.UPDATE, Translator.translate("Update"));
        setButtonCaption(CrudOperation.DELETE, Translator.translate("Delete"));
        cancelButtonCaption = Translator.translate("Cancel");
        validationErrorMessage = Translator.translate("PleaseFix");
    }
}
