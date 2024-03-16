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
	public void test1() {
		GAMX gamx = new GAMX();
		//zscore.doGetZScore(290,Gender.M,89.0D);
		assertEquals(267.045F, gamx.doGetZScore(Gender.F,191.0,250), 0.001F);
		assertEquals(133.430F, gamx.doGetZScore(Gender.F,35.2,40), 0.001F);
		assertEquals(267.085F, gamx.doGetZScore(Gender.M,89.0,290), 0.001F);
		assertEquals(215.923F, gamx.doGetZScore(Gender.M,80.3,213), 0.001F);
		
		assertEquals(267.895F, gamx.doGetZScore(Gender.M,80.0,278), 0.001F);
		assertEquals(267.360F, gamx.doGetZScore(Gender.M,81.0,279), 0.001F);
		
//		float coefficient = zscore.doGetZCoefficient(Gender.M, 89.0, 267.085F);
//		logger.warn("coeff {}",coefficient);
//		
		int target = gamx.kgTarget(Gender.M, 267.085F, 80.3);
		logger.info("target {} = {}", 80.3, target);
		
		target = gamx.kgTarget(Gender.M, 267.085F, 80.8);
		logger.info("target {} = {}", 80.8, target);
		
		//assertEquals(215.345F, zscore.doGetZScore(Gender.M,80.0,278), 0.001F);
	}
	

}
