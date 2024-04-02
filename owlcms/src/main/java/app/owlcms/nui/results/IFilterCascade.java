package app.owlcms.nui.results;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.combobox.ComboBox;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public interface IFilterCascade {

	default public void defineFilterCascade(GridCrud<Athlete> crud) {

		if (this.getChampionshipFilter() == null) {
			this.setChampionshipFilter(new ComboBox<>());
		}
		this.getChampionshipFilter().setPlaceholder(Translator.translate("Championship"));
		this.getChampionshipFilter().setWidth("25ch");
		this.setChampionshipItems(Championship.findAllUsed());
		this.getChampionshipFilter().setItems(this.getChampionshipItems());
		this.getChampionshipFilter().setItemLabelGenerator((ad) -> ad.translate());
		this.getChampionshipFilter().setClearButtonVisible(true);
		this.getChampionshipFilter().getStyle().set("margin-left", "1em");

		if (this.getAgeGroupFilter() == null) {
			this.setAgeGroupFilter(new ComboBox<>());
		}
		this.getAgeGroupFilter().setPlaceholder(Translator.translate("AgeGroup"));
		this.getAgeGroupFilter().setEnabled(false);
		this.getAgeGroupFilter().setClearButtonVisible(true);
		this.getAgeGroupFilter().setValue(null);
		this.getAgeGroupFilter().setWidth("20ch");
		this.getAgeGroupFilter().setClearButtonVisible(true);
		this.getAgeGroupFilter().getStyle().set("margin-left", "1em");

		getCrudLayout(crud).addFilterComponent(this.getChampionshipFilter());
		getCrudLayout(crud).addFilterComponent(this.getAgeGroupFilter());

		if (this.getCategoryFilter() == null) {
			this.setCategoryFilter(new ComboBox<>());
		}
		this.getCategoryFilter().setClearButtonVisible(true);
		this.getCategoryFilter().setPlaceholder(Translator.translate("Category"));
		this.getCategoryFilter().setClearButtonVisible(true);
		this.getCategoryFilter().setWidth("10em");

		getCrudLayout(crud).addFilterComponent(this.getCategoryFilter());

		// hidden group filter
		this.getGroupFilter().setVisible(false);

		if (this.getGenderFilter() == null) {
			this.setGenderFilter(new ComboBox<>());
		}
		this.getGenderFilter().setPlaceholder(Translator.translate("Gender"));
		this.getGenderFilter().setItems(Gender.M, Gender.F);
		this.getGenderFilter().setItemLabelGenerator((i) -> {
			return i == Gender.M ? Translator.translate("Gender.Men") : Translator.translate("Gender.Women");
		});
		this.getGenderFilter().setClearButtonVisible(true);
		this.getGenderFilter().addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.getGenderFilter().setWidth("10em");
		getCrudLayout(crud).addFilterComponent(this.getGenderFilter());
	}

	default public void defineSelectionListeners() {
		Championship urlAD = getChampionship();
		String urlAG = getAgeGroupPrefix();

		setChampionshipSelectionListener();
		if (urlAD != null && this.getChampionshipItems().contains(urlAD)) {
			this.getChampionshipFilter().setValue(urlAD);
		} else if (this.getChampionshipItems() != null && this.getChampionshipItems().size() == 1) {
			setChampionship(this.getChampionshipItems().get(0));
			this.getChampionshipFilter().setValue(this.getChampionshipItems().get(0));
		}

		setAgeGroupSelectionListener();
		if (urlAG != null && this.getChampionshipAgeGroupPrefixes().contains(urlAG)) {
			this.getAgeGroupFilter().setValue(urlAG);
		} else if (this.getChampionshipAgeGroupPrefixes() != null
		        && this.getChampionshipAgeGroupPrefixes().size() == 1) {
			setAgeGroupPrefix(this.getChampionshipAgeGroupPrefixes().get(0));
			this.getAgeGroupFilter().setValue(this.getChampionshipAgeGroupPrefixes().get(0));
		}

		updateCategoryFilter(getChampionship(), getAgeGroupPrefix());
		this.getCategoryFilter().addValueChangeListener(e -> {
			// logger.debug("categoryFilter set {}", e.getValue());
			setCategoryValue(e.getValue());
			this.getCrudGrid().refreshGrid();
		});
	}

	public default void doAgeGroupPrefixRefresh(String string) {
		setAgeGroupPrefix(string);
		updateCategoryFilter(getChampionship(), getAgeGroupPrefix());
		// xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
		if (this.getCrudGrid() != null) {
			this.getCrudGrid().refreshGrid();
		}
	}

	public ComboBox<String> getAgeGroupFilter();

	public String getAgeGroupPrefix();

	public ComboBox<Category> getCategoryFilter();

	public Category getCategoryValue();

	public Championship getChampionship();

	public List<String> getChampionshipAgeGroupPrefixes();

	public ComboBox<Championship> getChampionshipFilter();

	public List<Championship> getChampionshipItems();

	public OwlcmsCrudGrid<Athlete> getCrudGrid();

	default public CrudLayout getCrudLayout(GridCrud<Athlete> crud) {
		return crud.getCrudLayout();
	}

	public ComboBox<Gender> getGenderFilter();

	public ComboBox<Group> getGroupFilter();

	public Logger getLogger();

	public void setAgeGroupFilter(ComboBox<String> topBarAgeGroupPrefixSelect);

	public void setAgeGroupPrefix(String value);

	public default void setAgeGroupSelectionListener() {
		this.getAgeGroupFilter().addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds
			// the packageDownloadButton button.
			doAgeGroupPrefixRefresh(e.getValue());
		});
	}

	public void setCategoryFilter(ComboBox<Category> categoryFilter);

	public void setCategoryValue(Category category);

	public void setChampionship(Championship ageDivision);

	public void setChampionshipAgeGroupPrefixes(List<String> ageDivisionAgeGroupPrefixes);

	public void setChampionshipFilter(ComboBox<Championship> topBarAgeDivisionSelect);

	public void setChampionshipItems(List<Championship> championshipItems);

	default public void setChampionshipSelectionListener() {
		this.getChampionshipFilter().addValueChangeListener(e -> {
			// logger.debug("championshipFilter {}",e.getValue());
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the packageDownloadButton button.
			Championship ageDivisionValue = e.getValue();
			setChampionship(ageDivisionValue);
			if (ageDivisionValue == null) {
				this.getAgeGroupFilter().setValue(null);
				this.getAgeGroupFilter().setItems(new ArrayList<>());
				this.getAgeGroupFilter().setEnabled(false);
				this.getAgeGroupFilter().setValue(null);
				this.getCrudGrid().refreshGrid();
				return;
			}

			this.setChampionshipAgeGroupPrefixes(AgeGroupRepository.findActiveAndUsedAgeGroups(ageDivisionValue));
			this.getAgeGroupFilter().setItems(this.getChampionshipAgeGroupPrefixes());
			boolean notEmpty = this.getChampionshipAgeGroupPrefixes().size() > 0;
			// logger.debug("championshipAgeGroupPrefixes {}",championshipAgeGroupPrefixes);
			this.getAgeGroupFilter().setEnabled(notEmpty);
			String first = (notEmpty && ageDivisionValue == Championship.of(Championship.IWF))
			        || (this.getChampionshipAgeGroupPrefixes().size() == 1)
			                ? this.getChampionshipAgeGroupPrefixes().get(0)
			                : null;

			String ageGroupPrefix2 = getAgeGroupPrefix();
			if (this.getChampionshipAgeGroupPrefixes().contains(ageGroupPrefix2)) {
				// prefix is valid
				this.getAgeGroupFilter().setValue(ageGroupPrefix2);
			} else {
				// this will trigger other changes and eventually, refresh the grid
				String value = notEmpty ? first : null;
				this.getAgeGroupFilter().setValue(value);
				if (value == null) {
					doAgeGroupPrefixRefresh(null);
				}
			}
		});
	}

	public void setGenderFilter(ComboBox<Gender> genderFilter);

	public default void updateCategoryFilter(Championship ageDivision2, String ageGroupPrefix2) {
		getLogger().warn("updateCategoryFilter {} {} {}", ageDivision2, ageGroupPrefix2, LoggerUtils.whereFrom());
		List<Category> categories = CategoryRepository.findByGenderDivisionAgeBW(this.getGenderFilter().getValue(),
		        getChampionship(), null, null);
		if (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) {
			categories = categories.stream().filter((c) -> c.getAgeGroup().getCode().equals(getAgeGroupPrefix()))
			        .collect(Collectors.toList());
		}

		if (this.getAgeGroupPrefix() == null || this.getAgeGroupPrefix().isBlank()) {
			this.getCategoryFilter().setItems(new ArrayList<>());
		} else {
			Category prevValue = this.getCategoryValue();
			this.getCategoryFilter().setItems(categories);
			// contains is not reliable for Categories, check codes
			if (categories != null && prevValue != null) {
				Optional<Category> cat = categories.stream()
				        .filter(c -> c.getComputedCode().contentEquals(prevValue.getComputedCode())).findFirst();
				Category value = cat.isPresent() ? cat.get() : null;
				this.getCategoryFilter().setValue(value);
			} else {
				this.getCategoryFilter().setValue(null);
			}

		}
	}
}