package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.AutoRowAndColUnit;
import com.github.appreciated.css.grid.interfaces.MinMaxUnit;
import com.github.appreciated.css.grid.interfaces.RowOrColUnit;
import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

/**
 * A class which mimics the css value "auto"
 */
public class Auto implements AutoRowAndColUnit, RowOrColUnit, TemplateRowsAndColsUnit, MinMaxUnit, TemplateAreaUnit {
    @Override
    public String getValue() {
        return "auto";
    }
}
