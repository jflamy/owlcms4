/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;

public class ConfigTest {

    private String originalFeatureSwitchesProperty;

    @Before
    public void captureFeatureSwitchesProperty() {
        this.originalFeatureSwitchesProperty = System.getProperty("featureSwitches");
        System.clearProperty("featureSwitches");
    }

    @After
    public void restoreFeatureSwitchesProperty() {
        if (this.originalFeatureSwitchesProperty == null) {
            System.clearProperty("featureSwitches");
        } else {
            System.setProperty("featureSwitches", this.originalFeatureSwitchesProperty);
        }
    }

    @Test
    public void legacyFeatureSwitchStringMigratesToJson() {
        Config config = new Config();

        config.setFeatureSwitches("teamPointsTotalOnly; -noLiveLights AthleteCardEntryTotal");

        assertEquals("{\"teamPointsTotalOnly\":true,\"noLiveLights\":false,\"athleteCardEntryTotal\":true}",
                config.getFeatureSwitchJson());
        assertEquals("teamPointsTotalOnly,-noLiveLights,athleteCardEntryTotal", config.getFeatureSwitches());
        assertTrue(config.featureSwitch(FeatureSwitch.TEAM_POINTS_TOTAL_ONLY));
        assertFalse(config.featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS));
        assertTrue(config.featureSwitch(FeatureSwitch.ATHLETE_CARD_ENTRY_TOTAL));
    }

    @Test
    public void loadedLegacyFeatureSwitchStringMigratesOnJsonRead() throws Exception {
        Config config = new Config();
        Field featureSwitches = Config.class.getDeclaredField("featureSwitches");
        featureSwitches.setAccessible(true);
        featureSwitches.set(config, "teamPointsTotalOnly -noLiveLights");

        assertEquals("{\"teamPointsTotalOnly\":true,\"noLiveLights\":false}", config.getFeatureSwitchJson());
        assertEquals("teamPointsTotalOnly,-noLiveLights", config.getFeatureSwitches());
        assertTrue(config.featureSwitch(FeatureSwitch.TEAM_POINTS_TOTAL_ONLY));
        assertFalse(config.featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS));
    }

    @Test
    public void jsonFeatureSwitchCanRemoveEnvironmentSwitch() {
        System.setProperty("featureSwitches", FeatureSwitch.NO_LIVE_LIGHTS.getId());
        Config config = new Config();

        config.setFeatureSwitchJson("{\"noLiveLights\":false}");

        assertFalse(config.featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS));
        assertEquals("-noLiveLights", config.getFeatureSwitches());
    }

    @Test
    public void jsonFeatureSwitchSetterCanonicalizesAliases() {
        Config config = new Config();

        config.setFeatureSwitchJson("{\"AthleteCardEntryTotal\":true}");

        assertEquals("{\"athleteCardEntryTotal\":true}", config.getFeatureSwitchJson());
        assertEquals("athleteCardEntryTotal", config.getFeatureSwitches());
        assertTrue(config.featureSwitch(FeatureSwitch.ATHLETE_CARD_ENTRY_TOTAL));
    }
}
