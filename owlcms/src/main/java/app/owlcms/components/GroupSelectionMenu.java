package app.owlcms.components;

import java.util.List;
import java.util.function.BiConsumer;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons.Icon;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class GroupSelectionMenu extends MenuBar {
    
    public GroupSelectionMenu(List<Group> groups, FieldOfPlay fop, BiConsumer<Group,FieldOfPlay> whenChecked, BiConsumer<Group,FieldOfPlay> whenUnselected) {
        MenuItem item;
        if (fop.getGroup() != null) {
            item = this.addItem(fop.getGroup().getName()+"\u2003\u25bd");
            this.addThemeVariants(MenuBarVariant.LUMO_SMALL);
        } else {
            item = this.addItem(Translator.translate("Group")+"\u2003\u25bc");
            this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY); 
        }
        SubMenu subMenu = item.getSubMenu();
        MenuItem currentlyChecked[] = {null};
        for (Group g : groups) {
            MenuItem subItem = subMenu.addItem(
                    describedName(g),
                    e -> whenChecked.accept(g,fop));
            subItem.setCheckable(true);
            subItem.setChecked(g.compareTo(fop.getGroup()) == 0);
            subItem.getElement().setAttribute("style", "margin: 0px; padding: 0px");
            if (g.compareTo(fop.getGroup()) == 0) {
                currentlyChecked[0] = subItem;
            }
        }
        Hr ruler = new Hr();
        ruler.getElement().setAttribute("style", "color: var(--lumo-contrast-50pct); border-color: red; var(--lumo-contrast-50pct): var(--lumo-contrast-50pct)");
        MenuItem separator = subMenu.addItem(ruler);
        separator.getElement().setAttribute("style", "margin-top: -1em; margin-bottom: -1.5em; margin-left: -1.5em; padding: 0px; padding-left: -1em;");
        Icon icon = IronIcons.CLEAR.create();
        icon.getElement().setAttribute("style", "margin: 0px; padding: 0px");
        HorizontalLayout component = new HorizontalLayout(icon,new Label(Translator.translate("NoGroup")));
        component.setPadding(false);
        component.setMargin(false);
        component.getElement().setAttribute("style", "margin: 0; padding: 0");
        component.setAlignItems(Alignment.CENTER);
        MenuItem item3 = subMenu.addItem(component, 
                e -> {
                    if (currentlyChecked[0] != null) {
                        currentlyChecked[0].setChecked(false);
                    }
                    whenUnselected.accept(null,fop);
                    
                });
        item3.setCheckable(false);
        item.setEnabled(true);
    }

    
    private String describedName(Group g) {
        String desc = g.getDescription();
        if (desc == null || desc.isBlank()) {
            return g.getName();
        } else {
            return g.getName() + " - " + g.getDescription();
        }
    }

}
