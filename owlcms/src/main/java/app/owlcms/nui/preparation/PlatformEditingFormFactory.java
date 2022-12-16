/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;

import javax.sound.sampled.Mixer;

import com.vaadin.flow.component.HasValue;

import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
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
        return PlatformRepository.save(platform);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
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
        super.bindField(field, property, propertyType);
    }
}