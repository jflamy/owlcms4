/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.home;

import static com.github.appreciated.app.layout.entity.Section.FOOTER;
import static com.github.appreciated.app.layout.entity.Section.HEADER;
import static com.github.appreciated.app.layout.notification.entitiy.Priority.MEDIUM;

import java.util.function.Consumer;

import org.ledocte.owlcms.init.OwlcmsFactory;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.ui.displaySetup.DisplayNavigationContent;
import org.ledocte.owlcms.ui.lifting.LiftingNavigationContent;
import org.ledocte.owlcms.ui.preparation.PreparationNavigationContent;
import org.ledocte.owlcms.ui.wrapup.WrapupNavigationContent;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.github.appreciated.app.layout.builder.AppLayoutBuilder;
import com.github.appreciated.app.layout.component.appbar.AppBarBuilder;
import com.github.appreciated.app.layout.component.appmenu.MenuHeaderComponent;
import com.github.appreciated.app.layout.component.appmenu.left.LeftClickableComponent;
import com.github.appreciated.app.layout.component.appmenu.left.LeftNavigationComponent;
import com.github.appreciated.app.layout.component.appmenu.left.builder.LeftAppMenuBuilder;
import com.github.appreciated.app.layout.component.appmenu.top.TopNavigationComponent;
import com.github.appreciated.app.layout.component.appmenu.top.builder.TopAppMenuBuilder;
import com.github.appreciated.app.layout.entity.DefaultBadgeHolder;
import com.github.appreciated.app.layout.notification.DefaultNotificationHolder;
import com.github.appreciated.app.layout.notification.component.AppBarNotificationButton;
import com.github.appreciated.app.layout.notification.entitiy.DefaultNotification;
import com.github.appreciated.app.layout.notification.entitiy.Priority;
import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

/**
 * The Class MainNavigationLayout.
 */
@Push
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@StyleSheet("frontend://styles/owlcms.css")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/av-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/hardware-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/social-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/places-icons.html")
public class MainNavigationLayout extends AppLayoutRouterLayout {

	private static final long serialVersionUID = 1L;
	
	/** The notification holder. */
	DefaultNotificationHolder notificationHolder;
	
	/** The badge holder. */
	DefaultBadgeHolder badgeHolder;
	private Behaviour variant;
	private Thread currentThread;

	/* (non-Javadoc)
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayout#createAppLayoutInstance()
	 */
	@Override
	public AppLayout createAppLayoutInstance() {
		OwlcmsSession.setAttribute("fop", OwlcmsFactory.getDefaultFOP());
		
		if (variant == null) {
			variant = Behaviour.LEFT_OVERLAY;
//			notificationHolder = new DefaultNotificationHolder(newStatus -> {
//				/* Do something with it */});
//			badgeHolder = new DefaultBadgeHolder();
		}
		reloadNotifications();

		if (!variant.isTop()) {
			LeftNavigationComponent home = new LeftNavigationComponent("Home",
					VaadinIcon.HOME.create(),
					MainNavigationContent.class);

//			notificationHolder.bind(home.getBadge());

			AppLayout appLayout = AppLayoutBuilder
				.get(variant)
				.withTitle("OWLCMS - Olympic Weightlifting Competition Management System")
				.withIcon("/frontend/images/logo.png")
				.withAppBar(AppBarBuilder
					.get()
//					.add(new AppBarNotificationButton(VaadinIcon.BELL, notificationHolder))
					.build())
				.withAppMenu(LeftAppMenuBuilder
					.get()
					.addToSection(new MenuHeaderComponent("OWLCMS", null, null), HEADER)
					.add(home)
					.add(new LeftNavigationComponent("Prepare Competition",
							new Icon("social", "group-add"),
							PreparationNavigationContent.class))
					.add(new LeftNavigationComponent("Setup Displays",
							new Icon("hardware", "desktop-windows"),
							DisplayNavigationContent.class))
					.add(new LeftNavigationComponent("Lifting Group",
							new FullIronIcon("places", "fitness-center"),
							LiftingNavigationContent.class))
					.add(new LeftNavigationComponent("Competition Documents",
							new Icon("maps", "local-printshop"),
							WrapupNavigationContent.class))
					.addToSection(new LeftClickableComponent("Preferences",
							VaadinIcon.COG.create(),
							clickEvent -> openModeSelector(variant)),
						FOOTER)
					.build())
				.build();
			return appLayout;
		} else {
			return AppLayoutBuilder
				.get(variant)
				.withTitle("OWLCMS")
				.withAppBar(AppBarBuilder
					.get()
					.add(new AppBarNotificationButton(VaadinIcon.BELL, notificationHolder))
					.build())
				.withAppMenu(TopAppMenuBuilder
					.get()
					.add(new TopNavigationComponent("Prepare Competition",
							new Icon("social", "group-add"),
							PreparationNavigationContent.class))
					.add(new TopNavigationComponent("Setup Displays",
							new Icon("hardware", "desktop-windows"),
							DisplayNavigationContent.class))
					.add(new TopNavigationComponent("Lifting Group",
							new FullIronIcon("places", "fitness-center"),
							LiftingNavigationContent.class))
					.add(new TopNavigationComponent("Competition Documents",
							new Icon("maps", "local-printshop"),
							WrapupNavigationContent.class))
					.build())
				.build();
		}
	}

//	@Override
//	protected void onAttach(AttachEvent attachEvent) {
//		super.onAttach(attachEvent);
//		getUI().get()
//			.getPage()
//			.executeJavaScript(
//				"document.documentElement.setAttribute(\"theme\",\"dark\")");
//	}

