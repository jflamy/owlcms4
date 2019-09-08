package app.owlcms.relay;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.dialog.testbench.DialogElement;
import com.vaadin.flow.component.grid.testbench.GridElement;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.testbench.TestBenchElement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class CategoriesListIT extends AbstractViewTest {
    private static final String NEW_CATEGORY_NAME = "Awesome drinks";

    public CategoriesListIT() {
        super("categories");
    }

    @Test
    public void createFilterAndRemoveCategory() {
        assertFalse("Expect to have categories on the main page",
                getVisibleCategoriesCount() <= 0L);
        findElement(By.className("view-toolbar__button")).click();

        CategoryDialog dialog = $(CategoryDialog.class).waitForFirst();

        dialog.addTextToNameAndSave(NEW_CATEGORY_NAME);
        applyFilter(NEW_CATEGORY_NAME);

        deleteCategory();
    }

    private long getVisibleCategoriesCount() {
        return getCategoriesList()
                .$(GridElement.class).first()
                .getRowCount();
    }

    private void applyFilter(String filterName) {
        findElement(By.className("view-toolbar__search-field"))
                .sendKeys(filterName);
        assertThat(
                String.format("Expect to have only one category with name '%s'",
                        filterName),
                getVisibleCategoriesCount(), is(1L));
    }

    private void deleteCategory() {
        getCategoriesList().findElement(By.className("review__edit")).click();
        CategoryDialog dialog = $(CategoryDialog.class).waitForFirst();

        dialog.deleteButton().click();

        assertThat(
                String.format("Expect to have no category with name '%s'",
                        NEW_CATEGORY_NAME),
                getVisibleCategoriesCount(), is(0L));
    }


    private TestBenchElement getCategoriesList() {
        return $(TestBenchElement.class)
                .attributeContains("class", "categories-list").first();
    }


    private static class CategoryDialog extends DialogElement {

        public CategoryDialog() {
        }

        @Override
        protected void init() {
            List<ButtonElement> buttons = $(ButtonElement.class).all();
            List<String> buttonTexts = buttons.stream().map(WebElement::getText)
                    .collect(Collectors.toList());
            assertThat(
                    "Expect to have three buttons on category dialog: Save, Cancel, Delete",
                    buttonTexts, is(Arrays.asList("Save", "Cancel", "Delete")));

            String beverageInputLabelText = $(TextFieldElement.class).first()
                    .getLabel();
            String expectedLabelText = "Name";
            assertThat(String.format(
                    "Expected categories dialog have its first text field label to be '%s'",
                    expectedLabelText), beverageInputLabelText,
                    is(expectedLabelText));
        }

        private ButtonElement saveButton() {
            return $(ButtonElement.class).first();
        }

        private ButtonElement deleteButton() {
            return $(ButtonElement.class).all().get(2);
        }

        private TextFieldElement categoryNameInput() {
            return $(TextFieldElement.class).first();
        }

        private void addTextToNameAndSave(String textToAdd) {
            categoryNameInput().setValue(textToAdd);
            saveButton().click();
            assertFalse("Dialog is expected to be closed", isOpen());
        }
    }
}

