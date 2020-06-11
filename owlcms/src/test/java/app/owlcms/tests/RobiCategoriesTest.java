package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.RobiCategories;

public class RobiCategoriesTest {

    @Test
    public void testInside() {
        Athlete a = new Athlete();
        a.setBodyWeight(57.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M61", cat.getCode());
    }
    
    @Test
    public void testOutside() {
        Athlete a = new Athlete();
        a.setBodyWeight(50.2D);
        a.setGender(Gender.M);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M55", cat.getCode());
    }

    @Test
    public void testYoung() {
        Athlete a = new Athlete();
        a.setBodyWeight(48.2D);
        a.setGender(Gender.M);
        a.setYearOfBirth(2003);
        Category cat = RobiCategories.findRobiCategory(a);
        assertEquals("M49", cat.getCode());
    }
}
