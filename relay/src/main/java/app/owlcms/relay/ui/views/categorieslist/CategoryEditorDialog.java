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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.validator.StringLengthValidator;
import app.owlcms.relay.backend.Category;
import app.owlcms.relay.backend.CategoryService;
import app.owlcms.relay.backend.ReviewService;
import app.owlcms.relay.ui.common.AbstractEditorDialog;

/**
 * A dialog for editing {@link Category} objects.
 */
public class CategoryEditorDialog extends AbstractEditorDialog<Category> {

    private final TextField categoryNameField = new TextField("Name");

    public CategoryEditorDialog(BiConsumer<Category, Operation> itemSaver,
            Consumer<Category> itemDeleter) {
        super("category", itemSaver, itemDeleter);

        addNameField();
    }

    private void addNameField() {
        getFormLayout().add(categoryNameField);

        getBinder().forField(categoryNameField)
                .withConverter(String::trim, String::trim)
                .withValidator(new StringLengthValidator(
                        "Category name must contain at least 3 printable characters",
                        3, null))
                .withValidator(
                        name -> !CategoryService.getInstance()
                                .findCategoryByName(name).isPresent(),
                        "Category name must be unique")
                .bind(Category::getName, Category::setName);
    }

    @Override
    protected void confirmDelete() {
        int reviewCount = ReviewService.getInstance()
                .findReviews(getCurrentItem().getName()).size();
        if (reviewCount > 0) {
            openConfirmationDialog("Delete category",
                    "Are you sure you want to delete the “"
                            + getCurrentItem().getName()
                            + "” category? There are " + reviewCount
                            + " reviews associated with this category.",
                    "Deleting the category will mark the associated reviews as “undefined”. "
                            + "You can edit individual reviews to select another category.");
        } else {
            doDelete(getCurrentItem());
        }
    }
}
