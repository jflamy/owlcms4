/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.crudui;

import java.util.Collection;

import org.vaadin.crudui.form.impl.field.provider.AbstractListingProvider;
import org.vaadin.gatanaso.MultiselectComboBox;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.data.renderer.ComponentRenderer;

@SuppressWarnings("serial")
public class OwlcmsMultiSelectComboBoxProvider<T> extends AbstractListingProvider<MultiselectComboBox<T>, T> {

    private ItemLabelGenerator<T> itemLabelGenerator;

    public OwlcmsMultiSelectComboBoxProvider(Collection<T> items) {
        super(items);
    }

    public OwlcmsMultiSelectComboBoxProvider(String caption, Collection<T> items) {
        super(caption, items);
    }

    public OwlcmsMultiSelectComboBoxProvider(String caption, Collection<T> items,
            ComponentRenderer<? extends Component, T> renderer, ItemLabelGenerator<T> itemLabelGenerator) {
        super(caption, items, renderer);
        this.itemLabelGenerator = itemLabelGenerator;
    }

    @Override
    protected MultiselectComboBox<T> buildAbstractListing() {
        MultiselectComboBox<T> field = new MultiselectComboBox<>();
        if (renderer != null) {
            field.setRenderer(renderer);
        }
        if (itemLabelGenerator != null) {
            field.setItemLabelGenerator(itemLabelGenerator);
        }
        field.setClearButtonVisible(true);
        field.setOrdered(true);
        return field;
    }
}
