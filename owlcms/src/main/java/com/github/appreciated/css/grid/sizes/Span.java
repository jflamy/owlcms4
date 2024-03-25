package com.github.appreciated.css.grid.sizes;

import java.util.Objects;

import com.github.appreciated.css.grid.exception.NegativeOrZeroValueException;
import com.github.appreciated.css.grid.interfaces.RowOrColUnit;
import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;

public class Span implements RowOrColUnit, TemplateAreaUnit {

	private CustomIdent area;
	private Integer span;

	public Span(CustomIdent area) {
		Objects.requireNonNull(area);
		this.area = area;
	}

	public Span(int span) {
		if (span < 1) {
			throw new NegativeOrZeroValueException(span);
		}
		this.span = span;
	}

	@Override
	public String getPrefixValue() {
		return "span ";
	}

	@Override
	public String getValue() {
		if (this.span != null && this.area != null) {
			return this.span + " " + this.area.getCssValue();
		} else if (this.span != null) {
			return String.valueOf(this.span);
		} else {
			return this.area.getCssValue();
		}
	}

	@Override
	public boolean hasPrefix() {
		return true;
	}
}
