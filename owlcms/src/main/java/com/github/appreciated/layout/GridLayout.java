package com.github.appreciated.layout;

import com.github.appreciated.css.grid.GridLayoutComponent;
import com.github.appreciated.css.grid.entities.GridTemplates;
import com.github.appreciated.css.grid.interfaces.RowOrColUnit;
import com.github.appreciated.css.grid.interfaces.TemplateAreaUnit;
import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.css.grid.sizes.TemplateAreas;
import com.github.appreciated.css.interfaces.CssUnit;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Style;
import org.vaddon.ClientMediaQuery;
import org.vaddon.css.query.MediaQuery;

import java.util.Arrays;
import java.util.Objects;

/**
 * GridLayout is a component container, which shows the subcomponents in
 * the order of their addition. The {@link GridLayout} does not have a predefined size - its size is either defined
 * by the components inside it, or can be set by using the <a href="https://developer.mozilla.org/de/docs/Web/CSS/CSS_Grid_Layout">css grid api</a>
 * Different to the {@link com.vaadin.flow.component.orderedlayout.VerticalLayout} and {@link com.vaadin.flow.component.orderedlayout.HorizontalLayout} a {@link GridLayout} can span its elements over multiple rows.
 */
@Tag("grid-layout")
@JsModule("./com/github/appreciated/grid-layout/grid-layout.js")
public class GridLayout extends LitTemplate implements GridLayoutComponent {
    @Id("grid-layout-element")
    Div gridLayout;

    @Id("queries")
    Div queries;

    /**
     * @param components
     */
    public GridLayout(Component... components) {
        this.add(components);
    }

    public GridLayout() {
    }

    /**
     * @return
     */
    public String getColumnGap() {
        return gridLayout.getStyle().get("grid-column-gap");
    }

    /**
     * @param columnGap
     */
    public void setColumnGap(Length columnGap) {
        if (columnGap == null) {
            gridLayout.getStyle().remove("grid-column-gap");
        } else {
            gridLayout.getStyle().set("grid-column-gap", columnGap.getCssValue());
        }
    }

    /**
     * @return
     */
    public String getRowGap() {
        return gridLayout.getStyle().get("grid-row-gap");
    }

    /**
     * @param rowGap
     */
    public void setRowGap(Length rowGap) {
        if (rowGap == null) {
            gridLayout.getStyle().remove("grid-row-gap");
        } else {
            gridLayout.getStyle().set("grid-row-gap", rowGap.getCssValue());
        }
    }

    /**
     * @param columnGap
     * @param rowGap
     */
    public void setGap(Length columnGap, Length rowGap) {
        Objects.requireNonNull(columnGap);
        Objects.requireNonNull(rowGap);
        gridLayout.getStyle().set("grid-gap", columnGap.getCssValue() + " " + rowGap.getCssValue());
    }

    /**
     * @return
     */
    public String getGap() {
        return gridLayout.getStyle().get("grid-gap");
    }

    /**
     * @param gap
     */
    public void setGap(Length gap) {
        if (gap == null) {
            gridLayout.getStyle().remove("grid-gap");
        } else {
            gridLayout.getStyle().set("grid-gap", gap.getCssValue());
        }
    }

    /**
     * Sets the number of columns in your grid-layout.  <br>
     * #Allowed Values  <br>
     * Fixed number of rows can be combined with the following Classes:  <br>
     * Pixels / Percentage: {@link Length} ... | multiple rows having pixel valued size  <br>
     * Auto: {@link com.github.appreciated.css.grid.sizes.Auto} ... | 4 rows having the same size  <br>
     * Flexible row width: {@link com.github.appreciated.css.grid.sizes.Flex} | checkout the css grid documentation <br>
     * {@link Repeat#Repeat(int, TemplateRowsAndColsUnit...)} | checkout the css grid documentation <br>
     * #Dynamic Number of rows  <br>
     * {@link com.github.appreciated.css.grid.sizes.Repeat#Repeat(Repeat.RepeatMode, TemplateRowsAndColsUnit...)}
     *
     * @param units "The column definition in your grid layout, can either be fixed or dynamic"
     */
    public void setTemplateColumns(TemplateRowsAndColsUnit... units) {
        String value = Arrays.stream(units).map(CssUnit::getCssValue).reduce((s, s2) -> s + " " + s2).orElse("");
        gridLayout.getStyle().set("grid-template-columns", value);
    }

