package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.data.scoring.GAMX;
import ch.qos.logback.classic.Logger;

public class GAMXTest {
	
	Logger logger = (Logger) LoggerFactory.getLogger(GAMXTest.class);

	@Test
	public void testGmax() {

//		assertEquals(267.045F, GAMX.doGetGamx(Gender.F,191.0,250), 0.001F);
//		assertEquals(133.430F, GAMX.doGetGamx(Gender.F,35.2,40), 0.001F);
//		assertEquals(267.085F, GAMX.doGetGamx(Gender.M,89.0,290), 0.001F);
//		assertEquals(215.923F, GAMX.doGetGamx(Gender.M,80.3,213), 0.001F);
//		
//		assertEquals(267.895F, GAMX.doGetGamx(Gender.M,80.0,278), 0.001F);
//		assertEquals(267.360F, GAMX.doGetGamx(Gender.M,81.0,279), 0.001F);
		
		float doGetGamx = GAMX.doGetGamx(Gender.F,75.54,198);

		assertEquals(297.198F, doGetGamx, 0.001F);
		
		
	}
	
	@Test
	public void testKgTarget() {
//		assertEquals(278, GAMX.kgTarget(Gender.M, 267.085F, 80.3));
//		assertEquals(279, GAMX.kgTarget(Gender.M, 267.085F, 80.8));
		
		float doGetGamx = GAMX.doGetGamx(Gender.F,75.54,198);
		doGetGamx = GAMX.doGetGamx(Gender.F,75.54,198);
		
		assertEquals(297.198F, doGetGamx, 0.001F);
		logger.info("{}",GAMX.kgTarget(Gender.F, doGetGamx, 75.54));
	}

}
