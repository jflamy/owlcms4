package com.github.appreciated.layout;

import com.github.appreciated.css.grid.FluentGridLayoutComponent;
import com.github.appreciated.css.grid.HasOverflow;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.css.grid.sizes.Span;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;

/**
 * A Layout which makes it easy for the user to create flexible grids.
 */
public class FlexibleGridLayout extends Composite<GridLayout> implements FluentGridLayoutComponent<FlexibleGridLayout>, HasOverflow<FlexibleGridLayout> {
    public FlexibleGridLayout() {

    }

    /**
     * Convenience method, for users which don't want to dive into the css-grid to set the number of row an item should span over
     *
     * @param component the component which column width should be set
     * @param width     the number of columns the item should span over
     */
    public FlexibleGridLayout withItemWithWidth(Component component, int width) {
        getContent().add(component);
        setItemWidth(component, width);
        return this;
    }

    /**
     * Convenience method, for users which don't want to dive into the css-grid to set the number of row an item should span over
     *
     * @param component the component which column width should be set
     * @param width     the number of columns the item should span over
     */
    public void setItemWidth(Component component, int width) {
        getContent().setColumn(component, new Span(width));
    }

    /**
     * Adds an item to the layout and also sets the width and height of the passed item
     *
     * @param component the component which column width should be set
     * @param width     the number of columns the item should span over
     * @param height    the number of rows the item should span over
     * @return
     */
    public FlexibleGridLayout withItemWithSize(Component component, int width, int height) {
        getContent().add(component);
        setItemSize(component, width, height);
        return this;
    }

    /**
     * Sets the width and height of the passed item.
     *
     * @param component the component which column width should be set
     * @param width     the number of columns the item should span over
     * @param height    the number of rows the item should span over
     */
    public void setItemSize(Component component, int width, int height) {
        setItemRowHeight(component, height);
        setItemWidth(component, width);
    }

    /**
     * Sets the height of the passed item.
     *
     * @param component the component which row height should be set
     * @param height    the number of rows the item should span over
     */
    public void setItemRowHeight(Component component, int height) {
        getContent().setRow(component, new Span(height));
    }

    /**
     * Shorthand fluent style method for adding a component and setting its height
     *
     * @param component the components to add
     * @param height    the number of rows the item should span over
     * @return this
     */
    public FlexibleGridLayout withItemWithRowHeight(Component component, int height) {
        getContent().add(component);
        setItemRowHeight(component, height);
        return this;
    }

    /**
     * Shorthand to set the <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template-columns">grid-template-columns</a> with flexible columns by using the <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/repeat">repeat</a> function with an auto-repeat mode.
     *
     * @param mode  the auto-repeat mode. (See under auto-fill and auto-repeat of the <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/repeat">repeat</a> function)
     * @param units the width the columns.
     * @return
     */
    public FlexibleGridLayout withColumns(Repeat.RepeatMode mode, TemplateRowsAndColsUnit units) {
        getContent().setTemplateColumns(new Repeat(mode, units));
        return this;
    }

    /**
     * @param units
     * @return
     */
    public FlexibleGridLayout withRows(TemplateRowsAndColsUnit... units) {
        getContent().setTemplateRows(units);
        return this;
    }

    /**
     * Sets the height of the rows with the height {@link com.github.appreciated.css.grid.sizes.Auto}. By default all rows receive that height.
     *
     * @param length the size that will be set
     * @return this
     */
    public FlexibleGridLayout withAutoRows(Length length) {
        getContent().setAutoRows(length);
        return this;
    }

    /**
     * Sets how the grid should behave weather keeping the order or filling up unused space with smaller elements using this makes sense when adding differently sized elements <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow">grid-auto-flow</a>.
     *
     * @param flow
     * @return
     */
    public FlexibleGridLayout withAutoFlow(AutoFlow flow) {
        getContent().setAutoFlow(flow);
        return this;
    }

    /**
     * Sets the row and column gap between the items
     *
     * @param gap the size that will be set
     * @return this
     */
    public FlexibleGridLayout withGap(Length gap) {
        getContent().setGap(gap);
        return this;
    }

    /**
     * Adds components to the layout
     *
     * @param components the components that are supposed to be added
     * @return this
     */
    public FlexibleGridLayout withItems(Component... components) {
        getContent().add(components);
        return this;
    }


}
