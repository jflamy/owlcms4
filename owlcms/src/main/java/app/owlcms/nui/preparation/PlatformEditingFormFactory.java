/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;

import javax.sound.sampled.Mixer;

import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.HasValue;

import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsFactory;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		if (property.equals("soundMixerName")) {
			field.addValueChangeListener(e -> {
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
		}
		super.bindField(field, property, propertyType, c);
	}
}