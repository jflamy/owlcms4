package app.owlcms.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.owlcms.data.platform.Platform;

public class PlatformTest {

    @Test
    public void displayOrderUsesAlphabeticalNameWhenUnset() {
        Platform blue = new Platform("Blue");
        Platform red = new Platform("Red");
        Platform white = new Platform("White");

        assertTrue("Blue before Red", blue.compareTo(red) < 0);
        assertTrue("Red before White", red.compareTo(white) < 0);
    }

    @Test
    public void explicitDisplayOrderPrecedesAlphabeticalName() {
        Platform blue = new Platform("Blue");
        Platform red = new Platform("Red");
        Platform white = new Platform("White");
        red.setDisplayOrder(1);
        white.setDisplayOrder(2);

        assertTrue("Explicit order before unset order", red.compareTo(blue) < 0);
        assertTrue("Lower explicit order first", red.compareTo(white) < 0);
        assertTrue("Unset order after explicit order", blue.compareTo(white) > 0);
    }
}
