/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;

@SuppressWarnings("serial")
public class LocalDateTimePicker extends CustomField<LocalDateTime> {

    private final DatePicker datePicker = new DatePicker();
    private final TimePicker timePicker = new TimePicker();

    public LocalDateTimePicker() {
        timePicker.getStyle().set("margin-left", "1em");
        add(datePicker, timePicker);
    }

    /**
     * @see com.vaadin.flow.component.AbstractField#getValue()
     */
    @Override
    public LocalDateTime getValue() {
        return generateModelValue();
    }

    @Override
    protected LocalDateTime generateModelValue() {
        final LocalDate date = datePicker.getValue();
        final LocalTime time = timePicker.getValue();
        return date != null && time != null ? LocalDateTime.of(date, time) : null;
    }

    @Override
    protected void setPresentationValue(LocalDateTime newPresentationValue) {
        datePicker.setValue(newPresentationValue != null ? newPresentationValue.toLocalDate() : null);
        timePicker.setValue(newPresentationValue != null ? newPresentationValue.toLocalTime() : null);
    }

}