    /**
     * Sets the number of rows in your grid-layout.  <br>
     * #Allowed Values  <br>
     * Fixed number of rows:  <br>
     * Pixels: 100px 200px 300px 400px | 4 rows having pixel valued size  <br>
     * Auto: auto auto auto auto | 4 rows having the same size  <br>
     * Fr: auto 1fr auto | checkout the css grid documentation <br>
     * #Dynamic Number of rows  <br>
     * Other: repeat(auto-fill, minmax(250px, 1fr));  <br>
     *
     * @param units "The row definition in your grid layout, can either be fixed or dynamic checkout the official css grid documentation for further details"
     */
    public void setTemplateRows(TemplateRowsAndColsUnit... units) {
        if (units == null) {
            gridLayout.getStyle().remove("grid-template-rows");
        } else {
            gridLayout.getStyle().set("grid-template-rows", Arrays.stream(units).map(CssUnit::getCssValue).reduce((s, s2) -> s + " " + s2).orElse(""));
        }
    }

    /**
     * @param template
     */
    public void setTemplate(GridTemplates template) {
        if (template == null) {
            gridLayout.getStyle().remove("grid-template");
        } else {
            gridLayout.getStyle().set("grid-template", template.getTemplateRows().getCssValue() + " / " + template.getTemplateColumns().getCssValue());
        }
    }

    /**
     * @return
     */
    public String getGrid() {
        return gridLayout.getStyle().get("grid");
    }

    /**
     * @param grid
     */
    public void setGrid(String grid) {
        if (grid == null) {
            gridLayout.getStyle().remove("grid");
        } else {
            gridLayout.getStyle().set("grid", grid);
        }
    }

    /**
     * @param component
     */
    public void getRow(Component component) {
        component.getElement().getStyle().get("grid-row");
    }

    /**
     * @param component
     * @param column
     * @param row
     */
    public void setColumnAndRow(Component component, RowOrColUnit column, RowOrColUnit row) {
        setColumn(component, column);
        setRow(component, row);
    }

    /**
     * Sets the 'grid-column' value of an element
     *
     * @param component
     * @param column
     */
    public void setColumn(Component component, RowOrColUnit column) {
        if (column == null) {
            component.getElement().getStyle().remove("grid-column");
        } else {
            component.getElement().getStyle().set("grid-column", column.getCssValue());
        }
    }

    /**
     * @param component
     * @param row
     */
    public void setRow(Component component, RowOrColUnit row) {
        if (row == null) {
            component.getElement().getStyle().remove("grid-row");
        } else {
            component.getElement().getStyle().set("grid-row", row.getCssValue());
        }
    }

    /**
     * @param component
     * @return
     */
    public String getColumnEnd(Component component) {
        return component.getElement().getStyle().get("grid-column-end");
    }

    /**
     * @param component
     * @return
     */
    public String getColumnStart(Component component) {
        return component.getElement().getStyle().get("grid-column-start");
    }

    /**
     * @param component
     * @return
     */
    public String getRowEnd(Component component) {
        return component.getElement().getStyle().get("grid-row-end");
    }

    /**
     * @param component
     * @return
     */
    public String getRowStart(Component component) {
        return component.getElement().getStyle().get("grid-row-start");
    }

    /**
     * @param component
     * @param area
     */
    public void setArea(Component component, TemplateAreaUnit... area) {
        if (area == null) {
            component.getElement().getStyle().remove("grid-area");
        } else if (area.length > 4) {
            throw new IllegalArgumentException("A maximum of 4 arguments for row/columns can be passed");
        } else {
            component.getElement().getStyle().set("grid-area", Arrays.stream(area).map(CssUnit::getCssValue)
                    .reduce((s, s2) -> s + " / " + s2)
                    .orElse(""));
        }
    }

    public void setTemplateAreas(MediaQuery queries, TemplateAreas... areas) {
        ClientMediaQuery mediaQuery = new ClientMediaQuery(gridLayout);
        setTemplateAreas(mediaQuery.getQueryStyle(), areas);
        mediaQuery.setQuery(queries);
        this.queries.add(mediaQuery);
    }

    private void setTemplateAreas(Style style, TemplateAreas[] templateAreas) {
        if (templateAreas == null) {
            style.remove("grid-template-areas");
        } else {
            String areas = Arrays.stream(templateAreas)
                    .map(s -> "'" + s.getCssValue() + "'")
                    .reduce((s, s2) -> s + " " + s2)
                    .orElse("");
            style.set("grid-template-areas", areas);
        }
    }

