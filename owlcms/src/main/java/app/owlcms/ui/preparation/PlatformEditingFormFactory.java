package app.owlcms.ui.preparation;

import java.util.Collection;
import java.util.List;

import javax.sound.sampled.Mixer;

import com.vaadin.flow.component.HasValue;

import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.sound.Speakers;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class PlatformEditingFormFactory extends OwlcmsCrudFormFactory<Platform> {
    PlatformEditingFormFactory(Class<Platform> domainType) {
        super(domainType);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        if (property.equals("soundMixerName")) {
            field.addValueChangeListener(e -> {
                List<Mixer> soundMixers = Speakers.getOutputs();
                for (Mixer curMixer : soundMixers) {
                    if (curMixer.getMixerInfo().getName().equals(e.getValue())) {
                        if (e.getOldValue() != null && ! e.getValue().equals(e.getOldValue())) Speakers.testSound(curMixer);
                        PlatformContent.logger.debug("testing mixer {}",curMixer.getMixerInfo().getName()); // LoggerUtils.stackTrace());
                        break;
                    }
                }
            });
        }
        super.bindField(field, property, propertyType);
    }

    @Override
    public Platform add(Platform platform) {
        PlatformRepository.save(platform);
        return platform;
    }

    @Override
    public Platform update(Platform platform) {
        return PlatformRepository.save(platform);
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
}