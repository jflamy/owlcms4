/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CategoryGridField extends CustomField<List<Category>> {

	private static final String CAT_WIDTH = "12.0ch";
	private static final String QT_WIDTH = "5ch";
	Collection<Button> editButtons = Collections.newSetFromMap(new WeakHashMap<>());
	Logger logger = (Logger) LoggerFactory.getLogger(CategoryGridField.class);
	private AgeGroup ageGroup;
	private VerticalLayout catGrid;
	private List<Category> presentationCategories = new ArrayList<>();
	private Div validationStatus;

	public CategoryGridField(AgeGroup ag) {
		super(new ArrayList<Category>());
		this.ageGroup = ag;

		validationStatus = new Div();

		catGrid = new VerticalLayout();
		// NativeLabel l = new NativeLabel(Translator.translate("LimitForCategory"));
		catGrid.setWidth("50em");

		HorizontalLayout adder = new HorizontalLayout();
		adder.getStyle().set("margin-top", "0.5em");
		adder.getStyle().set("margin-bottom", "1em");
		TextField newCategoryField = new TextField();
		newCategoryField.setPlaceholder(getTranslation("LimitForCategory"));
		newCategoryField.setAllowedCharPattern("[0-9]");
		newCategoryField.setPattern("[0-9]{0,3}");

		Button button = new Button(getTranslation("AddNewCategory"));
		button.addClickListener((click) -> {
			react(ag, newCategoryField);
		});

		button.setIcon(new Icon(VaadinIcon.PLUS));
		button.setThemeName("primary success");
		button.addClickShortcut(Key.ENTER);
		adder.add(newCategoryField, button);

		updatePresentation();

		add(adder);
		add(validationStatus);
		add(catGrid);

	}

	@Override
	public List<Category> getValue() {
		return presentationCategories;
	}

	private void addSentinel() {
		Category newCat = new Category(0.0, 999.0D, ageGroup.getGender(), true,
		        0, 0, 0, ageGroup, 0);
		presentationCategories.add(newCat);
	}

	private void react(AgeGroup ag, TextField newCategoryField) {
		String value = newCategoryField.getValue();
		if (ag == null) {
			Notification notif = new Notification();
			notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
			notif.setText(getTranslation("SaveAgeGroupBefore"));
			return; // was missing ?
		}
		if ((value == null) || value.trim().isEmpty()) {
			return;
		}
		double newMax = Double.parseDouble(value);
		Category newCat = new Category(0.0, newMax, ag.getGender(), true,
		        0, 0, 0, ag, 0);
		presentationCategories.add(newCat);
		updatePresentation();
		updateValue();
		newCategoryField.clear();
	}

	private void updatePresentation() {
		double prevDouble = 0.0;
		presentationCategories.sort((c1, c2) -> ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));

		// last category max must always be 999, create a sentinel.
		if (presentationCategories.size() == 0) {
			addSentinel();
		} else if (presentationCategories.get(presentationCategories.size() - 1).getMaximumWeight() <= 998.9D) {
			addSentinel();
		}

		Iterator<Category> ic = presentationCategories.iterator();
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
		populateGrid(catGrid, presentationCategories);
		catGrid.setSizeUndefined();
	}

	private void populateGrid(VerticalLayout catGrid2, List<Category> presentationCategories2) {
		catGrid.removeAll();
		Div filler = new Div();
		filler.getElement().setProperty("innerHtml", "&nbsp;");
		filler.setWidth(CAT_WIDTH);
		HorizontalLayout title = new HorizontalLayout(filler,
		        new Text(Translator.translate("Category.QualificationTotal")));
		catGrid.add(title);
		for (Category pc : presentationCategories2) {
			HorizontalLayout hl = new HorizontalLayout();
			NativeLabel nativeLabel = new NativeLabel(pc.getName());
			nativeLabel.getStyle().set("font-weight","bold");
			nativeLabel.setWidth(CAT_WIDTH);
			hl.add(nativeLabel);
			TextField qualTotField = new TextField();
			qualTotField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
			qualTotField.setAllowedCharPattern("[0-9]");
			qualTotField.setWidth(QT_WIDTH);
			Binder<Category> catBinder = new Binder<>(Category.class);
			catBinder
			        .forField(qualTotField)
			        .withConverter(
			                new StringToIntegerConverter("Qualifying total must be a number."))
			        .withStatusLabel(validationStatus).bind("qualifyingTotal");
			catBinder.setBean(pc);
			hl.add(qualTotField);		
			Button delete = new Button(Translator.translate("Delete"));
			delete.addClassName("delete");
			delete.addClickListener(e -> {
				// logger.trace("deleting {} {}",cat != null ? cat.shortDump() : null,
				// presentationCategories.contains(cat));
				if (pc.getMaximumWeight() >= 998.9D) {
					return; // leave the sentinel.
				}
				pc.setMaximumWeight(0D); // disconnect
				updatePresentation();
				updateValue();
			});
			delete.getStyle().set("margin-left","2em");
			hl.add(delete);
			catGrid2.add(hl);
		}
	}

	@Override
	protected List<Category> generateModelValue() {
		// the presentation objects are already model values and no conversion is
		// necessary
		// the business model can use them as is; the model ignores the categories with
		// no age group
		// the database ids are also preserved in the copy, and used to update the
		// database
		return presentationCategories;
	}

	@Override
	protected void setPresentationValue(List<Category> iCategories) {
		presentationCategories = iCategories.stream().map(c -> new Category(c)).collect(Collectors.toList());
		updatePresentation();
	}

}
