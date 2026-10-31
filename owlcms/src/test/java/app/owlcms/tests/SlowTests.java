/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import org.junit.runner.RunWith;

import com.googlecode.junittoolbox.IncludeCategories;
import com.googlecode.junittoolbox.SuiteClasses;
import com.googlecode.junittoolbox.WildcardPatternSuite;

/**
 * The tests in the {@link SlowCategory} category, which {@link AllTests} leaves out.
 */
@RunWith(WildcardPatternSuite.class)
@SuiteClasses({ "../**/*Test.class" })
@IncludeCategories(SlowCategory.class)
public class SlowTests {
}
