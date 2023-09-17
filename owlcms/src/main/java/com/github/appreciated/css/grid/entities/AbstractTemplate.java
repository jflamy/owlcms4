package com.github.appreciated.css.grid.entities;

import java.util.Arrays;

import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;
import com.github.appreciated.css.interfaces.CssUnit;

public abstract class AbstractTemplate implements CssUnit {
    private final TemplateRowsAndColsUnit[] units;

    public AbstractTemplate(TemplateRowsAndColsUnit... unit) {
        this.units = unit;
    }

    @Override
    public String getCssValue() {
        return Arrays.stream(units).map(CssUnit::getCssValue).reduce((unit, unit2) -> unit + " " + unit2).orElse("");
    }
}
