/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.treegrid.TreeGrid;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.CeremonyScope;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.init.OwlcmsSessionThreadLocal;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;

@SuppressWarnings("serial")
@Route(value = "lifting/medalCeremony", layout = OwlcmsLayout.class)
public class MedalCeremonyContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {
	private static final int MEDAL_DISPLAY_HEIGHT = 670;
	private static final int LARGE_SPACER_HEIGHT = 16;
	private static final int SMALL_SPACER_HEIGHT = 8;
	private static final int CATEGORY_HEADER_HEIGHT = 32;
	private static final int TABLE_HEADER_HEIGHT = 28;
	private static final int ATHLETE_ROW_HEIGHT = 27;

	private record AwardCategory(Category category, int medalists) {
	}

	private static class AwardNode {
		private final Category category;
		private final AgeGroup ageGroup;
		private final Championship championship;
		private final List<AwardNode> children = new ArrayList<>();
		private final String label;
		private final int medalists;

		AwardNode(String label, Championship championship, AgeGroup ageGroup, Category category) {
			this(label, championship, ageGroup, category, 0);
		}

		AwardNode(String label, Championship championship, AgeGroup ageGroup, Category category, int medalists) {
			this.label = label;
			this.championship = championship;
			this.ageGroup = ageGroup;
			this.category = category;
			this.medalists = medalists;
		}
	}

	private TreeGrid<AwardNode> awardTree;
	private Button endMedalCeremony;
	private Group medalGroup;
	private Category medalCategory;
	private AgeGroup medalAgeGroup;
	private Championship medalChampionship;
	private Button startMedalCeremony;

