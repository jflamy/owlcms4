package app.owlcms.ui.crudui;

import java.util.Collection;

import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;

@SuppressWarnings("serial")
public class OwlcmsComboBoxProvider<T> extends ComboBoxProvider<T> {
    
    private ItemLabelGenerator<T> itemLabelGenerator;

    public OwlcmsComboBoxProvider(Collection<T> items) {
        super(items);
    }

    public OwlcmsComboBoxProvider(String caption, Collection<T> items) {
        super(caption, items);
    }

    public OwlcmsComboBoxProvider(String caption, Collection<T> items, ComponentRenderer<? extends Component, T> renderer, ItemLabelGenerator<T> itemLabelGenerator) {
        super(caption, items, renderer, itemLabelGenerator);
        this.itemLabelGenerator = itemLabelGenerator;
    }

    @Override
    protected ComboBox<T> buildAbstractListing() {
        ComboBox<T> field = new ComboBox<>();
        if(renderer != null) {
            field.setRenderer(renderer);
        }
        if (itemLabelGenerator != null) {
            field.setItemLabelGenerator(itemLabelGenerator);
        }
        field.setClearButtonVisible(true);
        field.setItems(items);
        return field;
    }
}
