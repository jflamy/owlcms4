package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.AutoRowAndColUnit;
import com.github.appreciated.css.grid.interfaces.MinMaxUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

/**
 * A wrapper which is supposed to contain a <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/length">length</a> or a <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/percentage">percentage</a>
 *
 *
 */
public class Length implements TemplateRowsAndColsUnit, AutoRowAndColUnit, MinMaxUnit {

    private String size;

    public Length(String size) {
        this.size = size;
    }

    @Override
    public String getValue() {
        return size;
    }
}