	@Override
	public String getMenuTitle() {
		return Translator.translate("PublicMsg.Medals");
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("PublicMsg.Medals") + suffix;
	}

	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		NativeLabel fopLabel = new NativeLabel(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
		FieldOfPlay currentFop = getFop();
		if (currentFop == null) {
			currentFop = OwlcmsSession.getFop();
		}
		if (currentFop == null) {
			currentFop = OwlcmsFactory.getDefaultFOP();
		}
		fopSelect.setValue(currentFop);
		fopSelect.addValueChangeListener(e -> {
			setFop(e.getValue());
			OwlcmsSession.setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		FieldOfPlay currentFop = getFop();
		if (currentFop == null) {
			return;
		}
		this.uiEventBus = uiEventBusRegister(this, currentFop);
		this.medalGroup = latestCompletedSession(currentFop);
		CeremonyScope scope = currentFop.getActiveCeremony();
		if (scope != null && scope.isMedals()) {
			this.medalGroup = scope.session();
			this.medalCategory = scope.category();
			this.medalAgeGroup = scope.ageGroup();
			this.medalChampionship = scope.championship();
		}
		setSizeFull();

		this.startMedalCeremony = new Button(Translator.translate("BreakMgmt.startMedals"),
		        new Icon(VaadinIcon.PLAY), event -> startMedalCeremony());
		this.endMedalCeremony = new Button(Translator.translate("BreakMgmt.endMedals"),
		        new Icon(VaadinIcon.STOP), event -> endMedalCeremony());
		HorizontalLayout controls = new HorizontalLayout(this.startMedalCeremony, this.endMedalCeremony);
		controls.setAlignItems(Alignment.CENTER);

		VerticalLayout controlsColumn = new VerticalLayout();
		controlsColumn.setPadding(true);
		controlsColumn.setSpacing(true);
		controlsColumn.add(new NativeLabel(Translator.translate("Session")), createSessionSelector(currentFop), controls);
		controlsColumn.setWidth("35%");
		controlsColumn.setHeightFull();

		this.awardTree = createAwardTree();
		VerticalLayout awardsColumn = new VerticalLayout(this.awardTree);
		awardsColumn.setPadding(true);
		awardsColumn.setSpacing(true);
		awardsColumn.setSizeFull();
		awardsColumn.setMinHeight("0");
		awardsColumn.setFlexGrow(1.0, this.awardTree);

		HorizontalLayout content = new HorizontalLayout(controlsColumn, awardsColumn);
		content.setSizeFull();
		content.setMinHeight("0");
		content.setFlexGrow(1.0, awardsColumn);
		add(content);
		setFlexGrow(1.0, content);
		refreshAwardTree();
		updateButtonState();
	}

	private TreeGrid<AwardNode> createAwardTree() {
		TreeGrid<AwardNode> tree = new TreeGrid<>();
		tree.setSelectionMode(SelectionMode.SINGLE);
		tree.addHierarchyColumn(node -> node.label).setHeader(Translator.translate("Championship.Medals"))
		        .setWidth("20rem").setFlexGrow(0);
		tree.addComponentColumn(this::createShowScopeButton).setAutoWidth(true).setFlexGrow(0);
		tree.setSizeFull();
		tree.asSingleSelect().addValueChangeListener(event -> {
			AwardNode selected = event.getValue();
			this.medalCategory = selected != null ? selected.category : null;
			this.medalAgeGroup = selected != null ? selected.ageGroup : null;
			this.medalChampionship = selected != null ? selected.championship : null;
			updateButtonState();
		});
		return tree;
	}

	private Button createShowScopeButton(AwardNode node) {
		Button button = new Button(Translator.translate("Results.Start"), event -> showAwardScope(node));
		button.setAriaLabel(Translator.translate("DisplayParameters.PublicDisplay"));
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		if (fitsMedalDisplay(node)) {
			button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
		}
		return button;
	}

	private boolean fitsMedalDisplay(AwardNode node) {
		return estimatedHeight(node) <= MEDAL_DISPLAY_HEIGHT;
	}

	private int estimatedHeight(AwardNode node) {
		if (node.category != null) {
			return LARGE_SPACER_HEIGHT + SMALL_SPACER_HEIGHT + CATEGORY_HEADER_HEIGHT
			        + TABLE_HEADER_HEIGHT + node.medalists * ATHLETE_ROW_HEIGHT;
		}
		return node.children.stream().mapToInt(this::estimatedHeight).sum();
	}

	private HorizontalLayout createSessionSelector(FieldOfPlay fop) {
		MenuBar selector = new MenuBar();
		selector.addThemeVariants(MenuBarVariant.LUMO_SMALL, MenuBarVariant.LUMO_PRIMARY);
		Group selectedGroup = this.medalGroup;
		MenuItem root = selector.addItem(sessionLabel(selectedGroup));
		SubMenu menu = root.getSubMenu();
		MenuItem[] selectedItem = { null };

		if (selectedGroup != null) {
			selectedItem[0] = addSessionItem(menu, selectedGroup, root, selectedItem);
		}
		List<Group> completed = completedSessions();
		List<Group> fopCompleted = completed.stream()
		        .filter(group -> !group.equals(selectedGroup))
		        .filter(group -> Objects.equals(group.getPlatform(), fop.getPlatform()))
		        .toList();
		List<Group> otherCompleted = completed.stream()
		        .filter(group -> !group.equals(selectedGroup))
		        .filter(group -> !Objects.equals(group.getPlatform(), fop.getPlatform()))
		        .toList();
		boolean hasItems = selectedGroup != null;
		for (List<Group> section : List.of(fopCompleted, otherCompleted)) {
			if (section.isEmpty()) {
				continue;
			}
			if (hasItems) {
				addSeparator(menu);
			}
			for (Group group : section) {
				addSessionItem(menu, group, root, selectedItem);
			}
			hasItems = true;
		}
		Button clearSession = new Button(new Icon(VaadinIcon.CLOSE_SMALL), event -> {
			if (selectedItem[0] != null) {
				selectedItem[0].setChecked(false);
				selectedItem[0] = null;
			}
			this.medalGroup = null;
			this.medalCategory = null;
			this.medalAgeGroup = null;
			this.medalChampionship = null;
			root.setText(sessionLabel(null));
			refreshAwardTree();
			updateButtonState();
		});
		clearSession.setAriaLabel(Translator.translate("Clear"));
		clearSession.setTooltipText(Translator.translate("Clear"));
		clearSession.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		HorizontalLayout sessionControls = new HorizontalLayout(selector, clearSession);
		sessionControls.setAlignItems(Alignment.CENTER);
		return sessionControls;
	}

	private MenuItem addSessionItem(SubMenu menu, Group group, MenuItem root, MenuItem[] selectedItem) {
		MenuItem item = menu.addItem(sessionLabel(group), event -> {
			if (!event.isFromClient()) {
				return;
			}
			if (selectedItem[0] != null) {
				selectedItem[0].setChecked(false);
			}
			event.getSource().setChecked(true);
			selectedItem[0] = event.getSource();
			root.setText(sessionLabel(group));
			this.medalGroup = group;
			this.medalCategory = null;
			this.medalAgeGroup = null;
			this.medalChampionship = null;
			refreshAwardTree();
			updateButtonState();
		});
		item.setCheckable(true);
		item.setChecked(group.equals(this.medalGroup));
		item.getElement().getStyle().set("margin", "0px").set("padding", "0px");
		return item;
	}

	private void addSeparator(SubMenu menu) {
		Hr separator = new Hr();
		separator.getElement().getStyle().set("width", "100%").set("height", "2px");
		MenuItem item = menu.addItem(separator);
		item.getElement().getStyle().set("margin-top", "-1em").set("margin-bottom", "-1em")
		        .set("margin-left", "-1em").set("padding", "0px");
	}

	private List<Group> completedSessions() {
		return GroupRepository.findAll().stream()
		        .filter(Group::isDone)
		        .filter(group -> completionTime(group) != null)
		        .sorted(Comparator.comparing(this::completionTime, Comparator.reverseOrder())
		                .thenComparing(Group::getName, Comparator.nullsLast(Comparator.reverseOrder())))
		        .toList();
	}

	private LocalDateTime completionTime(Group group) {
		if (group.getLastCJDecisionTime() != null) {
			return group.getLastCJDecisionTime();
		}
		if (group.getLastSnatchDecisionTime() != null) {
			return group.getLastSnatchDecisionTime();
		}
		return null;
	}

	private Group latestCompletedSession(FieldOfPlay fop) {
		return completedSessions().stream()
		        .filter(group -> Objects.equals(group.getPlatform(), fop.getPlatform()))
		        .findFirst().orElse(null);
	}

	private String sessionLabel(Group group) {
		return group != null ? group.getName() + "\u2003\u25be" : Translator.translate("AllGroups") + "\u2003\u25be";
	}

	private void refreshAwardTree() {
		Group selectedGroup = this.medalGroup;
		UI ui = getUI().orElse(null);
		if (ui == null) {
			return;
		}
		new Thread(() -> {
			try {
				List<AwardCategory> categories = awardCategories(selectedGroup);
				ui.access(() -> {
					if (isAttached() && Objects.equals(selectedGroup, this.medalGroup)) {
						List<AwardNode> roots = awardHierarchy(categories);
						this.awardTree.setItems(roots, node -> node.children);
						this.awardTree.expand(roots);
						roots.forEach(root -> root.children.forEach(this::expandOversizedScopes));
					}
				});
			} finally {
				OwlcmsSessionThreadLocal.remove();
			}
		}, "MedalCeremonyAwards").start();
	}

	private void expandOversizedScopes(AwardNode node) {
		if (node.children.isEmpty() || fitsMedalDisplay(node)) {
			return;
		}
		this.awardTree.expand(List.of(node));
		node.children.forEach(this::expandOversizedScopes);
	}

	private List<AwardCategory> awardCategories(Group group) {
		List<Group> sessions = group != null ? List.of(group) : completedSessions();
		Map<Category, Integer> medalistsByCategory = new HashMap<>();
		for (Group session : sessions) {
			for (List<Athlete> medalists : Competition.getCurrent().getMedals(session, true).values()) {
				Category category = medalists.isEmpty() ? null : medalists.get(0).getCategory();
				if (category == null || category.getAgeGroup() == null || !category.getAgeGroup().getMedals()) {
					continue;
				}
				int displayed = (int) medalists.stream()
				        .filter(athlete -> athlete.getGroup() != null && athlete.isMedalist())
				        .count();
				if (displayed > 0) {
					medalistsByCategory.merge(category, displayed, Math::max);
				}
			}
		}
		return medalistsByCategory.entrySet().stream()
		        .sorted(Map.Entry.comparingByKey(Category.medalingComparator()))
		        .map(entry -> new AwardCategory(entry.getKey(), entry.getValue()))
		        .toList();
	}

	private List<AwardNode> awardHierarchy(List<AwardCategory> categories) {
		Map<Championship, Map<AgeGroup, List<AwardCategory>>> championships = new TreeMap<>();
		for (AwardCategory awardCategory : categories) {
			Category category = awardCategory.category();
			AgeGroup ageGroup = category.getAgeGroup();
			Championship championship = ageGroup.getChampionship();
			championships.computeIfAbsent(championship, unused -> new TreeMap<>())
			        .computeIfAbsent(ageGroup, unused -> new ArrayList<>()).add(awardCategory);
		}

		List<AwardNode> roots = new ArrayList<>();
		for (Map.Entry<Championship, Map<AgeGroup, List<AwardCategory>>> championship : championships.entrySet()) {
			Map<AgeGroup, List<AwardCategory>> ageGroups = championship.getValue();
			if (ageGroups.size() == 1) {
				Map.Entry<AgeGroup, List<AwardCategory>> ageGroup = ageGroups.entrySet().iterator().next();
				roots.add(ageGroupNode(ageGroup.getKey(), ageGroup.getValue()));
			} else {
				AwardNode championshipNode = new AwardNode(championship.getKey().getName(), championship.getKey(), null, null);
				for (Map.Entry<AgeGroup, List<AwardCategory>> ageGroup : ageGroups.entrySet()) {
					championshipNode.children.add(ageGroupNode(ageGroup.getKey(), ageGroup.getValue()));
				}
				roots.add(championshipNode);
			}
		}
		AwardNode allChampionships = new AwardNode(Translator.translate("MedalCeremony.AllChampionships"),
		        null, null, null);
		allChampionships.children.addAll(roots);
		return List.of(allChampionships);
	}

	private AwardNode ageGroupNode(AgeGroup ageGroup, List<AwardCategory> categories) {
		AwardNode node = new AwardNode(ageGroup.getName(), ageGroup.getChampionship(), ageGroup, null);
		for (AwardCategory awardCategory : categories) {
			Category category = awardCategory.category();
			node.children.add(new AwardNode(category.getLimitString(), ageGroup.getChampionship(), ageGroup,
			        category, awardCategory.medalists()));
		}
		return node;
	}

	private void showAwardScope(AwardNode node) {
		FieldOfPlay currentFop = getFop();
		if (currentFop == null) {
			return;
		}
		startBreakIfNeeded(currentFop);
		currentFop.fopEventPost(new FOPEvent.CeremonyStarted(CeremonyType.MEDALS, this.medalGroup, node.championship,
		        node.ageGroup, node.category, this));
		this.medalCategory = node.category;
		this.medalAgeGroup = node.ageGroup;
		this.medalChampionship = node.championship;
		updateButtonState();
	}

	private void startMedalCeremony() {
		FieldOfPlay currentFop = getFop();
		if (currentFop == null) {
			return;
		}
		startBreakIfNeeded(currentFop);
		currentFop.fopEventPost(new FOPEvent.CeremonyStarted(
		        CeremonyType.MEDALS, this.medalGroup, this.medalChampionship,
		        this.medalAgeGroup, this.medalCategory, this));
		updateButtonState();
	}

	private void endMedalCeremony() {
		FieldOfPlay currentFop = getFop();
		if (currentFop == null || currentFop.getCeremonyType() != CeremonyType.MEDALS) {
			return;
		}
		currentFop.fopEventPost(new FOPEvent.CeremonyDone(CeremonyType.MEDALS, this));
		updateButtonState();
	}

	private void startBreakIfNeeded(FieldOfPlay fop) {
		if (fop.getState() != FOPState.BREAK && fop.getState() != FOPState.INACTIVE) {
			fop.getBreakTimer().setIndefinite();
			fop.setWeightAtLastStart(0);
			fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.FIRST_SNATCH, CountdownType.INDEFINITE,
			        null, null, true, this));
		}
	}

	private void updateButtonState() {
		if (this.startMedalCeremony == null || this.endMedalCeremony == null) {
			return;
		}
		FieldOfPlay currentFop = getFop();
		boolean ceremonyActive = currentFop != null && currentFop.getCeremonyType() == CeremonyType.MEDALS;
		this.startMedalCeremony.setEnabled(currentFop != null);
		this.endMedalCeremony.setEnabled(ceremonyActive);
		this.startMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.endMedalCeremony.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		if (ceremonyActive) {
			this.endMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		} else if (this.medalGroup != null) {
			this.startMedalCeremony.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}
	}

	@Subscribe
	private void ceremonyStarted(UIEvent.CeremonyStarted event) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, event, this::updateButtonState);
	}

	@Subscribe
	private void ceremonyDone(UIEvent.CeremonyDone event) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, event, this::updateButtonState);
	}
}