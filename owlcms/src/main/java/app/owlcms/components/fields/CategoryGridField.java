/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CategoryGridField extends CustomField<List<Category>> {

	private static final String QT_WIDTH = "5ch";
	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());
	Logger logger = (Logger) LoggerFactory.getLogger(CategoryGridField.class);
	private AgeGroup ageGroup;
	private Grid<Category> catGrid;
	private List<Category> presentationCategories = new ArrayList<>();
	private Div validationStatus;

	public CategoryGridField(AgeGroup ag) {
		super(new ArrayList<>());
		this.ageGroup = ag;

		this.validationStatus = new Div();

		this.catGrid = new Grid<>(Category.class, false);
		this.catGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
		this.catGrid.setAllRowsVisible(true);
		this.catGrid.setWidth("fit-content");
		this.catGrid.addColumn(new ComponentRenderer<>(pc -> {
			Span name = new Span(pc.getDisplayName());
			name.getStyle().set("font-weight", "bold");
			return name;
		})).setHeader(Translator.translate("Category")).setAutoWidth(true).setFlexGrow(0);
		this.catGrid.addColumn(new ComponentRenderer<>(this::qualifyingTotalField))
		        .setHeader(Translator.translate("Category.QualificationTotal")).setAutoWidth(true).setFlexGrow(0)
		        .setTextAlign(ColumnTextAlign.CENTER);
		this.catGrid.addColumn(new ComponentRenderer<>(this::deleteButton)).setAutoWidth(true).setFlexGrow(0);

		HorizontalLayout adder = new HorizontalLayout();
		adder.getStyle().set("margin-top", "0");
		adder.getStyle().set("margin-bottom", "0.25em");
		TextField newCategoryField = new TextField();
		newCategoryField.setPlaceholder(Translator.translate("LimitForCategory"));
		newCategoryField.setAllowedCharPattern("[0-9]");
		newCategoryField.setPattern("[0-9]{0,3}");

		Button button = new Button(Translator.translate("AddNewCategory"));
		button.addClickListener((click) -> {
			react(ag, newCategoryField);
		});

		button.setIcon(new Icon(VaadinIcon.PLUS));
		button.setThemeName("primary success");
		button.addClickShortcut(Key.ENTER);
		adder.add(newCategoryField, button);

		updatePresentation();

		add(adder);
		add(this.validationStatus);
		add(this.catGrid);

	}

	@Override
	public List<Category> getValue() {
		return this.presentationCategories;
	}

	@Override
	protected List<Category> generateModelValue() {
		// the presentation objects are the scratchpad's categories, edited in place;
		// the save service matches them to stored rows by upper limit, not by id
		return this.presentationCategories;
	}

	@Override
	protected void setPresentationValue(List<Category> iCategories) {
		this.presentationCategories = iCategories.stream().map(c -> new Category(c)).collect(Collectors.toList());
		updatePresentation();
	}

	private void addSentinel() {
		Category newCat = new Category(0.0, 999.0D, this.ageGroup.getGender(), true,
		        0, 0, 0, this.ageGroup, 0);
		this.presentationCategories.add(newCat);
	}

	private TextField qualifyingTotalField(Category pc) {
		TextField qualTotField = new TextField();
		qualTotField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
		qualTotField.setAllowedCharPattern("[0-9]");
		qualTotField.setWidth(QT_WIDTH);
		Binder<Category> catBinder = new Binder<>(Category.class);
		catBinder
		        .forField(qualTotField)
		        .withConverter(
		                new StringToIntegerConverter("Qualifying total must be a number."))
		        .withStatusLabel(this.validationStatus).bind("qualifyingTotal");
		catBinder.setBean(pc);
		return qualTotField;
	}

	private Component deleteButton(Category pc) {
		boolean deletable = (pc.getMaximumWeight() <= 998 && pc.getMinimumWeight() <= 200.0D)
		        // historical bug created false 999 999 sentinel categories
		        || (pc.getMaximumWeight() >= 998.9D && pc.getMinimumWeight() >= 998.0D);
		if (!deletable) {
			return new Div();
		}
		Button delete = new Button(Translator.translate("Delete"));
		delete.addClassName("delete");
		delete.addClickListener(e -> {
			if (pc.getMaximumWeight() >= 998.9D && (pc.getMinimumWeight() <= 998.0D)) {
				// old bug created false sentinels 999 999
				return; // leave the sentinel.
			}
			pc.setMaximumWeight(0D); // disconnect
			updatePresentation();
			updateValue();
		});
		return delete;
	}

	private void react(AgeGroup ag, TextField newCategoryField) {
		String value = newCategoryField.getValue();
		if (ag == null) {
			Notification notif = new Notification();
			notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notif.setText(Translator.translate("SaveAgeGroupBefore"));
			return; // was missing ?
		}
		if ((value == null) || value.trim().isEmpty()) {
			return;
		}
		double newMax = Double.parseDouble(value);
		boolean duplicate = this.presentationCategories.stream()
		        .anyMatch(c -> c.getMaximumWeight() != null && Math.abs(c.getMaximumWeight() - newMax) < 0.01);
		if (duplicate) {
			Notification notif = new Notification();
			notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notif.setText(Translator.translate("AgeGroup.DuplicateUpperLimit", value.trim()));
			notif.open();
			return;
		}
		Category newCat = new Category(0.0, newMax, ag.getGender(), true,
		        0, 0, 0, ag, 0);
		this.presentationCategories.add(newCat);
		updatePresentation();
		updateValue();
		newCategoryField.clear();
	}

	private void updatePresentation() {
		double prevDouble = 0.0;
		this.presentationCategories.sort((c1, c2) -> ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));

		// last category max must always be 999, create a sentinel.
		if (this.presentationCategories.size() == 0) {
			addSentinel();
		} else if (this.presentationCategories.get(this.presentationCategories.size() - 1)
		        .getMaximumWeight() <= 998.9D) {
			addSentinel();
		}

		Iterator<Category> ic = this.presentationCategories.iterator();
		while (ic.hasNext()) {
			Category c = ic.next();
			AgeGroup ageGroup2 = c.getAgeGroup();
			// deletion is flagged by setting maximum weight
			if (c.getMaximumWeight() < 0.01) {
				ic.remove();
				continue;
			}
			c.setGender(ageGroup2.getGender() != null ? ageGroup2.getGender() : Gender.F);

			Double maximumWeight = c.getMaximumWeight();
			c.setMinimumWeight(prevDouble); // cover the gap if intervening weights have been skipped...

			prevDouble = maximumWeight;

		}
		this.catGrid.setItems(new ArrayList<>(this.presentationCategories));
	}

}
