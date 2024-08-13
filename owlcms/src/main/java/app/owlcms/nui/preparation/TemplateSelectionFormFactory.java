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

	TemplateSelectionFormFactory() {
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

	public FormLayout preWeighInTemplateSelectionForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("TemplateSelection");
		layout.add(title);
		layout.setColspan(title, 2);

		addTemplateSelection(layout, Templates.CARDS);
		addTemplateSelection(layout, Templates.WEIGHIN);
		return layout;
	}

	public FormLayout postWeighInTemplateSelectionForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("TemplateSelection");
		layout.add(title);
		layout.setColspan(title, 2);

		addTemplateSelection(layout, Templates.INTRODUCTION);
		addTemplateSelection(layout, Templates.EMPTY_PROTOCOL);
		addTemplateSelection(layout, Templates.JURY);
		return layout;
	}

	public FormLayout competitionTemplateSelectionForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("TemplateSelection");
		layout.add(title);
		layout.setColspan(title, 2);

		addTemplateSelection(layout, Templates.START_LIST);
		addTemplateSelection(layout, Templates.SCHEDULE);
		addTemplateSelection(layout, Templates.OFFICIALS);
		addTemplateSelection(layout, Templates.CHECKIN);

		return layout;
	}

	public FormLayout templateSelectionForm(String titleString, Templates type) {
		FormLayout layout = createLayout();
		Component title = createTitle(titleString);
		layout.add(title);
		layout.setColspan(title, 2);

		switch (type) {

			case CHECKIN:
			case OFFICIALS:
			case START_LIST:
			case SCHEDULE:
				return competitionTemplateSelectionForm();

			case CARDS:
			case WEIGHIN:
				return preWeighInTemplateSelectionForm();

			case EMPTY_PROTOCOL:
			case INTRODUCTION:
			case JURY:
				return postWeighInTemplateSelectionForm();
		}

		return layout;
	}

	private void addTemplateSelection(FormLayout layout, Templates template) {
		List<Resource> prioritizedList = computeResourceList(template.folder, (f) -> f.endsWith(template.extension));
		ComboBox<Resource> templateSelect = createTemplateSelect(layout, template.name(), prioritizedList, template.templateFileNameSupplier.get());

		templateSelect.addValueChangeListener(e -> {
			try {
				Resource value = e.getValue();
				String newTemplateName = value != null ? value.getFileName() : null;
				if (newTemplateName != null) {
					Resource res = searchMatch(prioritizedList, newTemplateName);
					if (res == null) {
						throw new FileNotFoundException("template not found " + newTemplateName);
					}
				}

				// lambda uses getCurrent().
				template.templateFileNameSetter.accept(newTemplateName);
				Competition current = Competition.getCurrent();
				CompetitionRepository.save(current);
				current = Competition.getCurrent();
			} catch (Throwable e1) {
				LoggerUtils.logError(logger, e1);
			}
		});
	}

	private List<Resource> computeResourceList(String resourceDirectoryLocation, Predicate<String> nameFilter) {
		List<Resource> resourceList = new ResourceWalker().getResourceList(
		        resourceDirectoryLocation,
		        ResourceWalker::relativeName,
		        nameFilter,
		        OwlcmsSession.getLocale(),
		        Config.getCurrent().isLocalTemplatesOnly());
		List<Resource> prioritizedList = xlsxPriority(resourceList);
		return prioritizedList;
	}

	private ComboBox<Resource> createTemplateSelect(FormLayout layout, String labelKey, List<Resource> prioritizedList, String string) {
		ComboBox<Resource> templateSelect = new ComboBox<>();
		templateSelect.setPlaceholder(Translator.translate("AvailableTemplates"));
		templateSelect.setHelperText(Translator.translate("SelectTemplate"));
		templateSelect.setItems(prioritizedList);
		templateSelect.setValue(null);
		templateSelect.setWidth("15em");
		templateSelect.getStyle().set("margin-right", "0.8em");
		templateSelect.setClearButtonVisible(true);
		templateSelect.setWidthFull();
		layout.addFormItem(templateSelect, Translator.translate(labelKey));
		templateSelect.setValue(searchMatch(prioritizedList, string));
		return templateSelect;
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