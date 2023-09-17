package com.github.appreciated.layout;

import java.util.Arrays;

import com.github.appreciated.css.grid.FluentGridLayoutComponent;
import com.github.appreciated.css.grid.sizes.TemplateArea;
import com.github.appreciated.css.grid.sizes.TemplateAreas;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;

/**
 * A Layout which makes it easy for the user to create area based grids.
 */
@SuppressWarnings("serial")
public class AreaLayout extends Composite<GridLayout> implements FluentGridLayoutComponent<AreaLayout> {

    /**
     * Shorthand to allow setting the <a href="https://developer.mozilla.org/de/docs/Web/CSS/grid-template-areas">grid-template-areas</a>
     *
     * @param areas
     */
    public AreaLayout(String[][] areas) {
        getContent().setTemplateAreas(Arrays.stream(areas).map(strings ->
                new TemplateAreas(strings)
        ).toArray(TemplateAreas[]::new));
    }

    /**
     * Short hand to add an item and set its <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/grid-area">grid-area</a>
     *
     * @param component
     * @param area
     * @return
     */
    public AreaLayout withItemAtArea(Component component, String area) {
        getContent().add(component);
        getContent().setArea(component, new TemplateArea(area));
        return this;
    }
}
