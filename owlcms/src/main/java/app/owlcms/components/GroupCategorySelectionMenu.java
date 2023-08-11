package app.owlcms.components;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
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
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GroupCategorySelectionMenu extends MenuBar {
	
	Logger logger = (Logger) LoggerFactory.getLogger(GroupCategorySelectionMenu.class);

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

	private boolean includeNotCompleted;
	private List<Group> groups;
	private FieldOfPlay fop;
	private TriConsumer<Group, Category, FieldOfPlay> whenChecked;
	private TriConsumer<Group, Category, FieldOfPlay> whenUnselected;

	public GroupCategorySelectionMenu(List<Group> groups, FieldOfPlay fop,
	        TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected) {
		
		this.groups = groups;
		this.fop = fop;
		this.whenChecked = whenChecked;
		this.whenUnselected = whenUnselected;
		init(groups, fop, whenChecked, whenUnselected);
	}

	private void init(List<Group> groups, FieldOfPlay fop, TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected) {
		MenuItem item;
		String menuTitle = Translator.translate("Group") + "/" + Translator.translate("Category") + "\u2003\u25bc";
		item = this.addItem(menuTitle);
		this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);

		SubMenu subMenu = item.getSubMenu();
		for (Group g : groups) {
			Set<Category> categories = includeNotCompleted ? getAllCategories(g) : getFinishedCategories(g);
			if (categories.size() > 0) {
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

			for (Category c : categories) {
				MenuItem subItem1 = subMenu.addItem(
				        g.getName() + " - " + c.getTranslatedName(),
				        e -> {
					        whenChecked.accept(g, c, fop);
					        setChecked(e.getSource(), subMenu, true);
					        item.setText(g.getName() + " - " + c.getTranslatedName() + "\u2003\u25bd");
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
		Icon icon = new Icon("ironicons","clear");// IronIcons.CLEAR.create();
		//FIXME IronIcon
		icon.getElement().setAttribute("style", "margin: 0px; padding: 0px");
		HorizontalLayout component = new HorizontalLayout(icon, new NativeLabel(Translator.translate("NoGroup")));
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

	private Set<Category> getAllCategories(Group g) {
		TreeMap<Category, TreeSet<Athlete>> medals = Competition.getCurrent().getMedals(g, false);
		return medals.keySet();
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

	public void setIncludeNotCompleted(Boolean value) {
		this.includeNotCompleted = value;
	}

	public void recompute() {
		this.removeAll();
		init(groups, fop, whenChecked, whenUnselected);
	}

}
