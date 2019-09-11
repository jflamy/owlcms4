/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package app.owlcms.relay.ui.views.categorieslist;

import java.util.List;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import app.owlcms.relay.backend.Category;
import app.owlcms.relay.backend.CategoryService;
import app.owlcms.relay.backend.Review;
import app.owlcms.relay.backend.ReviewService;
import app.owlcms.relay.ui.MainLayout;
import app.owlcms.relay.ui.common.AbstractEditorDialog;

/**
 * Displays the list of available categories, with a search filter as well as
 * buttons to add a new category or edit existing ones.
 */
@Route(value = "categories", layout = MainLayout.class)
@PageTitle("Categories List")
public class CategoriesList extends VerticalLayout {

    private final TextField searchField = new TextField("",
            "Search categories");
    private final H2 header = new H2("Categories");
    private final Grid<Category> grid = new Grid<>();

    private final CategoryEditorDialog form = new CategoryEditorDialog(
            this::saveCategory, this::deleteCategory);

    public CategoriesList() {
        initView();

        addSearchBar();
        addContent();

        updateView();
    }

    private void initView() {
        addClassName("categories-list");
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
    }

    private void addSearchBar() {
        Div viewToolbar = new Div();
        viewToolbar.addClassName("view-toolbar");

        searchField.setPrefixComponent(new Icon("lumo", "search"));
        searchField.addClassName("view-toolbar__search-field");
        searchField.addValueChangeListener(e -> updateView());
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addFocusShortcut(Key.KEY_F, KeyModifier.CONTROL);

        Button newButton = new Button("New category", new Icon("lumo", "plus"));
        newButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newButton.addClassName("view-toolbar__button");
        newButton.addClickListener(e -> form.open(new Category(),
                AbstractEditorDialog.Operation.ADD));
        /*
            This is a fall-back method:
            '+' is not a event.code (DOM events), so as a fall-back shortcuts
            will perform a character-based comparison. Since Key.ADD changes
            locations frequently based on the keyboard language, we opted to use
            a character instead.
         */
        newButton.addClickShortcut(Key.of("+"));

        viewToolbar.add(searchField, newButton);
        add(viewToolbar);
    }

    private void addContent() {
        VerticalLayout container = new VerticalLayout();
        container.setClassName("view-container");
        container.setAlignItems(Alignment.STRETCH);

        grid.addColumn(Category::getName).setHeader("Name").setWidth("8em")
                .setResizable(true);
        grid.addColumn(this::getReviewCount).setHeader("Beverages")
                .setWidth("6em");
        grid.addColumn(new ComponentRenderer<>(this::createEditButton))
                .setFlexGrow(0);
        grid.setSelectionMode(SelectionMode.NONE);

        container.add(header, grid);
        add(container);
    }

    private Button createEditButton(Category category) {
        Button edit = new Button("Edit", event -> form.open(category,
                AbstractEditorDialog.Operation.EDIT));
        edit.setIcon(new Icon("lumo", "edit"));
        edit.addClassName("review__edit");
        edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        if (CategoryService.getInstance().getUndefinedCategory().getId()
                .equals(category.getId())) {
            edit.setEnabled(false);
        }
        return edit;
    }

    private String getReviewCount(Category category) {
        List<Review> reviewsInCategory = ReviewService.getInstance()
                .findReviews(category.getName());
        int sum = reviewsInCategory.stream().mapToInt(Review::getCount).sum();
        return Integer.toString(sum);
    }

    private void updateView() {
        List<Category> categories = CategoryService.getInstance()
                .findCategories(searchField.getValue());
        grid.setItems(categories);

        if (searchField.getValue().length() > 0) {
            header.setText("Search for “" + searchField.getValue() + "”");
        } else {
            header.setText("Categories");
        }
    }

    private void saveCategory(Category category,
            AbstractEditorDialog.Operation operation) {
        CategoryService.getInstance().saveCategory(category);

        Notification.show(
                "Category successfully " + operation.getNameInText() + "ed.",
                3000, Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        updateView();
    }

    private void deleteCategory(Category category) {
        List<Review> reviewsInCategory = ReviewService.getInstance()
                .findReviews(category.getName());

        reviewsInCategory.forEach(review -> {
            review.setCategory(
                    CategoryService.getInstance().getUndefinedCategory());
            ReviewService.getInstance().saveReview(review);
        });
        CategoryService.getInstance().deleteCategory(category);

        Notification.show("Category successfully deleted.", 3000,
                Position.BOTTOM_START)
            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        updateView();
    }
}
