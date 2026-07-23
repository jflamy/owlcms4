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
import java.util.Objects;

import javax.sound.sampled.Mixer;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.nui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.lifting.TCContent;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.sound.Speakers;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/platforms", layout = OwlcmsLayout.class)
public class PlatformContent extends BaseContent implements CrudListener<Platform>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(PlatformContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<Platform> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private Platform draggedPlatform;

	/**
	 * Instantiates the Platform crudGrid.
	 */
	public PlatformContent() {
		OwlcmsCrudFormFactory<Platform> crudFormFactory = createFormFactory();
		GridCrud<Platform> crud = createGrid(crudFormFactory);
		// defineFilters(crudGrid);
		fillHW(crud, this);
	}

	@Override
	public Platform add(Platform domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public void delete(Platform domainObjectToDelete) {
		this.editingFormFactory.delete(domainObjectToDelete);
	}

	/**
	 * The refresh button on the toolbar
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Platform> findAll() {
		return PlatformRepository.findAll();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("EditPlatforms");
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("Preparation_Platforms");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public Platform update(Platform domainObjectToUpdate) {
		return this.editingFormFactory.update(domainObjectToUpdate);
	}

	/**
	 * The content and ordering of the editing form.
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
		crudFormFactory.setVisibleProperties("name", "soundMixerName");
		crudFormFactory.setFieldCaptions(Translator.translate("PlatformName"), Translator.translate("Speakers"));
		List<String> outputNames = Speakers.getOutputNames();
		outputNames.add(0, Translator.translate("UseBrowserSound"));
		crudFormFactory.setFieldProvider("soundMixerName", new OwlcmsComboBoxProvider<>(outputNames));
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Platform> createGrid(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
		Grid<Platform> grid = new Grid<>(Platform.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addComponentColumn(platform -> {
			Icon dragHandle = VaadinIcon.MENU.create();
			dragHandle.getStyle().set("color", "var(--lumo-secondary-text-color)");
			return dragHandle;
		}).setHeader("").setWidth("2.5em").setFlexGrow(0);

		PlatformGrid crud = new PlatformGrid(Platform.class, new OwlcmsGridLayout(Platform.class),
		        crudFormFactory, grid);
		grid.addComponentColumn(platform -> createNameField(platform, crud))
		        .setHeader(Translator.translate("Name")).setWidth("15em").setFlexGrow(0);
		grid.addColumn(new ComponentRenderer<>(p -> {
			Button technical = openInNewTab(TCContent.class, Translator.translate("PlatesCollarBarbell"), p.getName());
			// prevent grid row selection from triggering
			technical.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			return technical;
		})).setHeader(Translator.translate("PlatesCollarBarbell")).setAutoWidth(true).setFlexGrow(0)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addComponentColumn(platform -> createSoundMixerField(platform, crud))
		        .setHeader(Translator.translate("Speakers")).setWidth("30em").setFlexGrow(0);

		grid.setRowsDraggable(true);
		grid.addDragStartListener(event -> {
			this.draggedPlatform = event.getDraggedItems().get(0);
			grid.setDropMode(GridDropMode.BETWEEN);
		});
		grid.addDragEndListener(event -> {
			this.draggedPlatform = null;
			grid.setDropMode(null);
		});
		grid.addDropListener(event -> reorderPlatforms(event.getDropTargetItem().orElse(null), event.getDropLocation(), crud));
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(false);
		return crud;
	}

	private TextField createNameField(Platform platform, PlatformGrid crud) {
		TextField nameField = new TextField();
		nameField.setAriaLabel(Translator.translate("Name"));
		nameField.setValue(platform.getName() != null ? platform.getName() : "");
		nameField.setValueChangeMode(ValueChangeMode.ON_BLUR);
		nameField.setWidthFull();
		nameField.addValueChangeListener(event -> {
			if (!event.isFromClient()) {
				return;
			}

			String normalizedName = PlatformRepository.normalizeName(event.getValue());
			ValidationResult validation = PlatformEditingFormFactory.validateName(platform, normalizedName);
			if (validation.isError()) {
				nameField.setInvalid(true);
				nameField.setErrorMessage(validation.getErrorMessage());
				return;
			}

			nameField.setInvalid(false);
			if (!Objects.equals(platform.getName(), normalizedName)) {
				platform.setName(normalizedName);
				this.update(platform);
				crud.refreshGrid();
			}
		});
		return nameField;
	}

	private ComboBox<String> createSoundMixerField(Platform platform, PlatformGrid crud) {
		ComboBox<String> soundMixerField = new ComboBox<>();
		soundMixerField.setAriaLabel(Translator.translate("Speakers"));
		List<String> outputNames = new ArrayList<>(Speakers.getOutputNames());
		outputNames.add(0, Translator.translate("UseBrowserSound"));
		soundMixerField.setItems(outputNames);
		soundMixerField.setValue(platform.getSoundMixerName());
		soundMixerField.setWidthFull();
		soundMixerField.addBlurListener(event -> {
			String soundMixerName = soundMixerField.getValue();
			if (!Objects.equals(platform.getSoundMixerName(), soundMixerName)) {
				String previousMixerName = platform.getSoundMixerName();
				platform.setSoundMixerName(soundMixerName);
				this.update(platform);
				testSoundMixer(previousMixerName, soundMixerName);
				crud.refreshGrid();
			}
		});
		return soundMixerField;
	}

	private void testSoundMixer(String previousMixerName, String soundMixerName) {
		if (previousMixerName == null || Objects.equals(previousMixerName, soundMixerName)) {
			return;
		}

		for (Mixer soundMixer : Speakers.getOutputs()) {
			if (soundMixer.getMixerInfo().getName().equals(soundMixerName)) {
				Speakers.testSound(soundMixer);
				logger.debug("testing mixer {}", soundMixer.getMixerInfo().getName());
				return;
			}
		}
	}

	private void reorderPlatforms(Platform dropTarget, GridDropLocation dropLocation, PlatformGrid crud) {
		if (this.draggedPlatform == null || dropTarget == null || this.draggedPlatform.equals(dropTarget)) {
			return;
		}

		List<Platform> platforms = new ArrayList<>(PlatformRepository.findAll());
		platforms.remove(this.draggedPlatform);
		int dropIndex = platforms.indexOf(dropTarget);
		if (dropLocation == GridDropLocation.BELOW) {
			dropIndex++;
		}
		platforms.add(dropIndex, this.draggedPlatform);
		PlatformRepository.updateDisplayOrder(platforms);
		WebSocketEventForwarder.sendDatabaseToAll();
		crud.refreshGrid();
	}

	/**
	 * Define the form used to edit a given Platform.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<Platform> createFormFactory() {
		this.editingFormFactory = createPlatformEditingFactory();
		createFormLayout(this.editingFormFactory);
		return this.editingFormFactory;
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 *
	 * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
	 *
	 * @return the actual factory, with the additional mechanisms to do validation
	 */
	private OwlcmsCrudFormFactory<Platform> createPlatformEditingFactory() {
		return new PlatformEditingFormFactory(Platform.class);
	}

	private <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass,
	        String parameter) {
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?fop=" + parameter
		        + "','" + targetClass.getSimpleName() + "')";
	}

	private <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
	        String label, String parameter) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
		return button;
	}
}
