/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;

/**
 * Editor for the championship template: the default scoring, medaling and team
 * rules used by championships that do not override them.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/championshipTemplate", layout = OwlcmsLayout.class)
public class ChampionshipTemplateContent extends BaseContent implements OwlcmsContent {
	private OwlcmsLayout routerLayout;

	public ChampionshipTemplateContent() {
		Paragraph intro = new Paragraph(Translator.translate("Competition.DefaultScoringMedalingRulesIntro"));
		intro.getStyle().set("margin-top", "0");

		ChampionshipDetailsForm form = new ChampionshipDetailsForm(ChampionshipRepository.ensureCompetitionTemplate());

		Button updateButton = new Button(Translator.translate("Update"), VaadinIcon.CHECK.create(), e -> {
			if (form.save()) {
				Notification notification = Notification.show(Translator.translate("ChampionshipTemplate.Saved"));
				notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout footer = new HorizontalLayout(updateButton);
		footer.setWidthFull();
		footer.setJustifyContentMode(JustifyContentMode.END);

		VerticalLayout layout = new VerticalLayout(intro, new Hr(), form, footer);
		layout.setPadding(false);
		layout.setWidthFull();
		fillH(layout, this);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("ChampionshipTemplate.Title");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
