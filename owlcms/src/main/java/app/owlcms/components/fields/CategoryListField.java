package app.owlcms.components.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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

    private FlexLayout flex;
    private List<Category> presentationCategories;

    private AgeGroup ageGroup;

    public CategoryListField(AgeGroup ag) {
        super(new ArrayList<Category>());
        this.ageGroup = ag;
        flex = new FlexLayout();
        flex.setSizeFull();
        flex.getStyle().set("flex-wrap", "wrap");
        add(flex);

        HorizontalLayout adder = new HorizontalLayout();
        adder.getStyle().set("margin-top", "0.5em");
        TextField newCategoryField = new TextField();
        newCategoryField.setPreventInvalidInput(true);
        newCategoryField.setPattern("[0-9]{0,3}");
        newCategoryField.setWidth("5em");
        Button button = new Button(getTranslation("AddNewCategory"));
        button.addClickListener((click) -> {
            String value = newCategoryField.getValue();
            if (value == null) return;
            double newMax = Double.parseDouble(value);
            Category newCat = new Category(null, 0.0, newMax, ag.getGender(),
                    true, 0, 0, 0, ag);
            presentationCategories.add(newCat);
            updatePresentation();
            newCategoryField.clear();
        });
        adder.add(newCategoryField, button);
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

    private void updatePresentation() {
        flex.removeAll();
        int prev = 0;
        double prevDouble = 0.0;
        presentationCategories.sort((c1,c2)->ObjectUtils.compare(c1.getMaximumWeight(), c2.getMaximumWeight()));
        
        //last category must be 999, create one if not the case. the loop below will sort out the labels.
        if (presentationCategories.size() == 0) {
            addSentinel();
        } else if (presentationCategories.get(presentationCategories.size()-1).getMaximumWeight() <= 998.9D) {
            addSentinel();
        }
        
        for (Category c : presentationCategories) {
            if (c.getAgeGroup() == null)
                continue;

            Double maximumWeight = c.getMaximumWeight();
            c.setMinimumWeight(prevDouble); // cover the gap if intervening weights have been skipped...
            int max = (int) Math.round(maximumWeight);
            Icon closeIcon = new Icon(VaadinIcon.CLOSE_CIRCLE_O);
            closeIcon.getStyle().set("font-size", "small");
            Span textSpan = new Span((max >= 200) ? ">" + prev : "" + max);
            Span spacer = new Span("\u00a0");
            Span aspan = new Span(textSpan, spacer, closeIcon);

            prev = max;
            prevDouble = maximumWeight;
            aspan.getElement().setAttribute("theme", "badge pill"
//                    + (c.getGender() == Gender.F ? " error" : "")
                    );
            aspan.getStyle().set("font-size", "medium");
            closeIcon.addClickListener(click -> {
                if (c.getMaximumWeight() >= 998.9D) return; // leave the sentinel.
                c.setAgeGroup(null); // disconnect
                updatePresentation();
                updateValue();
            });
            flex.add(aspan, new Span("\u00a0"));
        }
    }

    private void addSentinel() {
        Category newCat = new Category(null, 0.0, 999.0D, ageGroup.getGender(),
                true, 0, 0, 0, ageGroup);
        presentationCategories.add(newCat);
    }

}
