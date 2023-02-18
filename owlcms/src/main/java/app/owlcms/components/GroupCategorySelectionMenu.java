package app.owlcms.components;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class GroupCategorySelectionMenu extends MenuBar {

	@FunctionalInterface
	public interface TriConsumer<T, U, V> {

		/**
		 * Performs this operation on the given arguments.
		 *
		 * @param t the first input argument
		 * @param u the second input argument
		 */
		void accept(T t, U u, V v);
	}

	public GroupCategorySelectionMenu(List<Group> groups, FieldOfPlay fop,
	        TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected) {
		MenuItem item;
//        if (fop.getGroup() != null) {
//            item = this.addItem(fop.getGroup().getName() + "\u2003\u25bd");
//            this.addThemeVariants(MenuBarVariant.LUMO_SMALL);
//        } else {
		String menuTitle = Translator.translate("Group") + "/" + Translator.translate("Category") + "\u2003\u25bc";
		item = this.addItem(menuTitle);
		this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);
//        }
		SubMenu subMenu = item.getSubMenu();
		for (Group g : groups) {
			Set<Category> finishedCategories = getFinishedCategories(g);
			if (finishedCategories.size() > 0) {
				MenuItem subItem = subMenu.addItem(
				        g.getName(),
				        e -> {
					        whenChecked.accept(g, null, fop);
					        setChecked(e.getSource(), subMenu, true);
					        item.setText(g.getName() + "\u2003\u25bd");
				        });

				subItem.setCheckable(true);
				if (g.compareTo(fop.getGroup()) == 0) {
					setChecked(subItem, subMenu, true);
				}
				subItem.getElement().setAttribute("style", "margin: 0px; padding: 0px");
			}

			for (Category c : finishedCategories) {
				MenuItem subItem1 = subMenu.addItem(
				        g.getName() + " - " + c.getName(),
				        e -> {
					        whenChecked.accept(g, c, fop);
					        setChecked(e.getSource(), subMenu, true);
					        item.setText(g.getName() + " - " + c.getName() + "\u2003\u25bd");
				        });
				subItem1.setCheckable(true);
				subItem1.getElement().setAttribute("style", "margin: 0px; padding: 0px");
			}
		}
		Hr ruler = new Hr();
		ruler.getElement().setAttribute("style",
		        "color: var(--lumo-contrast-50pct); border-color: red; var(--lumo-contrast-50pct): var(--lumo-contrast-50pct)");
		MenuItem separator = subMenu.addItem(ruler);
		separator.getElement().setAttribute("style",
		        "margin-top: -1em; margin-bottom: -1.5em; margin-left: -1.5em; padding: 0px; padding-left: -1em;");
		Icon icon = IronIcons.CLEAR.create();
		icon.getElement().setAttribute("style", "margin: 0px; padding: 0px");
		HorizontalLayout component = new HorizontalLayout(icon, new Label(Translator.translate("NoGroup")));
		component.setPadding(false);
		component.setMargin(false);
		component.getElement().setAttribute("style", "margin: 0; padding: 0");
		component.setAlignItems(Alignment.CENTER);
		MenuItem item3 = subMenu.addItem(component,
		        e -> {
			        setChecked(e.getSource(), subMenu, false);
			        whenUnselected.accept(null, null, fop);
			        item.setText(menuTitle);
		        });
		item3.setCheckable(false);
		item.setEnabled(true);
	}

	@SuppressWarnings("unused")
	private String describedName(Group g) {
		String desc = g.getDescription();
		if (desc == null || desc.isBlank()) {
			return g.getName();
		} else {
			return g.getName() + " - " + g.getDescription();
		}
	}

	private Set<Category> getFinishedCategories(Group g) {
		Set<Category> finishedCategories = new TreeSet<>();
		TreeMap<Category, TreeSet<Athlete>> medals = Competition.getCurrent().getMedals(g, true);
		finishedCategories = medals.keySet();
		return finishedCategories;
	}

	private void setChecked(MenuItem menuItem, SubMenu subMenu, boolean checked) {
		for (MenuItem item : subMenu.getItems()) {
			item.setChecked(false);
		}
		if (checked) {
			menuItem.setChecked(checked);
		}
	}

}
