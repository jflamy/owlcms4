package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.*;

/**
 * A class which mimics the css value "auto"
 */
public class Auto implements AutoRowAndColUnit, RowOrColUnit, TemplateRowsAndColsUnit, MinMaxUnit, TemplateAreaUnit {
    @Override
    public String getValue() {
        return "auto";
    }
}
