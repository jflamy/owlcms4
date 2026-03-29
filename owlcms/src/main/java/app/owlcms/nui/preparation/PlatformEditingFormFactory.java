/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sound.sampled.Mixer;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;

import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.i18n.Translator;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.sound.Speakers;

@SuppressWarnings("serial")
class PlatformEditingFormFactory extends OwlcmsCrudFormFactory<Platform> {
	PlatformEditingFormFactory(Class<Platform> domainType) {
		super(domainType);
	}

	@Override
	public Platform add(Platform platform) {
		platform.defaultPlates();
		PlatformRepository.save(platform);
		return platform;
	}

	@Override
	public void delete(Platform platform) {
		PlatformRepository.delete(platform);
	}

	@Override
	public Collection<Platform> findAll() {
		// implemented on grid
		return null;
	}

	@Override
	public Platform update(Platform platform) {
		Platform saved = PlatformRepository.save(platform);
		// Platform configuration changed (plates, weights) - send updated database to trackers
		// Always send compressed binary ZIP (70-80% smaller than JSON)
		WebSocketEventForwarder.sendDatabaseToAll();
		// Reload session on the FOP to ensure consistency after plate configuration changes
		// This triggers a full refresh of lifting order with new constraints
		FieldOfPlay fop = OwlcmsFactory.getFOPByName(saved.getName());
		if (fop != null && fop.getGroup() != null) {
			fop.fopEventPost(new FOPEvent.SwitchGroup(fop.getGroup(), this));
		}
		return saved;
	}

	@Override
	public Component buildNewForm(CrudOperation operation, Platform platform, boolean readOnly,
	        ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
	        ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		this.binder = buildBinder(operation, platform);

		Component footer = this.buildFooter(operation, platform, cancelButtonClickListener,
		        operationButtonClickListener, deleteButtonClickListener, true);

		FormLayout form = platformLayout(platform);
		VerticalLayout mainLayout = new VerticalLayout(form, footer);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		this.binder.readBean(platform);
		return mainLayout;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		super.bindField(field, property, propertyType, c);
	}

	private String duplicatePlatformMessage(String normalizedName) {
		return Translator.translate("PlatformNameAlreadyExists", normalizedName);
	}

	private FormLayout platformLayout(Platform editedPlatform) {
		FormLayout layout = new FormLayout();

		TextField nameField = new TextField(Translator.translate("PlatformName"));
		layout.add(nameField);
		this.binder.forField(nameField)
		        .withNullRepresentation("")
		        .withValidator((name, context) -> {
		        	String normalizedName = PlatformRepository.normalizeName(name);
		        	if (normalizedName == null || normalizedName.isBlank()) {
		        		return ValidationResult.ok();
		        	}

		        	Platform candidate = new Platform();
		        	candidate.setId(editedPlatform.getId());
		        	candidate.setName(normalizedName);
		        	if (PlatformRepository.hasDuplicateName(candidate)) {
		        		return ValidationResult.error(duplicatePlatformMessage(normalizedName));
		        	}
		        	return ValidationResult.ok();
		        })
		        .bind(p -> PlatformRepository.normalizeName(p.getName()),
		                (p, name) -> p.setName(PlatformRepository.normalizeName(name)));

		ComboBox<String> soundMixerField = new ComboBox<>(Translator.translate("Speakers"));
		List<String> outputNames = new ArrayList<>(Speakers.getOutputNames());
		outputNames.add(0, Translator.translate("UseBrowserSound"));
		soundMixerField.setItems(outputNames);
		soundMixerField.addValueChangeListener(e -> {
			List<Mixer> soundMixers = Speakers.getOutputs();
			for (Mixer curMixer : soundMixers) {
				if (curMixer.getMixerInfo().getName().equals(e.getValue())) {
					if (e.getOldValue() != null && !e.getValue().equals(e.getOldValue())) {
						Speakers.testSound(curMixer);
					}
					PlatformContent.logger.debug("testing mixer {}", curMixer.getMixerInfo().getName());
					break;
				}
			}
		});
		this.binder.forField(soundMixerField)
		        .bind(Platform::getSoundMixerName, Platform::setSoundMixerName);
		layout.add(soundMixerField);

		return layout;
	}
}