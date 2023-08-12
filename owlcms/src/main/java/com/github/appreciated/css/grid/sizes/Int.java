package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.RowOrColUnit;

public class Int implements RowOrColUnit {
    private int value;

    public Int(int value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }
}
