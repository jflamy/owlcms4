package org.ledocte.owlcms.ui.main;

import static com.github.appreciated.app.layout.entity.Section.FOOTER;
import static com.github.appreciated.app.layout.entity.Section.HEADER;
import static com.github.appreciated.app.layout.notification.entitiy.Priority.MEDIUM;

import java.util.function.Consumer;

import org.ledocte.owlcms.category.CategoryLayout;
import org.ledocte.owlcms.ui.attemptboard.AttemptBoard;

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
import com.vaadin.flow.component.dialog.Dialog;
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
            variant = Behaviour.LEFT_HYBRID_SMALL;
            notificationHolder = new DefaultNotificationHolder(newStatus -> {/*Do something with it*/});
            badgeHolder = new DefaultBadgeHolder();
        }
        reloadNotifications();

        if (!variant.isTop()) {
            LeftNavigationComponent home = new LeftNavigationComponent("Home", VaadinIcon.HOME.create(), View1.class);
            LeftNavigationComponent menu = new LeftNavigationComponent("Menu", VaadinIcon.MENU.create(), View9.class);

            notificationHolder.bind(home.getBadge());
            badgeHolder.bind(menu.getBadge());
            return AppLayoutBuilder
                    .get(variant)
                    .withTitle("App Layout")
                    .withIcon("/frontend/images/logo.png")
                    .withAppBar(AppBarBuilder
                            .get()
                            .add(new AppBarNotificationButton(VaadinIcon.BELL, notificationHolder))
                            .build())
                    .withAppMenu(LeftAppMenuBuilder
                            .get()
                            .addToSection(new MenuHeaderComponent("App-Layout",
                                    "Version 2.0.4",
                                    "/frontend/images/logo.png"
                            ), HEADER)
                            .addToSection(new LeftClickableComponent("Set Behaviour HEADER",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), HEADER)
                            .add(home)
                            .add(new LeftNavigationComponent("Attempt", VaadinIcon.CHART_TIMELINE.create(), AttemptBoard.class))
                            .add(new LeftNavigationComponent("Grid", VaadinIcon.TABLE.create(), CategoryLayout.class))
//                            .add(LeftSubMenuBuilder
//                                    .get("My Submenu", VaadinIcon.PLUS.create())
//                                    .add(LeftSubMenuBuilder
//                                            .get("My Submenu", VaadinIcon.PLUS.create())
//                                            .add(new LeftNavigationComponent("Charts",
//                                                    VaadinIcon.SPLINE_CHART.create(),
//                                                    View2.class
//                                            ))
//                                            .add(new LeftNavigationComponent("Contact",
//                                                    VaadinIcon.CONNECT.create(),
//                                                    View3.class
//                                            ))
//                                            .add(new LeftNavigationComponent("More",
//                                                    VaadinIcon.COG.create(),
//                                                    View4.class
//                                            ))
//                                            .build())
//                                    .add(new LeftNavigationComponent("Contact1",
//                                            VaadinIcon.CONNECT.create(),
//                                            View5.class
//                                    ))
//                                    .add(new LeftNavigationComponent("More1", VaadinIcon.COG.create(), View6.class))
//                                    .build())
//                            .add(LeftSubMenuBuilder
//                                    .get("My Submenu", VaadinIcon.PLUS.create())
//                                    .add(new LeftNavigationComponent("Contact2",
//                                            VaadinIcon.CONNECT.create(),
//                                            View7.class
//                                    ))
//                                    .add(new LeftNavigationComponent("More2", VaadinIcon.COG.create(), View8.class))
//                                    .build())
                            .add(menu)
                            .addToSection(new LeftClickableComponent("Set Behaviour FOOTER",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), FOOTER)
                            .build())
                    .build();
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
                            .addToSection(new TopClickableComponent("Set Behaviour 1",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), HEADER)
                            .add(new TopNavigationComponent("Home", VaadinIcon.HOME.create(), View1.class))
                            .add(new TopNavigationComponent("Contact", VaadinIcon.SPLINE_CHART.create(), View2.class))
                            .addToSection(new TopClickableComponent("Set Behaviour 2",
                                    VaadinIcon.COG.create(),
                                    clickEvent -> openModeSelector(variant)
                            ), FOOTER)
                            .addToSection(
                                    new TopNavigationComponent("More", VaadinIcon.CONNECT.create(), View3.class),
                                    FOOTER
                            )
                            .build())
                    .build();
        }
    }

  /*@Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    getUI().get().getPage().executeJavaScript("document.documentElement.setAttribute(\"theme\",\"dark\")");
  }*/

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
                    //Thread.sleep(5000);
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
                priority
        ));
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
                    Behaviour.TOP_LARGE
            );
            group.setValue(current);
            layout.add(group);
            group.addValueChangeListener(singleSelectionEvent -> {
                consumer.accept(singleSelectionEvent.getValue());
                close();
            });
        }
    }
}
