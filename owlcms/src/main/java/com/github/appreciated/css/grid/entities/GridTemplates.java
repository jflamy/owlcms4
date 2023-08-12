package com.github.appreciated.css.grid.entities;

public class GridTemplates {
    private final ColumnTemplate columnTemplate;
    private final RowTemplate rowTemplate;

    public GridTemplates(ColumnTemplate columnTemplate, RowTemplate rowTemplate) {
        this.columnTemplate = columnTemplate;
        this.rowTemplate = rowTemplate;
    }

    public ColumnTemplate getTemplateColumns() {
        return columnTemplate;
    }

    public RowTemplate getTemplateRows() {
        return rowTemplate;
    }
}
