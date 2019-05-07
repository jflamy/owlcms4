/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.category;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Enum AgeDivision.
 */
public enum AgeGroups {

	M30, M35, M40, M45, M50, M55, M65, M70, M75, 
	M75PLUS {
		@Override
		public String toString() {
			return "M75+";
		}
	},
	W30, W35, W40, W45, W50, W55, W65, W70, W70PLUS {
		@Override
		public String toString() {
			return "W70+";
		}
	};


	/**
	 * Find all.
	 *
	 * @return the collection
	 */
	public static Collection<AgeGroups> findAll() {
		return Arrays.asList(AgeGroups.values());
	}
	
	/**
	 * Find all.
	 *
	 * @return the collection
	 */
	public static List<String> findAllStrings() {
		return Arrays.asList(AgeGroups.values()).stream().map((v) -> v.toString()).collect(Collectors.toList());
	}
}
