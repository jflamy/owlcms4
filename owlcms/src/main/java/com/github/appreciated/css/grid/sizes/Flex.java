package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.exception.NegativeValueException;
import com.github.appreciated.css.grid.interfaces.AutoRowAndColUnit;
import com.github.appreciated.css.grid.interfaces.MinMaxUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

/**
 *
 */
public class Flex implements AutoRowAndColUnit, TemplateRowsAndColsUnit, MinMaxUnit {

    private double fraction;

    public Flex() {
        this(1);
    }

    public Flex(double fraction) {
        if (fraction < 0) {
            throw new NegativeValueException(fraction);
        }
        this.fraction = fraction;
    }

    @Override
    public String getValue() {
        return String.valueOf(fraction);
    }

    @Override
    public boolean hasSuffix() {
        return true;
    }

    @Override
    public String getSuffixValue() {
        return "fr";
    }
}
