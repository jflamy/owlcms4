package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.scoring.AgeFactors;
import ch.qos.logback.classic.Logger;

public class AgeFactorsTest {

	Logger logger = (Logger) LoggerFactory.getLogger(AgeFactorsTest.class);

	@Test
	public void testAgeFactors() {
		
		float value = AgeFactors.doGetScore(Gender.F, 40.55, 13, 100);
		// differs from Huebner due to interpolation.
		assertEquals(259.4F, value, 0.1F);
		
		value = AgeFactors.doGetScore(Gender.M, 115.0D, 20, 100);
		// differs from Huebner due to interpolation.
		assertEquals(115.1, value, 0.1F);
		
		value = AgeFactors.doGetScore(Gender.M, 110.0D, 20, 100);
		// differs from Huebner due to interpolation.
		assertEquals(114.9, value, 0.1F);
		
	}

	@Test
	public void testKgTarget() {
		// assertEquals(278, GAMX.kgTarget(Gender.M, 267.085F, 80.3));
		// assertEquals(279, GAMX.kgTarget(Gender.M, 267.085F, 80.8));

		float value = AgeFactors.doGetScore(Gender.F, 40.55, 13, 100);
		int target = AgeFactors.kgTarget(Gender.F, value, 50, 12);
		assertEquals(112.0, target, 0.1F);
		
		value = AgeFactors.doGetScore(Gender.F,	50.0D, 12, 112);
		assertEquals(261.6, value, 0.1F);
		
	}

}
