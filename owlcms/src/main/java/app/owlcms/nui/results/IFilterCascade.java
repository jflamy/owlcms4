/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;

import org.slf4j.LoggerFactory;
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
import ch.qos.logback.classic.Logger;

public interface IFilterCascade {

	final static Logger logger = (Logger) LoggerFactory.getLogger(IFilterCascade.class);

	default public void clearFilters() {
		this.getAgeGroupFilter().clear();
		this.getChampionshipFilter().clear();
		this.getCategoryFilter().clear();
		this.getGenderFilter().clear();
	}

	/**
	 * A combo box has no minimum width derived from its placeholder, so the placeholder gets truncated in a narrow
	 * field. Size the field to the (translated) placeholder plus room for the toggle and clear buttons.
	 */
	default int placeholderChars(String placeholder, int minChars) {
		int chars = placeholder == null ? 0 : placeholder.length() + 5;
		return Math.max(minChars, chars);
	}

	default String placeholderWidth(String placeholder, int minChars) {
		return placeholderChars(placeholder, minChars) + "ch";
	}

	/**
	 * Make the dropdown list at least as wide as the field and as its widest item label, in the current language,
	 * when the field itself is narrower than its content.
	 */
	default <T> void fitOverlay(ComboBox<T> filter, Collection<T> items, int minChars) {
		int floor = placeholderChars(filter.getPlaceholder(), minChars);
		int widest = items == null ? 0
		        : items.stream()
		                .map(filter.getItemLabelGenerator()::apply)
		                .filter(Objects::nonNull)
		                .mapToInt(String::length)
		                .max()
		                .orElse(0);
		filter.getStyle().set("--vaadin-combo-box-overlay-width", Math.max(floor, widest + 6) + "ch");
	}

	default public void defineFilterCascade(GridCrud<Athlete> crud) {

		if (this.getChampionshipFilter() == null) {
			this.setChampionshipFilter(new ComboBox<>());
		}
		this.getChampionshipFilter().setPlaceholder(Translator.translate("Championship"));
		this.getChampionshipFilter().setWidth(placeholderWidth(this.getChampionshipFilter().getPlaceholder(), 14));
		// narrow field to fit a 1366x768 screen without a horizontal scrollbar; the list opens wider, see fitOverlay
		this.setChampionshipItems(Championship.findAllVisible(true));
		// Preserve the configured championship order from findAllVisible.
		List<Championship> championshipItemsSafe = Optional.ofNullable(this.getChampionshipItems()).orElse(Collections.emptyList())
			.stream()
			.filter(Objects::nonNull)
			.toList();
		this.getChampionshipFilter().setItems(championshipItemsSafe);
		this.getChampionshipFilter().setItemLabelGenerator((ad) -> ad.translate());
		fitOverlay(this.getChampionshipFilter(), championshipItemsSafe, 14);
		this.getChampionshipFilter().setClearButtonVisible(true);
		this.getChampionshipFilter().getStyle().set("margin-left", "1em");

		if (this.getAgeGroupFilter() == null) {
			this.setAgeGroupFilter(new ComboBox<>());
		}
		this.getAgeGroupFilter().setPlaceholder(Translator.translate("AgeGroup"));
		// this.getAgeGroupFilter().setEnabled(false);
		this.getAgeGroupFilter().setVisible(false);
		this.getAgeGroupFilter().setClearButtonVisible(true);
		this.getAgeGroupFilter().setValue(null);
		this.getAgeGroupFilter().setWidth(placeholderWidth(this.getAgeGroupFilter().getPlaceholder(), 12));
		fitOverlay(this.getAgeGroupFilter(), null, 12);
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
		this.getCategoryFilter().setWidth(placeholderWidth(this.getCategoryFilter().getPlaceholder(), 8));

		getCrudLayout(crud).addFilterComponent(this.getCategoryFilter());

		// hidden group filter
		this.getGroupFilter().setVisible(false);

		if (this.getGenderFilter() == null) {
			this.setGenderFilter(new ComboBox<>());
		}
		if (showGenderFilter()) {
			this.getGenderFilter().setPlaceholder(Translator.translate("Gender"));
			this.getGenderFilter().setItems(Gender.M, Gender.F);
			this.getGenderFilter().setItemLabelGenerator((i) -> {
				return i.asGenderName();
			});
			this.getGenderFilter().setClearButtonVisible(true);
			this.getGenderFilter().setValue(getGender());
			this.getGenderFilter().addValueChangeListener(e -> {
				this.setGender(e.getValue());
				crud.refreshGrid();
			});
			this.getGenderFilter().setWidth(placeholderWidth(this.getGenderFilter().getPlaceholder(), 8));
			fitOverlay(this.getGenderFilter(), List.of(Gender.M, Gender.F), 8);
			getCrudLayout(crud).addFilterComponent(this.getGenderFilter());
		}
	}

