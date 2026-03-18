/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;
import app.owlcms.init.OwlcmsSessionThreadLocal;

@SuppressWarnings("serial")
public class GroupCategorySelectionMenu extends MenuBar {

	@FunctionalInterface
	public interface TriConsumer<F, S, V> {

		/**
		 * Performs this operation on the given arguments.
		 *
		 * @param F the first input argument
		 * @param S the second input argument
		 * @param V the third input argument
		 */
		void accept(F f, S s, V v);
	}

	Logger logger = (Logger) LoggerFactory.getLogger(GroupCategorySelectionMenu.class);
	private boolean includeNotCompleted;
	private List<Group> groups;
	private FieldOfPlay fop;
	private TriConsumer<Group, Category, FieldOfPlay> whenChecked;
	private TriConsumer<Group, Category, FieldOfPlay> whenUnselected;
	boolean subMenuLoaded = false;
	Map<Group, Set<String>> medalCategoriesPerGroup = new HashMap<>();

	public GroupCategorySelectionMenu(List<Group> groups, FieldOfPlay fop,
	        TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected) {

		this.groups = groups;
		this.fop = fop;
		this.whenChecked = whenChecked;
		this.whenUnselected = whenUnselected;
	}

	public void fillMenu(List<Group> groups, Map<Group, Set<String>> medalCategoriesPerGroup, FieldOfPlay fop,
	        TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected, MenuItem item, String menuTitle) {
		if (fop == null) {
			this.logger.debug("fillMenu called with null FieldOfPlay");
		}
		SubMenu subMenu = item.getSubMenu();
		for (Group g : groups) {
			Set<String> categories = medalCategoriesPerGroup.get(g);
			if (categories != null && categories.size() > 0) {
				MenuItem subItem = subMenu.addItem(
				        g.getName(),
				        e -> {
					        whenChecked.accept(g, null, fop);
					        setChecked(e.getSource(), subMenu, true);
					        item.setText(g.getName() + "\u2003\u25bd");
				        });

				subItem.setCheckable(true);
				if (fop != null && fop.getGroup() != null && g.compareTo(fop.getGroup()) == 0) {
					setChecked(subItem, subMenu, true);
				}
				subItem.getElement().setAttribute("style", "margin: 0px; padding: 0px");

				this.logger.debug("medal categories {}", categories);
				for (String c : categories) {

					Category cat = CategoryRepository.findByCode(c);
					MenuItem subItem1 = subMenu.addItem(
					        g.getName() + " - " + cat.getNameWithAgeGroup(),
					        e -> {
						        whenChecked.accept(g, cat, fop);
						        setChecked(e.getSource(), subMenu, true);
						        item.setText(g.getName() + " - " + cat.getNameWithAgeGroup() + "\u2003\u25bd");
					        });
					subItem1.setCheckable(true);
					subItem1.getElement().setAttribute("style", "margin: 0px; padding: 0px");
				}
			}
		}
		Hr ruler = new Hr();
		ruler.getElement().setAttribute("style",
		        "color: var(--lumo-contrast-50pct); border-color: red; var(--lumo-contrast-50pct): var(--lumo-contrast-50pct)");
		MenuItem separator = subMenu.addItem(ruler);
		separator.getElement().setAttribute("style",
		        "margin-top: -1em; margin-bottom: -1.5em; margin-left: -1.5em; padding: 0px; padding-left: -1em;");
		Icon icon = new Icon(VaadinIcon.CLOSE_SMALL);
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

	public void recompute() {
		UI ui = this.getUI().get();
		this.removeAll();
		this.subMenuLoaded = false;
		init(this.groups, this.fop, this.whenChecked, this.whenUnselected, ui);
	}

	public void setIncludeNotCompleted(Boolean value) {
		this.includeNotCompleted = value;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		this.subMenuLoaded = false;
		init(this.groups, this.fop, this.whenChecked, this.whenUnselected, this.getUI().get());
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

	private Set<String> getAllCategories(Group g) {
		TreeMap<String, List<Athlete>> medals = Competition.getCurrent().getMedals(g, false);
		return medals.keySet();
	}

	private Set<String> getFinishedCategories(Group g) {
		Set<String> finishedCategories = new TreeSet<>();
		TreeMap<String, List<Athlete>> medals = Competition.getCurrent().getMedals(g, true);
		finishedCategories = medals.keySet();
		return finishedCategories;
	}

	private void init(List<Group> groups, FieldOfPlay fop, TriConsumer<Group, Category, FieldOfPlay> whenChecked,
	        TriConsumer<Group, Category, FieldOfPlay> whenUnselected, UI ui) {

		MenuItem item;
		String menuTitle = Translator.translate("Group") + "/" + Translator.translate("Category") + "\u2003\u25bc";
		this.setId("sessionDropDown");
		item = this.addItem(menuTitle);
		this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);
		this.setEnabled(false);

		if (!this.subMenuLoaded) {
			this.medalCategoriesPerGroup.clear();
			// Explicitly capture parameters for thread closure
			final List<Group> capturedGroups = groups;
			final FieldOfPlay capturedFop = fop;
			final TriConsumer<Group, Category, FieldOfPlay> capturedWhenChecked = whenChecked;
			final TriConsumer<Group, Category, FieldOfPlay> capturedWhenUnselected = whenUnselected;
			final MenuItem capturedItem = item;
			final String capturedMenuTitle = menuTitle;
			
			new Thread(() -> {
				try {
					for (Group g : capturedGroups) {
						Set<String> categories = this.includeNotCompleted ? getAllCategories(g) : getFinishedCategories(g);
						if (!categories.isEmpty()) {
							this.medalCategoriesPerGroup.put(g, categories);
						}
					}
					this.subMenuLoaded = true;
					ui.access(() -> {
						fillMenu(capturedGroups, this.medalCategoriesPerGroup, capturedFop, capturedWhenChecked, capturedWhenUnselected, capturedItem, capturedMenuTitle);
						this.setEnabled(true);
					});
				} finally {
					// Defensive cleanup of thread-local state copied to this thread via InheritableThreadLocal
					try { OwlcmsSessionThreadLocal.remove(); } catch (Throwable ignore) {}
				}
			}).start();
		}

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
