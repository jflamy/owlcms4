package com.github.appreciated.css.grid.sizes;

import java.util.Objects;

import com.github.appreciated.css.grid.exception.NegativeOrZeroValueException;
import com.github.appreciated.css.grid.interfaces.RowOrColUnit;
import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;

public class Span implements RowOrColUnit, TemplateAreaUnit {

    private CustomIdent area;
    private Integer span;

    public Span(int span) {
        if (span < 1) {
            throw new NegativeOrZeroValueException(span);
        }
        this.span = span;
    }

    public Span(CustomIdent area) {
        Objects.requireNonNull(area);
        this.area = area;
    }

    @Override
    public String getValue() {
        if (span != null && area != null) {
            return span + " " + area.getCssValue();
        } else if (span != null) {
            return String.valueOf(span);
        } else {
            return area.getCssValue();
        }
    }

    @Override
    public boolean hasPrefix() {
        return true;
    }

    @Override
    public String getPrefixValue() {
        return "span ";
    }
}
