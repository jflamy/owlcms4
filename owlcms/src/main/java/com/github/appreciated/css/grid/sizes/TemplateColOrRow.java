package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;

import java.util.Objects;

public class TemplateColOrRow implements TemplateRowsAndColsUnit {

    private TemplateRowsAndColsUnit size;
    private String colOrRowName;

    public TemplateColOrRow(String colOrRowName) {
        this(colOrRowName, null);
    }

    public TemplateColOrRow(String colOrRowName, TemplateRowsAndColsUnit size) {
        Objects.requireNonNull(colOrRowName);
        this.colOrRowName = colOrRowName;
        this.size = size;
    }

    @Override
    public String getValue() {
        return "[" + colOrRowName + "]" + (size != null ? " " + size.getCssValue() : "");
    }
}