    /**
     * Sets the column and row definition of your grid-layout. Instead of setting the sizes for rows and columns you
     * define areas by using custom keywords. <br>
     * <p>
     * Example:   <br>
     * 'header header header header header header' <br>
     * 'menu main main main right right'  <br>
     * 'menu footer footer footer footer footer';  <br>
     * <p>
     * In the second step you set the area for each item which will then span over the above defined area. Use therefore {@link GridLayout#setArea(Component, TemplateAreaUnit...)}
     *
     * @param templateAreas
     */
    public void setTemplateAreas(TemplateAreas[] templateAreas) {
        setTemplateAreas(gridLayout.getStyle(), templateAreas);
    }

    /**
     * @param component
     * @param rowStart
     * @param colStart
     * @param rowEnd
     * @param colEnd
     */
    public void setRowAndColumn(Component component, RowOrColUnit rowStart, RowOrColUnit colStart, RowOrColUnit rowEnd, RowOrColUnit colEnd) {
        setRowStartAndEnd(component, rowStart, rowEnd);
        setColumnStartAndEnd(component, colStart, colEnd);
    }

    /**
     * @param component
     * @param start
     * @param end
     */
    public void setRowStartAndEnd(Component component, RowOrColUnit start, RowOrColUnit end) {
        setRowStart(component, start);
        setRowEnd(component, end);
    }

    /**
     * @param component
     * @param start
     * @param end
     */
    public void setColumnStartAndEnd(Component component, RowOrColUnit start, RowOrColUnit end) {
        setColumnStart(component, start);
        setColumnEnd(component, end);
    }

    /**
     * @param component
     * @param rowStart
     */

    public void setRowStart(Component component, RowOrColUnit rowStart) {
        if (rowStart == null) {
            component.getElement().getStyle().remove("grid-row-start");
        } else {
            component.getElement().getStyle().set("grid-row-start", rowStart.getCssValue());
        }
    }

    /**
     * @param component
     * @param rowEnd
     */
    public void setRowEnd(Component component, RowOrColUnit rowEnd) {
        if (rowEnd == null) {
            component.getElement().getStyle().remove("grid-row-end");
        } else {
            component.getElement().getStyle().set("grid-row-end", rowEnd.getCssValue());
        }
    }

    /**
     * @param component
     * @param colStart
     */
    public void setColumnStart(Component component, RowOrColUnit colStart) {
        if (colStart == null) {
            component.getElement().getStyle().remove("grid-column-start");
        } else {
            component.getElement().getStyle().set("grid-column-start", colStart.getCssValue());
        }
    }

    /**
     * @param component
     * @param colEnd
     */
    public void setColumnEnd(Component component, RowOrColUnit colEnd) {
        if (colEnd == null) {
            component.getElement().getStyle().remove("grid-column-end");
        } else {
            component.getElement().getStyle().set("grid-column-end", colEnd.getCssValue());
        }
    }

    /**
     * @param component
     * @return
     */
    public String getArea(Component component) {
        return component.getElement().getStyle().get("grid-area");
    }

    /**
     * @return
     */
    public String getAutoColumns() {
        return gridLayout.getStyle().get("grid-auto-columns");
    }

    /**
     * @param size
     */
    public void setAutoColumns(Length size) {
        if (size == null) {
            gridLayout.getStyle().remove("grid-auto-columns");
        } else {
            gridLayout.getStyle().set("grid-auto-columns", size.getCssValue());
        }
    }

    /**
     * @return
     */
    public AutoFlow getAutoFlow() {
        return AutoFlow.toAutoFlow(gridLayout.getStyle().get(AutoFlow.cssProperty));
    }

    /**
     * Sets how the grid should behave weather keeping the order or filling up unused space with smaller elements using this makes sense when adding differently sized elements <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/grid-auto-flow">grid-auto-flow</a>
     *
     * @param autoFlow
     */
    public void setAutoFlow(AutoFlow autoFlow) {
        if (autoFlow == null) {
            gridLayout.getStyle().remove(AutoFlow.cssProperty);
        } else {
            gridLayout.getStyle().set(AutoFlow.cssProperty, autoFlow.getAutoFlowValue());
        }
    }

    /**
     * @return
     */
    public String getAutoRows() {
        return gridLayout.getStyle().get("grid-auto-rows");
    }

    /**
     * @param size
     */
    public void setAutoRows(Length size) {
        if (size == null) {
            gridLayout.getStyle().remove("grid-auto-rows");
        } else {
            gridLayout.getStyle().set("grid-auto-rows", size.getCssValue());
        }
    }

    public Div getGridLayout() {
        return gridLayout;
    }
}

