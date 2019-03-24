/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.lifting;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.ui.home.MainNavigationLayout;

/**
 * Class LiftingNavigationLayout.
 */
@SuppressWarnings("serial")
public class LiftingNavigationLayout extends MainNavigationLayout {

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {

		AppLayout appLayout = super.getLayoutConfiguration(variant);

		ComboBox<FieldOfPlayState> fopSelect = new ComboBox<>();
		ComboBox<Group> groupSelect = new ComboBox<>();
		
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
				fop.setGroup(group);
				groupSelect.setValue(group);
			});
		});

		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setWidth("10rem");
		OwlcmsSession.withFop((fop) -> {
			groupSelect.setValue(fop.getGroup());
		});
		groupSelect.addValueChangeListener(e -> {
			OwlcmsSession.withFop((fop) -> {
				fop.setGroup(e.getValue());
			});
		});
		Label label = new Label("Run a Lifting Group");
		HorizontalLayout titleComponent = new HorizontalLayout(label,fopSelect, groupSelect);
		titleComponent.setSpacing(true);
		appLayout.setTitleComponent(titleComponent);
		
//		String fopName = (String) VaadinSession.getCurrent().getAttribute("fopName");
//		appLayout.setAppBar(new Label(fopName != null ? fopName : ""));
		return appLayout;
	}
}
