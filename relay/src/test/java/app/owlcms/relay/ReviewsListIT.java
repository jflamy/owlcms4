package app.owlcms.relay;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.combobox.testbench.ComboBoxElement;
import com.vaadin.flow.component.dialog.testbench.DialogElement;
import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.H2Element;
import com.vaadin.flow.component.textfield.testbench.TextFieldElement;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.testbench.TestBenchElement;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;


public class ReviewsListIT extends AbstractViewTest {

    private static final String NEW_BEVERAGE_NAME = "Cherrytree Cola";
    private static final String CATEGORY = "Soft Drink";
    private static final String ADDITION = " With Lime Twist";

    @Test
    public void buttonIsLumoThemed() {
        ButtonElement newButton = getReviewsList().$(ButtonElement.class).first();
        assertThemePresentOnElement(newButton, Lumo.class);
    }

    @Test
    public void numberOfReviewsIsCorrect() {
        TestBenchElement reviewsList = getReviewsList();
        WebElement header = getReviewsList().$(H2Element.class).id("header");
        String reviewsCountText = header
                .findElement(By.tagName("span")).getText();
        int reviewsCount = Integer.valueOf(
                reviewsCountText.substring(0, reviewsCountText.indexOf(' ')));

        List<DivElement> reviewElements = reviewsList.$(DivElement.class)
                .attributeContains("class", "review").all();

        Assert.assertEquals(
                "The number of rendered reviews must be equal to the actual number of the reviews.",
                reviewsCount, reviewElements.size());
    }

    @Test
    public void createEditAndRemoveReview() {
        getReviewsList().$(ButtonElement.class).id("newReview").click();
        ReviewDialog dialog = $(ReviewDialog.class).waitForFirst();
        dialog.addTextToNameAndSave(NEW_BEVERAGE_NAME);

        applyFilter(NEW_BEVERAGE_NAME);

        WebElement newBeverage = getReviews().iterator().next();
        editReviewName(newBeverage);

        deleteReview(newBeverage);
    }

    private void deleteReview(WebElement newBeverage) {
        ReviewDialog dialog = openReviewEdit(newBeverage);
        dialog.deleteButton().click();

        DialogElement deleteDialog = $(DialogElement.class).all().get(1);
        ButtonElement deleteButton = deleteDialog.$(ButtonElement.class).first();
        assertThat("Expected to have 'Delete' as first button in delete dialog",
                deleteButton.getText(), is("Delete"));

        deleteButton.click();

        assertFalse(String.format(
                "Expect to have no beverages with name '%s' after deletion",
                NEW_BEVERAGE_NAME),
                getReviews().stream().map(this::getReviewName).anyMatch(
                        reviewText -> reviewText.equals(NEW_BEVERAGE_NAME)));
    }

    private void editReviewName(WebElement newBeverage) {
        ReviewDialog dialog = openReviewEdit(newBeverage);
        dialog.addTextToNameAndSave(ADDITION);
        assertThat("Expect new beverage to have edited name",
                getReviewName(newBeverage), is(NEW_BEVERAGE_NAME + ADDITION));
    }

    private TestBenchElement getReviewsList() {
        return $("reviews-list").first();
    }

    private List<TestBenchElement> getReviews() {
        return getReviewsList().$(TestBenchElement.class)
                .attributeContains("class","reviews").all();
    }

    private String getReviewName(WebElement review) {
        return review.findElement(By.className("review__name")).getText();
    }

    private ReviewDialog openReviewEdit(WebElement review) {
        review.findElement(By.className("review__edit")).click();
        return $(ReviewDialog.class).waitForFirst();
    }

    private void applyFilter(String filterName) {
        getReviewsList().$(TextFieldElement.class).id("search")
                .setValue(filterName);
        assertThat(
                String.format("Expect to have only one beverage with name '%s'",
                        filterName),
                getReviews().size(), is(1));
    }


    private static class ReviewDialog extends DialogElement {

        @Override
        protected void init() {
            List<ButtonElement> buttons = $(ButtonElement.class).all();
            List<String> buttonTexts = buttons.stream().map(WebElement::getText)
                    .collect(Collectors.toList());
            assertThat(
                    "Expect to have three buttons on review dialog: Save, Cancel, Delete",
                    buttonTexts, is(Arrays.asList("Save", "Cancel", "Delete")));

            String beverageInputLabelText = $(TextFieldElement.class).first()
                    .getLabel();
            String expectedLabelText = "Beverage";
            assertThat(String.format(
                    "Expected reviews dialog have its first text field label to be '%s'",
                    expectedLabelText), beverageInputLabelText,
                    is(expectedLabelText));
        }


        private ButtonElement saveButton() {
            return $(ButtonElement.class).all().get(0);
        }

        private ButtonElement deleteButton() {
            return $(ButtonElement.class).all().get(2);
        }

        private TextFieldElement nameInput() {
            return $(TextFieldElement.class).first();
        }

        private ComboBoxElement categoryInput() {
            return $(ComboBoxElement.class).first();
        }

        private void addTextToNameAndSave(String textToAdd) {
            nameInput().setValue(nameInput().getValue() + textToAdd);
            categoryInput().selectByText(CATEGORY);
            saveButton().click();
            assertFalse("Dialog is expected to be closed", isOpen());
        }
    }
}
