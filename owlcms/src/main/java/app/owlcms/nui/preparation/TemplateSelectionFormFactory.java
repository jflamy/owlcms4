/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.function.ValueProvider;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TemplateSelectionFormFactory extends VerticalLayout {

	private Logger logger = (Logger) LoggerFactory.getLogger(ConfigRepository.class);
	@SuppressWarnings("unused")
	private Object origin;

	TemplateSelectionFormFactory(Class<Config> domainType, Object origin) {
		this.origin = origin;
		this.add(templateSelectionForm());
	}


	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		// layout.setWidth("1024px");
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
		        new ResponsiveStep("800px", 2, LabelsPosition.TOP));
		return layout;
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0");
		return title;
	}

	private FormLayout templateSelectionForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("TemplateSelection");
		layout.add(title);
		layout.setColspan(title, 2);

		addTemplateSelection(layout, "Card", "/templates/cards", (f) -> f.endsWith(".xls"), Competition::getCardsTemplateFileName, Competition::setCardsTemplateFileName);
		return layout;
	}

	private void addTemplateSelection(FormLayout layout,
	        String labelKey,
	        String resourceDirectoryLocation,
	        Predicate<String> nameFilter,
	        ValueProvider<Competition, String> templateNameGetter,
	        Setter<Competition, String> templateNameSetter) {
		ComboBox<Resource> templateSelect = new ComboBox<>();
		templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
		templateSelect.setHelperText(Translator.translate("SelectTemplate"));
		List<Resource> resourceList = new ResourceWalker().getResourceList(
		        resourceDirectoryLocation,
		        ResourceWalker::relativeName,
		        nameFilter,
		        OwlcmsSession.getLocale(),
		        Config.getCurrent().isLocalTemplatesOnly());
		List<Resource> prioritizedList = xlsxPriority(resourceList);
		templateSelect.setItems(prioritizedList);
		templateSelect.setValue(null);
		templateSelect.setWidth("15em");
		// templateSelect.getStyle().set("margin-left", "1em");
		templateSelect.getStyle().set("margin-right", "0.8em");

		templateSelect.setWidthFull();
		layout.addFormItem(templateSelect, Translator.translate(labelKey));

		templateSelect.addValueChangeListener(e -> {
			try {
				String newTemplateName = e.getValue().getFileName();

				Competition current = Competition.getCurrent();
				Resource res = searchMatch(prioritizedList, newTemplateName);
				if (res == null) {
					this.logger.debug("(2) template NOT found {} {}", newTemplateName, prioritizedList);
					throw new FileNotFoundException("template not found " + newTemplateName);
				}
				this.logger.debug("(2) template found {}", res != null ? res.getFilePath() : null);
				templateNameSetter.accept(current, newTemplateName);

				CompetitionRepository.save(current);
				current = Competition.getCurrent();
			} catch (Throwable e1) {
				this.logger.error("{}", LoggerUtils.stackTrace(e1));
			}
		});
	}

	private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
		Resource found = null;
		for (Resource curResource : resourceList) {
			String fileName = curResource.getFileName();
			this.logger.trace("comparing {} {}", fileName, curTemplateName);
			if (fileName.equals(curTemplateName)) {
				found = curResource;
				break;
			}
		}
		return found;
	}

	/**
	 * give precedence to .xlsx file if both .xls and .xlsx
	 *
	 * @param resourceList
	 * @return
	 */
	private List<Resource> xlsxPriority(List<Resource> resourceList) {
		// xlsx will come before xls
		resourceList.sort(Comparator.comparing(Resource::getFileName).reversed());

		ArrayList<Resource> proritizedList = new ArrayList<>();
		String prevName = "";
		for (Resource r : resourceList) {
			String curName = r.getFileName();
			// give precedence to .xlsx file if both .xls and .xlsx
			if (curName.endsWith(".xlsx") || (curName.endsWith(".xls") && !prevName.contentEquals(curName + "x"))) {
				proritizedList.add(r);
			}
			prevName = curName;
		}
		proritizedList.sort(Comparator.comparing(Resource::getFileName));
		return proritizedList;
	}

}