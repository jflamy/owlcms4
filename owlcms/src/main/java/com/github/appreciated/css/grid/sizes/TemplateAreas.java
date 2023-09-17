package com.github.appreciated.css.grid.sizes;

import java.util.Arrays;
import java.util.Objects;

import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;
import com.github.appreciated.css.interfaces.CssUnit;

public class TemplateAreas implements TemplateAreaUnit {

    private TemplateArea[] areas;

    public TemplateAreas(String... areas) {
        this(Arrays.stream(areas).map(s -> new TemplateArea(s)).toArray(TemplateArea[]::new));
    }

    public TemplateAreas(TemplateArea... areas) {
        this.areas = areas;
        Objects.requireNonNull(areas);
    }

    @Override
    public String getValue() {
        return Arrays.stream(areas).map(CssUnit::getCssValue).reduce((s, s2) -> s + " " + s2).orElse("");
    }
}
