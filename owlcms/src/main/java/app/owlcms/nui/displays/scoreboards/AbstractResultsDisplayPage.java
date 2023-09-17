package app.owlcms.nui.displays.scoreboards;

import java.text.DecimalFormat;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.SoundEntries;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Logger;

/**
 * Wrapper class to wrap a board as navigable page, to store the board display options, and to present an option editing
 * dialog.
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")

public abstract class AbstractResultsDisplayPage extends AbstractDisplayPage
        implements SoundEntries, DisplayParametersReader, HasDynamicTitle, SafeEventBusRegistration {

	Logger logger = (Logger) LoggerFactory.getLogger(AbstractResultsDisplayPage.class);
	private final DecimalFormat df = new DecimalFormat("0.000");
	protected DisplayParameters board;
	private static final int DEBOUNCE = 50;
	private long now;
	private long lastShortcut;

	public AbstractResultsDisplayPage() {
		// intentionally empty; superclass will invoke init() as required.
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSoundEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSwitchableEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSectionEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSizingEntries(vl, target, this);

		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setEmFontSize(getEmFontSize() + 0.005);
			}
			lastShortcut = now;
		}, Key.ARROW_UP);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setEmFontSize(getEmFontSize() - 0.005);
			}
			lastShortcut = now;
		}, Key.ARROW_DOWN);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setTeamWidth(getTeamWidth() + 0.5);
			}
			lastShortcut = now;
		}, Key.ARROW_RIGHT);
		UI.getCurrent().addShortcutListener(() -> {
			now = System.currentTimeMillis();
			if (now - lastShortcut > DEBOUNCE) {
				setTeamWidth(getTeamWidth() - 0.5);
			}
			lastShortcut = now;
		}, Key.ARROW_LEFT);
	}

	@Override
	public Double getEmFontSize() {
		if (super.getEmFontSize() == null) {
			return 1.2;
		}
		return super.getEmFontSize();
	}

	@Override
	public Double getTeamWidth() {
		if (super.getTeamWidth() == null) {
			return 12.0D;
		}
		return super.getTeamWidth();
	}

	@Override
	public void pushEmSize() {
		String formattedEm = null;
		if (getEmFontSize() != null) {
			formattedEm = df.format(getEmFontSize());
			getBoard().getElement().setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "rem;");
		}
	}

	@Override
	public void pushTeamWidth() {
		String formattedTW = null;
		if (getTeamWidth() != null) {
			formattedTW = df.format(getTeamWidth());
			getBoard().getElement().setProperty("twOverride", "--nameWidth: 1fr; --clubWidth:" + formattedTW + "em;");
		}
	}

	@Override
	protected abstract void init();

}
