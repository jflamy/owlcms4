package com.github.appreciated.layout;

import org.vaddon.css.query.MediaQuery;

import com.github.appreciated.css.grid.FluentGridLayoutComponent;
import com.github.appreciated.css.grid.HasOverflow;
import com.github.appreciated.css.grid.exception.NegativeValueException;
import com.github.appreciated.css.grid.interfaces.RowOrColUnit;
import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;
import com.github.appreciated.css.grid.sizes.Int;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.TemplateArea;
import com.github.appreciated.css.grid.sizes.TemplateAreas;
import com.vaadin.flow.component.Component;

@SuppressWarnings("serial")
public class FluentGridLayout extends GridLayout implements FluentGridLayoutComponent<FluentGridLayout>, HasOverflow<FluentGridLayout> {

    /**
     * Fluent method of {@link GridLayout#setTemplateColumns(TemplateRowsAndColsUnit...)}
     *
     * @param units
     * @return
     */
    public FluentGridLayout withTemplateColumns(TemplateRowsAndColsUnit... units) {
        setTemplateColumns(units);
        return this;
    }

    /**
     * Fluent method of {@link GridLayout#setTemplateColumns(TemplateRowsAndColsUnit...)}
     *
     * @param rows
     * @return
     */
    public FluentGridLayout withTemplateRows(TemplateRowsAndColsUnit... rows) {
        setTemplateRows(rows);
        return this;
    }

    /**
     * @param component
     * @param rowStart
     * @param colStart
     * @return
     */
    public FluentGridLayout withRowAndColumn(Component component, int rowStart, int colStart) {
        return withRowAndColumn(component, rowStart, colStart, rowStart, colStart);
    }

    /**
     * @param component
     * @param rowStart
     * @param colStart
     * @param rowEnd
     * @param colEnd
     * @return
     */
    public FluentGridLayout withRowAndColumn(Component component, int rowStart, int colStart, int rowEnd, int colEnd) {
        if (rowStart < 0 || colStart < 0 || rowEnd < 0 || colEnd < 0) {
            throw new NegativeValueException(rowStart, colStart, rowEnd, colEnd);
        }
        return withRowAndColumn(component, new Int(rowStart), new Int(colStart), new Int(rowEnd), new Int(colEnd));
    }

    /**
     * Shorthand fluent style method for adding a component and setting its area
     *
     * @param component
     * @param rowStart  row in which you want the component span to begin
     * @param colStart  col in which you want the component span to begin
     * @param rowEnd    row in which you want the component span to end
     * @param colEnd    col in which you want the component span to end
     * @return this
     */
    public FluentGridLayout withRowAndColumn(Component component, RowOrColUnit rowStart, RowOrColUnit colStart, RowOrColUnit rowEnd, RowOrColUnit colEnd) {
        add(component);
        setRowAndColumn(component, rowStart, colStart, rowEnd, colEnd);
        return this;
    }

    /**
     * Shorthand fluent style method for adding a component and setting its area
     *
     * @param component the component to add
     * @param area      the area the element should be assigned
     * @return this
     */
    public FluentGridLayout withRowAndColumn(Component component, TemplateArea area) {
        add(component);
        setArea(component, area);
        return this;
    }

    /**
     * Shorthand fluent style method for adding a component
     *
     * @param component the component to add
     * @return this
     */
    public FluentGridLayout withItem(Component component) {
        add(component);
        return this;
    }

    /**
     * Shorthand fluent style method for adding one or multiple components
     *
     * @param components the components to add
     * @return this
     */
    public FluentGridLayout withItems(Component... components) {
        add(components);
        return this;
    }

    /**
     * Shorthand fluent style method to set the width of the layout
     *
     * @param width the width that should be assigned
     * @return this
     */
    public FluentGridLayout withWidth(String width) {
        setWidth(width);
        return this;
    }

    /**
     * Fluent method of {@link GridLayout#setTemplateAreas(TemplateAreas[])} for setting the template areas available
     *
     * @param templateAreas the template areas you want to be assigned
     * @return this
     */
    public FluentGridLayout withTemplateAreas(TemplateAreas[] templateAreas) {
        setTemplateAreas(templateAreas);
        return this;
    }

    /**
     * Fluent method of {@link GridLayout#setTemplateAreas(TemplateAreas[])} for setting the template areas available
     *
     * @param templateAreas the template areas you want to be assigned
     * @return this
     */
    public FluentGridLayout withTemplateAreas(MediaQuery query, TemplateAreas... templateAreas) {
        setTemplateAreas(query, templateAreas);
        return this;
    }

    /**
     * @param component
     * @param columnAlign
     * @return
     */
    public FluentGridLayout withColumnAlign(Component component, ColumnAlign columnAlign) {
        setColumnAlign(component, columnAlign);
        return this;
    }

    /**
     * @param component
     * @param end
     * @return
     */
    public FluentGridLayout withRowAlign(Component component, RowAlign end) {
        setRowAlign(component, end);
        return this;
    }

    /**
     * @param size
     * @return
     */
    public FluentGridLayout withGap(Length size) {
        setGap(size);
        return this;
    }

    /**
     * @param size
     * @return
     */
    public FluentGridLayout withAutoRows(Length size) {
        setAutoRows(size);
        return this;
    }

    /**
     * @param size
     * @return
     */
    public FluentGridLayout withAutoColumns(Length size) {
        setAutoColumns(size);
        return this;
    }

    /**
     * @param component
     * @param unit
     * @return
     */
    public FluentGridLayout withColumnStart(Component component, RowOrColUnit unit) {
        setColumnStart(component, unit);
        return this;
    }

    /**
     * @param component
     * @param unit
     * @return
     */
    public FluentGridLayout withColumnEnd(Component component, RowOrColUnit unit) {
        setColumnEnd(component, unit);
        return this;
    }

    /**
     * @param component
     * @param unit
     * @return
     */
    public FluentGridLayout withRowStart(Component component, RowOrColUnit unit) {
        setRowStart(component, unit);
        return this;
    }

    /**
     * @param component
     * @param unit
     * @return
     */
    public FluentGridLayout withRowEnd(Component component, RowOrColUnit unit) {
        setRowEnd(component, unit);
        return this;
    }

    public FluentGridLayout withArea(Component component, TemplateAreaUnit unit) {
        setArea(component, unit);
        return this;
    }
}
