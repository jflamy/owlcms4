/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.displays;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.group.Group;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.ui.home.MainNavigationLayout;

/**
 * The Class DisplayNavigationLayout.
 */
@SuppressWarnings("serial")
public class DisplayNavigationLayout extends MainNavigationLayout {


	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		HorizontalLayout announcerBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		announcerBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		announcerBar.setJustifyContentMode(JustifyContentMode.START);
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 20em");

		ComboBox<FieldOfPlayState> fopSelect = new ComboBox<>();
		
		fopSelect.setPlaceholder("Select Platform");
		fopSelect.setItems(OwlcmsFactory.getFOPs());
		fopSelect.setItemLabelGenerator(FieldOfPlayState::getName);
		fopSelect.setWidth("10rem");
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			OwlcmsSession.withFop((fop) -> {
				Group group = e.getValue().getGroup();
				Group currentGroup = fop.getGroup();
				if (group == null) {
					fop.switchGroup(null);
				} else if (!group.equals(currentGroup)) {
					fop.switchGroup(group);
				}
			});
		});
		
		Label label = new Label("Start Displays");
		appLayout.setTitleComponent(label);
		
		Label fopLabel = new Label("Competition Platform");
		fopLabel.getStyle().set("font-size", "small");
		fopLabel.getStyle().set("text-align", "right");

		HorizontalLayout appBar = new HorizontalLayout(fopLabel, fopSelect);
		appBar.setSpacing(true);
		appBar.setAlignItems(FlexComponent.Alignment.CENTER);
		appLayout.setAppBar(appBar);

		return appLayout;
	}

	public ComboBox<FieldOfPlayState> keep() {
		ComboBox<FieldOfPlayState> fopSelect = new ComboBox<>();
		
		fopSelect.setPlaceholder("Select Platform");
		fopSelect.setItems(OwlcmsFactory.getFOPs());
		fopSelect.setItemLabelGenerator(FieldOfPlayState::getName);
		fopSelect.setWidth("10rem");
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			OwlcmsSession.withFop((fop) -> {
				Group group = e.getValue().getGroup();
				Group currentGroup = fop.getGroup();
				if (group == null) {
					fop.switchGroup(null);
				} else if (!group.equals(currentGroup)) {
					fop.switchGroup(group);
				}
			});
		});
		return fopSelect;
	}
	
	

}
