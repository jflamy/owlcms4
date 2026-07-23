/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.platform.Platform;

public class GroupComparatorTest {

    @Test
    public void sessionBlocksSortLegacyPlatformsByDisplayOrder() {
        Group red = group("1 RED", "RED", 1);
        Group white = group("1 WHITE", "WHITE", 2);
        Group blue = group("1 BLUE", "BLUE", 3);

        List<Group> groups = new ArrayList<>(Arrays.asList(blue, red, white));
        groups.sort(Group.groupWeighinTimeComparator(sessionBlocksConfig()));

        assertEquals(Arrays.asList(red, white, blue), groups);
    }

    @Test
    public void sessionBlocksSortUnnumberedPlatformsAlphabetically() {
        Group a = group("1 A", "A", null);
        Group b = group("1 B", "B", null);

        List<Group> groups = new ArrayList<>(Arrays.asList(b, a));
        groups.sort(Group.groupWeighinTimeComparator(sessionBlocksConfig()));

        assertEquals(Arrays.asList(a, b), groups);
    }

    @Test
    public void sessionBlocksSortCustomPlatformsByDisplayOrder() {
        Group scarlet = group("1 SCARLET", "SCARLET", 1);
        Group gray = group("1 GRAY", "GRAY", 2);

        List<Group> groups = new ArrayList<>(Arrays.asList(gray, scarlet));
        groups.sort(Group.groupWeighinTimeComparator(sessionBlocksConfig()));

        assertEquals(Arrays.asList(scarlet, gray), groups);
    }

    private Config sessionBlocksConfig() {
        Config config = new Config();
        config.setFeatureSwitchValue(FeatureSwitch.USAW_SESSION_BLOCKS, true);
        return config;
    }

    private Group group(String groupName, String platformName, Integer displayOrder) {
        Platform platform = new Platform(platformName);
        platform.setDisplayOrder(displayOrder);
        Group group = new Group(groupName);
        group.setPlatform(platform);
        return group;
    }
}