	@SuppressWarnings("unused")
	private void reloadNotifications() {
		if (!(0 == 1)) return;
		if (currentThread != null && !currentThread.isInterrupted()) {
			currentThread.interrupt();
		}
		badgeHolder.clearCount();
		notificationHolder.clearNotifications();
		currentThread = new Thread(() -> {
			try {
				Thread.sleep(1000);
				for (int i = 0; i < 3; i++) {
					// Thread.sleep(5000);
					getUI().ifPresent(ui -> ui.access(() -> {
						addNotification(MEDIUM);
						badgeHolder.increase();
						badgeHolder.increase();
					}));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		currentThread.start();
	}

	private void addNotification(Priority priority) {
		notificationHolder.addNotification(new DefaultNotification(
				"Title" + badgeHolder.getCount(),
				"Description ..............................................."
						+ badgeHolder.getCount(),
				priority));
	}

	private void setDrawerVariant(Behaviour variant) {
		this.variant = variant;
		reloadConfiguration();
	}

	private void openModeSelector(Behaviour variant) {
		new BehaviourSelector(variant, this::setDrawerVariant).open();
	}

	/**
	 * The Class BehaviourSelector.
	 */
	@SuppressWarnings("serial")
	class BehaviourSelector extends Dialog {

		/**
		 * Instantiates a new behaviour selector.
		 *
		 * @param current the current
		 * @param consumer the consumer
		 */
		public BehaviourSelector(Behaviour current, Consumer<Behaviour> consumer) {
			VerticalLayout layout = new VerticalLayout();
			add(layout);
			RadioButtonGroup<Behaviour> group = new RadioButtonGroup<>();
			group
				.getElement()
				.getStyle()
				.set("display", "flex");
			group
				.getElement()
				.getStyle()
				.set("flexDirection", "column");
			group.setItems(Behaviour.LEFT,
				Behaviour.LEFT_OVERLAY,
				Behaviour.LEFT_RESPONSIVE,
				Behaviour.LEFT_HYBRID,
				Behaviour.LEFT_HYBRID_SMALL,
				Behaviour.LEFT_RESPONSIVE_HYBRID,
				Behaviour.LEFT_RESPONSIVE_HYBRID_NO_APP_BAR,
				Behaviour.LEFT_RESPONSIVE_HYBRID_OVERLAY_NO_APP_BAR,
				Behaviour.LEFT_RESPONSIVE_OVERLAY,
				Behaviour.LEFT_RESPONSIVE_OVERLAY_NO_APP_BAR,
				Behaviour.LEFT_RESPONSIVE_SMALL,
				Behaviour.LEFT_RESPONSIVE_SMALL_NO_APP_BAR,
				Behaviour.TOP,
				Behaviour.TOP_LARGE);
			group.setValue(current);
			layout.add(group);
			group.addValueChangeListener(singleSelectionEvent -> {
				consumer.accept(singleSelectionEvent.getValue());
				close();
			});
		}
	}
}
