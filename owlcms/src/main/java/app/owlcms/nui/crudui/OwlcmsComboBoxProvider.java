/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.crudui;

import java.util.Collection;

import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;

@SuppressWarnings("serial")
public class OwlcmsComboBoxProvider<T> extends ComboBoxProvider<T> {

    private boolean clearButtonVisible = false;
    private ItemLabelGenerator<T> itemLabelGenerator;

    public OwlcmsComboBoxProvider(Collection<T> items) {
        super(items);
    }

    public OwlcmsComboBoxProvider(String caption, Collection<T> items) {
        super(caption, items);
        this.clearButtonVisible = true;
    }

    public OwlcmsComboBoxProvider(String caption, Collection<T> items,
            ComponentRenderer<? extends Component, T> renderer, ItemLabelGenerator<T> itemLabelGenerator) {
        super(caption, items, renderer, itemLabelGenerator);
        this.itemLabelGenerator = itemLabelGenerator;
        this.clearButtonVisible = true;
    }

    public OwlcmsComboBoxProvider(String caption, Collection<T> items,
            ComponentRenderer<? extends Component, T> renderer, ItemLabelGenerator<T> itemLabelGenerator,
            boolean clearButtonVisible) {
        super(caption, items, renderer, itemLabelGenerator);
        this.itemLabelGenerator = itemLabelGenerator;
        this.clearButtonVisible = clearButtonVisible;
    }

    @Override
    protected ComboBox<T> buildAbstractListing() {
        ComboBox<T> field = new ComboBox<>();
        if (renderer != null) {
            field.setRenderer(renderer);
        }
        if (itemLabelGenerator != null) {
            field.setItemLabelGenerator(itemLabelGenerator);
        }
        field.setClearButtonVisible(clearButtonVisible);
        field.setItems(items);
        return field;
    }
}
