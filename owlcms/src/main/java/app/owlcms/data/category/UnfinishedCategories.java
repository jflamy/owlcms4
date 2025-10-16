/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.category;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper class for unfinished categories.
 * Maintains a set of category display names for categories that have not yet finished.
 */
public class UnfinishedCategories {
	private final Set<String> categoryDisplayNames;

	/**
	 * Constructor that creates an empty set of unfinished categories.
	 */
	public UnfinishedCategories() {
		this.categoryDisplayNames = new HashSet<>();
	}

	/**
	 * Add a category to the unfinished set if not already present.
	 * Uses the category's display name as the canonical string.
	 * 
	 * @param category Category to add
	 * @return true if the category was added (wasn't already present)
	 */
	public boolean add(Category category) {
		if (category == null || category.getDisplayName() == null) {
			return false;
		}
		String canonicalString = category.getDisplayName();
		return categoryDisplayNames.add(canonicalString);
	}

	/**
	 * Get the set of unfinished category display names.
	 * 
	 * @return Set of category display names
	 */
	public Set<String> getCategoryDisplayNames() {
		return new HashSet<>(categoryDisplayNames);
	}

	/**
	 * Check if a category is in the unfinished set.
	 * 
	 * @param category Category to check
	 * @return true if the category is unfinished
	 */
	public boolean contains(Category category) {
		if (category == null || category.getDisplayName() == null) {
			return false;
		}
		return categoryDisplayNames.contains(category.getDisplayName());
	}

	/**
	 * Get the count of unfinished categories.
	 * 
	 * @return number of unfinished categories
	 */
	public int size() {
		return categoryDisplayNames.size();
	}

	/**
	 * Check if there are no unfinished categories.
	 * 
	 * @return true if the set is empty
	 */
	public boolean isEmpty() {
		return categoryDisplayNames.isEmpty();
	}

	@Override
	public String toString() {
		return categoryDisplayNames.toString();
	}
}
