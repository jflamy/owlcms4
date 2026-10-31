/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * {@link AllTests} followed by {@link SlowTests}.
 */
@RunWith(Suite.class)
@SuiteClasses({ AllTests.class, SlowTests.class })
public class FullTests {
}
