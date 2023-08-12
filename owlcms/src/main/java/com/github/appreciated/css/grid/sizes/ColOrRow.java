package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.RowOrColUnit;

public class ColOrRow implements RowOrColUnit {

    private String colOrRowName;

    public ColOrRow(String colOrRowName) {
        this.colOrRowName = colOrRowName;
    }

    @Override
    public String getValue() {
        return colOrRowName;
    }
}
