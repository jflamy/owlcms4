package org.ledocte.owlcms.ui.home;

import static com.github.appreciated.app.layout.entity.Section.FOOTER;
import static com.github.appreciated.app.layout.entity.Section.HEADER;
import static com.github.appreciated.app.layout.notification.entitiy.Priority.MEDIUM;

import java.util.function.Consumer;

import org.ledocte.owlcms.ui.displaySetup.DisplayContent;
import org.ledocte.owlcms.ui.lifting.LiftingContent;
import org.ledocte.owlcms.ui.preparation.PreparationContent;
import org.ledocte.owlcms.ui.wrapup.WrapupContent;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.github.appreciated.app.layout.builder.AppLayoutBuilder;
import com.github.appreciated.app.layout.component.appbar.AppBarBuilder;
import com.github.appreciated.app.layout.component.appmenu.MenuHeaderComponent;
import com.github.appreciated.app.layout.component.appmenu.left.LeftClickableComponent;
import com.github.appreciated.app.layout.component.appmenu.left.LeftNavigationComponent;
import com.github.appreciated.app.layout.component.appmenu.left.builder.LeftAppMenuBuilder;
import com.github.appreciated.app.layout.component.appmenu.top.TopClickableComponent;
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
 * The main view contains a button and a template element.
 */

@Push
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@StyleSheet("frontend://styles/owlcms.css")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/av-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/hardware-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/maps-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/social-icons.html")
@HtmlImport("frontend://bower_components/iron-icons/places-icons.html")
public class MainLayout extends AppLayoutRouterLayout {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	DefaultNotificationHolder notificationHolder;
	DefaultBadgeHolder badgeHolder;
	private Behaviour variant;
	private Thread currentThread;

	@Override
    public AppLayout createAppLayoutInstance() {
        if (variant == null) {
            variant = Behaviour.LEFT_OVERLAY;
            notificationHolder = new DefaultNotificationHolder(newStatus -> {/*Do something with it*/});
            badgeHolder = new DefaultBadgeHolder();
        }
        reloadNotifications();

        if (!variant.isTop()) {
            LeftNavigationComponent home = new LeftNavigationComponent("Home", VaadinIcon.HOME.create(), MainContent.class);

            notificationHolder.bind(home.getBadge());

			AppLayout appLayout = AppLayoutBuilder
                    .get(variant)
                    .withTitle("App Layout")
                    .withIcon("/frontend/images/logo.png")
                    .withAppBar(AppBarBuilder
                            .get()
                            .add(new AppBarNotificationButton(VaadinIcon.BELL, notificationHolder))
                            .build())
                    .withAppMenu(LeftAppMenuBuilder
                            .get()
                            .addToSection(new MenuHeaderComponent("OWLCMS", null, null), HEADER)
                            .add(home)
                            .add(new LeftNavigationComponent("Prepare Competition", new Icon("social","group-add"), PreparationContent.class))
                            .add(new LeftNavigationComponent("Setup Displays", new Icon("hardware", "desktop-windows"), DisplayContent.class))
                            .add(new LeftNavigationComponent("Lifting Group",  new FullIronIcon("places", "fitness-center"), LiftingContent.class))
                            .add(new LeftNavigationComponent("Competition Documents", new Icon("maps", "local-printshop"), WrapupContent.class))
                            .addToSection(new LeftClickableComponent("Preferences",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), FOOTER)
                            .build())
                    .build();
			return appLayout;
        } else {
            return AppLayoutBuilder
                    .get(variant)
                    .withTitle("App Layout")
                    .withAppBar(AppBarBuilder
                            .get()
                            .add(new AppBarNotificationButton(VaadinIcon.BELL, notificationHolder))
                            .build())
                    .withAppMenu(TopAppMenuBuilder
                            .get()
                            .add(new TopNavigationComponent("Prepare Competition", new Icon("social","group-add"), PreparationContent.class))
                            .add(new TopNavigationComponent("Setup Displays", new Icon("hardware", "desktop-windows"), DisplayContent.class))
                            .add(new TopNavigationComponent("Lifting Group",  new FullIronIcon("places", "fitness-center"), LiftingContent.class))
                            .add(new TopNavigationComponent("Competition Documents", new Icon("maps", "local-printshop"), WrapupContent.class))
                            .addToSection(new LeftClickableComponent("Preferences",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), FOOTER)
                            .addToSection(new TopClickableComponent("Preferences",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), FOOTER)
                            .build())
                    .build();
        }
    }

	/*
	 * @Override protected void onAttach(AttachEvent attachEvent) {
	 * super.onAttach(attachEvent); getUI().get().getPage().executeJavaScript(
	 * "document.documentElement.setAttribute(\"theme\",\"dark\")"); }
	 */

	private void reloadNotifications() {
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

	class BehaviourSelector extends Dialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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
