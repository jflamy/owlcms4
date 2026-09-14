package app.owlcms.spreadsheet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NRegistrationFileProcessorTest {

    @Test
    public void athleteKeyIgnoresNameCaseAndWhitespace() {
        assertEquals(
                NRegistrationFileProcessor.athleteKey("GONZALEZ", "CARLOS", 12),
                NRegistrationFileProcessor.athleteKey(" gonzalez ", " carlos ", 12));
        assertEquals(
            NRegistrationFileProcessor.athleteKey("IRMA", "SCOTT", 13),
            NRegistrationFileProcessor.athleteKey("irma", "scott", 13));
    }
}