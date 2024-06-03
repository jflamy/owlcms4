package app.owlcms.components;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

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

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GroupSelectionMenu extends MenuBar {

	static Icon xIcon;
	Logger logger = (Logger) LoggerFactory.getLogger(GroupSelectionMenu.class);
	{
		xIcon = new Icon(VaadinIcon.CLOSE_SMALL);
		xIcon.getElement().setAttribute("style", "margin: 0px; padding: 0px");
	}

	public GroupSelectionMenu(List<Group> groups, Group curGroup, FieldOfPlay fop2,
	        Consumer<Group> whenChecked, Consumer<Group> whenUnselected) {
		this(groups, curGroup, fop2, whenChecked, whenUnselected, xIcon, Translator.translate("NoGroup"), true);
	}

	public GroupSelectionMenu(List<Group> groups, Group curGroup, FieldOfPlay fop2,
	        Consumer<Group> whenChecked, Consumer<Group> whenUnselected, Icon unselectedIcon, String unselectedLabel, boolean doneSeparator) {
		MenuItem item;
		if (curGroup != null) {
			// logger.debug(curGroup.toString());
			item = this.addItem(curGroup.getName() + "\u2003\u25bd");
			this.addThemeVariants(MenuBarVariant.LUMO_SMALL);
		} else {
			// logger.debug("null group");
			item = this.addItem(Translator.translate("Group") + "\u2003\u25bc");
			this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);
		}
		SubMenu subMenu = item.getSubMenu();
		MenuItem currentlyChecked[] = { null };
		Group prevGroup = null;
		for (Group g : groups) {
			if (doneSeparator && prevGroup != null && (prevGroup.isDone() != g.isDone())) {
				addSeparator(subMenu);
			}
			MenuItem subItem = subMenu.addItem(
			        describedName(g),
			        e -> {
			        	if (!e.isFromClient()) {
			        		return;
			        	}
				        whenChecked.accept(g);
				        if (currentlyChecked[0] != null) {
					        currentlyChecked[0].setChecked(false);
				        }
				        e.getSource().setChecked(true);
				        currentlyChecked[0] = e.getSource();
				        item.setText(g.getName() + "\u2003\u25bd");
				        // logger.debug("selected {}",g.getName());
				        this.removeThemeVariants(MenuBarVariant.LUMO_PRIMARY);
			        });
			subItem.setCheckable(true);
			subItem.setChecked(g.compareTo(curGroup) == 0);
			subItem.getElement().setAttribute("style", "margin: 0px; padding: 0px");
			if (g.compareTo(curGroup) == 0) {
				currentlyChecked[0] = subItem;
			}
			prevGroup = g;
		}
		addSeparator(subMenu);

		HorizontalLayout hl = new HorizontalLayout();
		if (unselectedIcon != null) {
			hl.add(unselectedIcon);
		}
		hl.add(new NativeLabel(unselectedLabel));

		hl.setPadding(false);
		hl.setMargin(false);
		hl.getElement().setAttribute("style", "margin: 0; padding: 0");
		hl.setAlignItems(Alignment.CENTER);
		MenuItem item3 = subMenu.addItem(hl,
		        e -> {
		        	if (!e.isFromClient()) {
		        		return;
		        	}
			        if (currentlyChecked[0] != null) {
				        currentlyChecked[0].setChecked(false);
			        }
			        whenUnselected.accept(null);
			        item.setText(Translator.translate("Group") + "\u2003\u25bd");
			        this.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);
		        });
		item3.setCheckable(false);
		item.setEnabled(true);
	}

	private void addSeparator(SubMenu subMenu) {
		Hr ruler = new Hr();
		ruler.getElement().setAttribute("style", "width: 100%; height: 2px; color: black");
		MenuItem separator = subMenu.addItem(ruler);
		separator.getElement().setAttribute("style",
		        "margin-top: -1em; margin-bottom: -1em; margin-left: -1em; margin-right: 0em; padding: 0px; padding-left: -1em; width: 100%");
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
