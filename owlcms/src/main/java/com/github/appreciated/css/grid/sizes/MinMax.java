package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.AutoRowAndColUnit;
import com.github.appreciated.css.grid.interfaces.MinMaxUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

/**
 * A class which is supposed to mimic the minmax <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/minmax">minmax</a> css function
 */
public class MinMax implements AutoRowAndColUnit, TemplateRowsAndColsUnit {

    private final MinMaxUnit min;
    private final MinMaxUnit max;

    /**
     * @param min either the minimum width/height for a column or a row
     * @param max either the minimum width/height for a column or a row
     */
    public MinMax(MinMaxUnit min, MinMaxUnit max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getValue() {
        return min.getCssValue() + "," + max.getCssValue();
    }

    @Override
    public boolean hasSuffix() {
        return true;
    }

    @Override
    public String getSuffixValue() {
        return ")";
    }

    @Override
    public boolean hasPrefix() {
        return true;
    }

    @Override
    public String getPrefixValue() {
        return "minmax(";
    }
}
