package app.owlcms.nui.shared;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.access.IwfComplianceCheck;
import app.owlcms.access.IwfComplianceCheck.Item;
import app.owlcms.access.IwfComplianceCheck.Status;
import app.owlcms.i18n.Translator;

/** Shows the IWF compliance check: the full list on the Administration page, the problems elsewhere. */
public final class IwfComplianceDisplay {

	private static final String SUCCESS_COLOR = "var(--lumo-success-color)";
	private static final String ERROR_COLOR = "var(--lumo-error-color)";
	private static final String WARNING_COLOR = "var(--lumo-warning-color)";

	private IwfComplianceDisplay() {
	}

	/** Every requirement, met or not. */
	public static Component checklist() {
		return markedList(IwfComplianceCheck.evaluate());
	}

	/** @return the requirements not met, when the check is on and some are */
	public static Optional<Component> warning() {
		List<Item> problems = IwfComplianceCheck.problems();
		if (problems.isEmpty()) {
			return Optional.empty();
		}
		Paragraph heading = new Paragraph(icon(VaadinIcon.WARNING, WARNING_COLOR),
		        new Span(Translator.translate("IwfCheck.NotMet")));
		heading.getStyle().set("font-weight", "bold");
		return Optional.of(new Div(heading, markedList(problems)));
	}

	/** For busy pages such as the Competition Supervisor page: a notification that stays until closed. */
	public static void notifyIfNeeded() {
		List<Item> problems = IwfComplianceCheck.problems();
		if (problems.isEmpty()) {
			return;
		}
		Notification notification = new Notification();
		notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
		Button close = new Button(Translator.translate("GotIt"), event -> notification.close());
		close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		// the notification is red: the icons keep its text color
		Paragraph heading = new Paragraph(icon(VaadinIcon.WARNING, null),
		        new Span(Translator.translate("IwfCheck.NotMet")));
		UnorderedList list = new UnorderedList();
		for (Item item : problems) {
			list.add(new ListItem(text(item)));
		}
		VerticalLayout content = new VerticalLayout(heading, list, close);
		content.setPadding(false);
		notification.add(content);
		notification.setPosition(Position.TOP_CENTER);
		notification.open();
	}

	private static UnorderedList markedList(List<Item> items) {
		UnorderedList list = new UnorderedList();
		list.getStyle().set("list-style", "none").set("padding-left", "0");
		for (Item item : items) {
			ListItem line = new ListItem(mark(item.status()), new Span(text(item)));
			line.getStyle().set("display", "flex").set("align-items", "flex-start");
			list.add(line);
		}
		return list;
	}

	private static Icon mark(Status status) {
		return switch (status) {
			case MET -> icon(VaadinIcon.CHECK_CIRCLE, SUCCESS_COLOR);
			case NOT_MET -> icon(VaadinIcon.CLOSE_CIRCLE, ERROR_COLOR);
			// a new key is ready but only used after the next restart
			case AFTER_RESTART -> icon(VaadinIcon.WARNING, WARNING_COLOR);
		};
	}

	private static Icon icon(VaadinIcon vaadinIcon, String color) {
		Icon icon = vaadinIcon.create();
		if (color != null) {
			icon.setColor(color);
		}
		icon.setSize("1.1em");
		icon.getStyle().set("flex", "none").set("margin", "0.15em 0.4em 0 0");
		return icon;
	}

	private static String text(Item item) {
		String text = Translator.translate(item.requirement().getTranslationKey());
		return item.status() == Status.AFTER_RESTART ? text + " " + Translator.translate("IwfCheck.AfterRestart")
		        : text;
	}
}
