/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.appLayout.AppLayoutContent;
import app.owlcms.ui.home.MainNavigationLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@SuppressWarnings("serial")
public class WeighinLayout extends MainNavigationLayout {

	private final static Logger logger = (Logger)LoggerFactory.getLogger(WeighinLayout.class);
	static {logger.setLevel(Level.DEBUG);}
	
	private HorizontalLayout topBar;
	private H2 groupName;
	private ComboBox<Group> gridGroupFilter;
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		this.topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		createTopBar(topBar);
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px");
		return appLayout;
	}
	
	/**
	 * The layout is created before the content. This routine has created the content, we can refer to
	 * the content using {@link #getLayoutContent()} and the content can refer to us via
	 * {@link AppLayoutContent#getParentLayout()}
	 * 
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		WeighinContent weighinContent = (WeighinContent) getLayoutContent();
		weighinContent.setParentLayout(this);
		gridGroupFilter = weighinContent.getGroupFilter();
	}
	
	/**
	 * Create the top bar.
	 * 
	 * Note: the top bar is created before the content.
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 * 
	 * @param announcerBar
	 */
	protected void createTopBar(HorizontalLayout announcerBar) {
		logger.warn("createTopBar");
		logger.warn("layout = {}", this.toString());
//		logger.warn("content = {}", this.getLayoutContent().toString());

		
		groupName = new H2();
		ComboBox<Group> groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.addValueChangeListener(e -> {
			gridGroupFilter.setValue(e.getValue());
		});
//		groupName.setText("\u2013");
		groupName.add(groupSelect);
		groupName.getStyle()
			.set("margin", "0px 0px 0px 0px");

		HorizontalLayout buttons = new HorizontalLayout(
				new Button("Generate Start Numbers", (e) -> {
					generateStartNumbers();
				}),
				new Button("Clear Start Numbers", (e) -> {
					clearStartNumbers();
				}),
				new Button("Generate Protocol Sheet (starting weights)", (e) -> {
					generateProtocolSheet();
				}));
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		announcerBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		announcerBar.removeAll();
		announcerBar.add(groupName, buttons);
		announcerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		announcerBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	private void generateProtocolSheet() {
		// TODO generateProtocolSheet
	}

	private void clearStartNumbers() {
		// TODO clearStartNumbers
		
	}

	private void generateStartNumbers() {
		// TODO generateStartNumbers
		
	}
}
