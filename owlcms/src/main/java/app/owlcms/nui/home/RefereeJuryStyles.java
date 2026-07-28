/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.utils.StartupUtils;

/**
 * Loads the referee/jury launcher stylesheet with a cache-busting suffix.
 *
 * The @StyleSheet annotation is not used because Vaadin only appends its content hash in production
 * mode. During development the stylesheet would keep being served from the browser cache, which
 * mobile browsers hold on to aggressively.
 */
class RefereeJuryStyles {

	private static final String STYLESHEET = "styles/referee-jury-home.css";
	private static final String LOADED_KEY = "refereeJuryStylesLoaded";

	/**
	 * Adds the stylesheet once per UI. Repeated navigation between the referee and jury launchers
	 * reuses the same page, so the link element must not be added again.
	 *
	 * @param ui the current UI
	 */
	static void ensureLoaded(UI ui) {
		if (ui == null || Boolean.TRUE.equals(ComponentUtil.getData(ui, LOADED_KEY))) {
			return;
		}
		ComponentUtil.setData(ui, LOADED_KEY, Boolean.TRUE);
		ui.getPage().addStyleSheet(STYLESHEET + "?v=" + version(ui));
	}

	/**
	 * Forces the light theme. These launchers are used on personal phones, which may be set to dark
	 * mode, and the buttons must stay legible under competition lighting.
	 *
	 * @param ui the current UI
	 */
	static void applyLightTheme(UI ui) {
		if (ui == null) {
			return;
		}
		ThemeList themeList = ui.getElement().getThemeList();
		themeList.remove(Lumo.DARK);
		themeList.add(Lumo.LIGHT);
	}

	/**
	 * @return a token that changes on every page load during development, and on every release in
	 *         production.
	 */
	private static String version(UI ui) {
		return isProductionMode(ui) ? releaseToken() : Long.toString(System.currentTimeMillis());
	}

	private static boolean isProductionMode(UI ui) {
		VaadinSession session = ui.getSession();
		DeploymentConfiguration configuration = session != null ? session.getConfiguration() : null;
		// when in doubt, behave as production and keep the stylesheet cacheable
		return configuration == null || configuration.isProductionMode();
	}

	private static String releaseToken() {
		String release = StartupUtils.getVersion();
		if (release == null || release.isBlank()) {
			release = StartupUtils.getBuildTimestamp();
		}
		return release == null ? "0" : release.replaceAll("[^A-Za-z0-9._-]", "");
	}

	private RefereeJuryStyles() {
	}
}
