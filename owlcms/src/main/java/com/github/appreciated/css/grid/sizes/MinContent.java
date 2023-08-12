package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.AutoRowAndColUnit;
import com.github.appreciated.css.grid.interfaces.MinMaxUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

public class MinContent implements AutoRowAndColUnit, TemplateRowsAndColsUnit, MinMaxUnit {
    @Override
    public String getValue() {
        return "min-content";
    }
}
