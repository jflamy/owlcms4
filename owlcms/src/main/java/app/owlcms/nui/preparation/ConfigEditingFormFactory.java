/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.provider.ListDataProvider;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.TimeZoneUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class ConfigEditingFormFactory
        extends OwlcmsCrudFormFactory<Config>
        implements CustomFormFactory<Config> {

    private String browserZoneId;

    private Logger logger = (Logger) LoggerFactory.getLogger(ConfigRepository.class);
    @SuppressWarnings("unused")
    private ConfigContent origin;

    ConfigEditingFormFactory(Class<Config> domainType, ConfigContent origin) {
        super(domainType);
        this.origin = origin;
    }

    @Override
    public Config add(Config config) {
        Config.setCurrent(config);
        return config;
    }

    @Override
    public Binder<Config> buildBinder(CrudOperation operation, Config domainObject) {
        return super.buildBinder(operation, domainObject);
    }

    @Override
    public String buildCaption(CrudOperation operation, Config config) {
        return Translator.translate("Config.Titles");
    }

    @Override
    public Component buildFooter(CrudOperation operation, Config domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
            Button... buttons) {
        return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
                deleteButtonClickListener, false, buttons);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Config domainObject, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener) {
        return this.buildNewForm(operation, domainObject, readOnly, cancelButtonClickListener,
                operationButtonClickListener, null);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Config config, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        binder = buildBinder(operation, config);

        FormLayout accessLayout = accessForm();
        FormLayout tzLayout = tzForm();
        FormLayout languageLayout = presentationForm();
        FormLayout publicResultsLayout = publicResultsForm();
        FormLayout localOverrideLayout = localOverrideForm();
        FormLayout translationLayout = translationForm();
        FormLayout featuresLayout = featuresForm();

        Component footer = this.buildFooter(operation, config, cancelButtonClickListener,
                c -> {
                    Config.setCurrent(config); // does a save
                    Config current = Config.getCurrent();
                    Locale defaultLocale = current.getDefaultLocale();
                    Translator.reset();
                    Translator.setForcedLocale(defaultLocale);
                    logger.debug("config locale {} {} {}", current.getDefaultLocale(),
                            defaultLocale, Translator.getForcedLocale());
                }, deleteButtonClickListener, false);

        VerticalLayout mainLayout = new VerticalLayout(
                footer,
                languageLayout, separator(),
                tzLayout, separator(),
                accessLayout, separator(),
                publicResultsLayout, separator(),
                localOverrideLayout, separator(),
                featuresLayout, separator(),
                translationLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);

        config.setSkipReading(false);
        binder.readBean(config);
        config.setSkipReading(true);
        return mainLayout;
    }

    @Override
    public Button buildOperationButton(CrudOperation operation, Config domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        return super.buildOperationButton(operation, domainObject, gridCallBackAction);
    }

    @Override
    public TextField defineOperationTrigger(CrudOperation operation, Config domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        return super.defineOperationTrigger(operation, domainObject, action);
    }

    @Override
    public void delete(Config config) {
        ConfigRepository.delete(config);
    }

    @Override
    public Collection<Config> findAll() {
        // will not be called, handled by the grid.
        return null;
    }

    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
        return super.setErrorLabel(validationStatus, showErrorOnFields);
    }

    @Override
    public Config update(Config config) {
        try {
            config.setSkipReading(true);
            if (config.isClearZip()) {
                config.setLocalZipBlob(null);
                ResourceWalker.checkForLocalOverrideDirectory();
            }
            Config saved = Config.setCurrent(config);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignored
            }
            UI.getCurrent().getPage().reload();
            return saved;
        } finally {
            config.setSkipReading(false);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
        binder.forField(field);
        super.bindField(field, property, propertyType, c);
    }

    private FormLayout accessForm() {
        FormLayout configLayout = createLayout();
        Component title = createTitle("Config.AccessControlTitle");
        configLayout.add(title);
        configLayout.setColspan(title, 2);

        PasswordField passwordField = new PasswordField();
        passwordField.setWidthFull();
        configLayout.addFormItem(passwordField, Translator.translate("Config.PasswordOrPIN"));
        binder.forField(passwordField)
                .withNullRepresentation("")
                .bind(Config::getPinForField, Config::setPinForField);

        // configLayout.addFormItem(new Html("<br/>"), "");

        TextField accessListField = new TextField();
        accessListField.setWidthFull();
        configLayout.addFormItem(accessListField, Translator.translate("Config.AccessList"));
        binder.forField(accessListField)
                .withNullRepresentation("")
                .bind(Config::getIpAccessList, Config::setIpAccessList);

        PasswordField displayPasswordField = new PasswordField();
        displayPasswordField.setWidthFull();
        configLayout.addFormItem(displayPasswordField, Translator.translate("Config.DisplayPIN"));
        binder.forField(displayPasswordField)
                .withNullRepresentation("")
                .bind(Config::getDisplayPinForField, Config::setDisplayPinForField);

        // configLayout.addFormItem(new Html("<br/>"), "");

        TextField displayListField = new TextField();
        displayListField.setWidthFull();
        configLayout.addFormItem(displayListField, Translator.translate("Config.DisplayAccessList"));
        binder.forField(displayListField)
                .withNullRepresentation("")
                .bind(Config::getIpDisplayList, Config::setIpDisplayList);

        TextField backdoorField = new TextField();
        backdoorField.setWidthFull();
        configLayout.addFormItem(backdoorField, Translator.translate("Config.Backdoor"));
        binder.forField(backdoorField)
                .withNullRepresentation("")
                .bind(Config::getIpBackdoorList, Config::setIpBackdoorList);

        return configLayout;
    }

    private FormLayout createLayout() {
        FormLayout layout = new FormLayout();
//        layout.setWidth("1024px");
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

    private FormLayout featuresForm() {
        FormLayout layout = createLayout();
        Component title = createTitle("Config.FeatureSwitchesTitle");
        layout.add(title);
        layout.setColspan(title, 2);

        TextField featureSwitchesField = new TextField();
        featureSwitchesField.setWidthFull();
        FormItem fi = layout.addFormItem(featureSwitchesField, Translator.translate("Config.FeatureSwitchesLabel"));
        layout.setColspan(fi, 2);
        binder.forField(featureSwitchesField)
                .withNullRepresentation("")
                .bind(Config::getFeatureSwitches, Config::setFeatureSwitches);

        return layout;
    }

    private FormLayout localOverrideForm() {
        FormLayout layout = createLayout();
        Component title = createTitle("Config.ResourceOverride");
        layout.add(title);
        layout.setColspan(title, 2);

        ZipFileField accessListField = new ZipFileField();
        accessListField.setWidthFull();
        layout.addFormItem(accessListField, Translator.translate("Config.UploadLabel"));
        binder.forField(accessListField)
                .bind(Config::getLocalZipBlob, Config::setLocalZipBlob);

        byte[] localOverride = Config.getCurrent().getLocalZipBlob();
        if (localOverride == null) {
            localOverride = new byte[0];
        }
        Div downloadDiv = DownloadButtonFactory.createDynamicZipDownloadButton("resourcesOverride",
                Translator.translate("Config.Download"), localOverride);
        downloadDiv.setWidthFull();
        Optional<Component> downloadButton = downloadDiv.getChildren().findFirst();
        if (localOverride.length == 0) {
            downloadButton.ifPresent(c -> ((Button) c).setEnabled(false));
        }
        layout.addFormItem(downloadDiv, Translator.translate("Config.DownloadLabel"));

        Checkbox clearField = new Checkbox(Translator.translate("Config.ClearZip"));
        clearField.setWidthFull();
        layout.addFormItem(clearField, Translator.translate("Config.ClearZipLabel"));
        binder.forField(clearField)
                .bind(Config::isClearZip, Config::setClearZip);

        Checkbox localTemplatesField = new Checkbox(Translator.translate("Config.LocalTemplate"));
        localTemplatesField.setWidthFull();
        layout.addFormItem(localTemplatesField, Translator.translate("Config.LocalTemplateLabel"));
        binder.forField(localTemplatesField)
                .bind(Config::isLocalTemplatesOnly, Config::setLocalTemplatesOnly);

        return layout;
    }

    private FormLayout presentationForm() {
        FormLayout layout = createLayout();
        Component title = createTitle("Competition.presentationTitle");
        layout.add(title);
        layout.setColspan(title, 2);

        ComboBox<Locale> defaultLocaleField = new ComboBox<>();
        defaultLocaleField.setClearButtonVisible(true);
        defaultLocaleField.setItems(new ListDataProvider<>(Translator.getAllAvailableLocales()));
        defaultLocaleField.setItemLabelGenerator((locale) -> locale.getDisplayName(locale));
        binder.forField(defaultLocaleField).bind(Config::getDefaultLocale, Config::setDefaultLocale);
        layout.addFormItem(defaultLocaleField, Translator.translate("Competition.defaultLocale"));

        return layout;
    }

    private FormLayout publicResultsForm() {
        FormLayout layout = createLayout();
        Component title = createTitle("Config.PublicResultsTitle");
        layout.add(title);
        layout.setColspan(title, 2);

        TextField federationField = new TextField();
        federationField.setWidthFull();
        layout.addFormItem(federationField, Translator.translate("Config.publicResultsURL"));
        binder.forField(federationField)
                .withNullRepresentation("")
                .bind(Config::getPublicResultsURL, Config::setPublicResultsURL);

        PasswordField updateKey = new PasswordField();
        updateKey.setWidthFull();
        layout.addFormItem(updateKey, Translator.translate("Config.UpdateKey"));
        binder.forField(updateKey)
                .withNullRepresentation("")
                .bind(Config::getUpdatekey, Config::setUpdatekey);

        return layout;
    }

    private Hr separator() {
        Hr hr = new Hr();
        hr.getStyle().set("margin-top", "0.5em");
        hr.getStyle().set("margin-bottom", "1.0em");
        hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
        hr.getStyle().set("height", "2px");
        return hr;
    }

    private FormLayout translationForm() {
        FormLayout layout = createLayout();
        Component title = createTitle("Translation");
        layout.add(title);
        layout.setColspan(title, 2);

        Button resetTranslation = new Button(Translator.translate("reloadTranslation"),
                buttonClickEvent -> Translator.reset());
        layout.addFormItem(resetTranslation, Translator.translate("reloadTranslationInfo"));
        return layout;
    }

    private FormLayout tzForm() {

        FormLayout layout = createLayout();
        ComboBox<TimeZone> tzCombo = new ComboBox<>();
        tzCombo.setWidthFull();

        Component title = createTitle("Config.TZTitle");
        layout.add(title);
        layout.setColspan(title, 2);

        UnorderedList ulTZ = new UnorderedList();
        ListItem defaultTZ = new ListItem();
        ListItem browserTZ = new ListItem();
        Span browserTZText = new Span();
        Button browserTZButton = new Button("", (e) -> {
            tzCombo.setValue(browserZoneId != null ? TimeZone.getTimeZone(browserZoneId) : null);
        });
        browserTZ.add(browserTZText, browserTZButton);
        ListItem explainTZ = new ListItem();
        explainTZ.getElement().setProperty("innerHTML", Translator.translate("Config.TZExplain"));
        ulTZ.add(defaultTZ, browserTZ, explainTZ);
        layout.add(ulTZ);
        layout.setColspan(ulTZ, 2);

        layout.addFormItem(tzCombo, Translator.translate("Config.TZ_Selection"));

        List<TimeZone> tzList = TimeZoneUtils.allTimeZones();
        tzCombo.setItems(tzList);
        tzCombo.setItemLabelGenerator((tzone) -> TimeZoneUtils.toIdWithOffsetString(tzone));
        tzCombo.setClearButtonVisible(true);
        binder.forField(tzCombo)
                // .withNullRepresentation("Etc/GMT")
                .bind(Config::getTimeZone, Config::setTimeZone);

        PendingJavaScriptResult pendingResult = UI.getCurrent().getPage()
                .executeJs("return Intl.DateTimeFormat().resolvedOptions().timeZone");
        pendingResult.then(String.class, (res) -> {
            browserZoneId = res;
            String defZone = TimeZoneUtils.toIdWithOffsetString(TimeZone.getDefault());
            String browserZoneText = TimeZoneUtils.toIdWithOffsetString(TimeZone.getTimeZone(res));
            browserTZText.getElement().setProperty("innerHTML",
                    Translator.translate("Config.TZ_FromBrowser", browserZoneText) + "&nbsp;");
            browserTZButton.setText(browserZoneText);
            defaultTZ.setText(Translator.translate("Config.TZ_FromServer", defZone));
        });

        return layout;
    }

}