	default public void defineSelectionListeners() {
		Championship urlAD = getChampionship();
		String urlAG = getAgeGroupPrefix();

		setChampionshipSelectionListener();
		if (urlAD != null && this.getChampionshipItems().contains(urlAD)) {
			this.getChampionshipFilter().setValue(urlAD);
		} else if (this.getChampionshipItems() != null && this.getChampionshipItems().size() == 1) {
			setChampionship(this.getChampionshipItems().get(0));
			this.getChampionshipFilter().setItems(this.getChampionshipItems());
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
			setCategoryValue(e.getValue());
			this.getCrudGrid().refreshGrid();
		});
	}

	public default void doAgeGroupPrefixRefresh(String string) {
		setAgeGroupPrefix(string);
		updateCategoryFilter(getChampionship(), getAgeGroupPrefix());
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

	public Gender getGender();

	public ComboBox<Gender> getGenderFilter();

	public ComboBox<Group> getGroupFilter();

	public Logger getLogger();

	public void setAgeGroupFilter(ComboBox<String> topBarAgeGroupPrefixSelect);

	public void setAgeGroupPrefix(String value);

	public default void setAgeGroupSelectionListener() {

		this.getAgeGroupFilter().addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the packageDownloadButton button.
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
			Championship championshipValue = e.getValue();
			setChampionship(championshipValue);
			
			// When championship changes, nullify age group prefix to let cascade recompute it
			// Different championships have different age groups available
			setAgeGroupPrefix(null);
			
			// Update scoring system dropdown when championship changes
			onChampionshipChanged(championshipValue);
			
			if (championshipValue == null) {
				this.getAgeGroupFilter().setValue(null);
				this.getAgeGroupFilter().setItems(new ArrayList<>());
				this.getAgeGroupFilter().setVisible(false);
				this.getAgeGroupFilter().setValue(null);
				this.getCrudGrid().refreshGrid();
				return;
			}

			this.setChampionshipAgeGroupPrefixes(AgeGroupRepository.findActiveAndUsedAgeGroupNames(championshipValue));
			List<String> championshipAgeGroupPrefixes = this.getChampionshipAgeGroupPrefixes();
			this.getAgeGroupFilter().setItems(championshipAgeGroupPrefixes);
			fitOverlay(this.getAgeGroupFilter(), championshipAgeGroupPrefixes, 12);

			boolean notEmpty = championshipAgeGroupPrefixes.size() > 0;
			// this.getAgeGroupFilter().setEnabled(notEmpty);
			this.getAgeGroupFilter().setVisible(championshipAgeGroupPrefixes.size() > 1);
			String first = notEmpty ? championshipAgeGroupPrefixes.get(0) : null;

			String ageGroupPrefix2 = getAgeGroupPrefix();
			if (championshipAgeGroupPrefixes.contains(ageGroupPrefix2)) {
				// prefix is valid
				this.getAgeGroupFilter().setValue(ageGroupPrefix2);
			} else {
				// Only auto-select the age group when there is exactly one available candidate.
				String value = (championshipAgeGroupPrefixes.size() == 1) ? first : null;
				this.getAgeGroupFilter().setValue(value);
				if (value == null) {
					doAgeGroupPrefixRefresh(null);
				}
			}
		});
	}

	public void setGender(Gender value);

	public void setGenderFilter(ComboBox<Gender> genderFilter);

	public default boolean showGenderFilter() {
		return false;
	}
	
	/**
	 * Hook called when championship selection changes.
	 * Override to update other UI components (e.g., scoring system dropdown).
	 */
	public default void onChampionshipChanged(Championship championship) {
		// Default implementation does nothing
	}

	public default void updateCategoryFilter(Championship ageDivision2, String ageGroupPrefix2) {
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
			fitOverlay(this.getCategoryFilter(), categories, 8);
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