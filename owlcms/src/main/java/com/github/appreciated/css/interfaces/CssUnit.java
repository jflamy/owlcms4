package com.github.appreciated.css.interfaces;

public interface CssUnit {
	default String getCssValue() {
		return (hasPrefix() ? getPrefixValue() : "") + getValue() + (hasSuffix() ? getSuffixValue() : "");
	}

	default String getPrefixValue() {
		return null;
	}

	default String getSuffixValue() {
		return null;
	}

	default String getValue() {
		return null;
	}

	default boolean hasPrefix() {
		return false;
	}

	default boolean hasSuffix() {
		return false;
	}
}
