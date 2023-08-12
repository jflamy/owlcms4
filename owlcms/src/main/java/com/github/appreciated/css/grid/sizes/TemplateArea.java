package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;

import java.util.Objects;

public class TemplateArea implements TemplateAreaUnit {

    private String area;

    public TemplateArea(String area) {
        Objects.requireNonNull(area);
        this.area = area;
    }

    @Override
    public String getValue() {
        return area;
    }
}
