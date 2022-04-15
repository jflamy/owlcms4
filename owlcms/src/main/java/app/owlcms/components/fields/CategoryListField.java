/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class CategoryListField extends CustomField<List<Category>> {

    Logger logger = (Logger) LoggerFactory.getLogger(CategoryListField.class);

    private AgeGroup ageGroup;
    private FlexLayout flex;

    private List<Category> presentationCategories = new ArrayList<>();

    public CategoryListField(AgeGroup ag) {
        super(new ArrayList<Category>());
        this.ageGroup = ag;
        flex = new FlexLayout();
        flex.setSizeFull();
        flex.getStyle().set("flex-wrap", "wrap");
        flex.getStyle().set("margin-top", "1em");
        add(flex);

        HorizontalLayout adder = new HorizontalLayout();
        adder.getStyle().set("margin-top", "0.5em");
        adder.getStyle().set("margin-bottom", "1em");
        TextField newCategoryField = new TextField();
        newCategoryField.setPlaceholder(getTranslation("LimitForCategory"));
        newCategoryField.setPreventInvalidInput(true);
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
    }

    @Override
    protected List<Category> generateModelValue() {
        // the presentation objects are already model values and no conversion is necessary
        // the business model can use them as is; the model ignores the categories with no age group
        // the database ids are also preserved in the copy, and used to update the database
        return presentationCategories;
    }

    @Override
    protected void setPresentationValue(List<Category> iCategories) {
        presentationCategories = iCategories.stream().map(c -> new Category(c)).collect(Collectors.toList());
        updatePresentation();
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
        }
        if ((value == null) || value.trim().isEmpty()) {
            return;
        }
        double newMax = Double.parseDouble(value);
        Category newCat = new Category(0.0, newMax, ag.getGender(), true,
                0, 0, 0, ag, 0);
        presentationCategories.add(newCat);
        updatePresentation();
        newCategoryField.clear();
    }

    private void updatePresentation() {
        flex.removeAll();
        double prevDouble = 0.0;
        presentationCategories.sort((c1, c2) -> ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));

        // last category must be 999, create one if not the case. the loop below will sort out the labels.
        if (presentationCategories.size() == 0) {
            addSentinel();
        } else if (presentationCategories.get(presentationCategories.size() - 1).getMaximumWeight() <= 998.9D) {
            addSentinel();
        }

        for (Category c : presentationCategories) {
            AgeGroup ageGroup2 = c.getAgeGroup();
            if (ageGroup2 == null) {
                continue;
            }
            c.setGender(ageGroup2.getGender() != null ? ageGroup2.getGender() : Gender.F);

            Double maximumWeight = c.getMaximumWeight();
            c.setMinimumWeight(prevDouble); // cover the gap if intervening weights have been skipped...
            Icon closeIcon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
            closeIcon.getStyle().set("font-size", "small");
            Span textSpan = new Span(c.getLimitString());
            Span spacer = new Span("\u00a0");
            Span aspan;
            if (c.getMaximumWeight() >= 998.9D) {
                aspan = new Span(textSpan);
            } else {
                aspan = new Span(textSpan, spacer, closeIcon);
            }

            prevDouble = maximumWeight;
            aspan.getElement().setAttribute("theme", "badge pill");
            aspan.getStyle().set("font-size", "medium");
            aspan.getStyle().set("margin-bottom", "0.5em");
            closeIcon.addClickListener(click -> {
                if (c.getMaximumWeight() >= 998.9D) {
                    return; // leave the sentinel.
                }
                c.setAgeGroup(null); // disconnect
                updatePresentation();
                updateValue();
            });
            flex.add(aspan, new Span("\u00a0"));
        }
    }

}
