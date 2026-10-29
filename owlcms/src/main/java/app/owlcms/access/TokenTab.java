package app.owlcms.access;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

/**
 * The token identity of one browser tab. It never holds the token itself, and it is kept on the tab's UI, not in the
 * HTTP session: a token tab and a password tab in the same browser keep their own identities. A reload creates a new
 * UI, and the token in the URL authenticates again.
 */
public record TokenTab(Long accountId, String tokenId, Principal principal) {

	/** @return the token identity of this tab, or null if it was not opened with a token */
	public static TokenTab of(UI ui) {
		return ui == null ? null : ComponentUtil.getData(ui, TokenTab.class);
	}

	public static void bind(UI ui, TokenTab tab) {
		ComponentUtil.setData(ui, TokenTab.class, tab);
	}
}
