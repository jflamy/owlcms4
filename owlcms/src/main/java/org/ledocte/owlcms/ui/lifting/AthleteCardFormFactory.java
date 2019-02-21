package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Int;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.GridLayout;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class AthleteCardFormFactory extends OwlcmsCrudFormFactory<Athlete> {

	private static final int HEADER = 1;
	private static final int DECLARATION = HEADER + 1;
	private static final int CHANGE1 = DECLARATION + 1;
	private static final int CHANGE2 = CHANGE1 + 1;
	private static final int ACTUAL = CHANGE2 + 1;

	private static final int LEFT = 1;
	private static final int SNATCH1 = LEFT + 1;
	private static final int SNATCH2 = SNATCH1 + 1;
	private static final int SNATCH3 = SNATCH2 + 1;
	private static final int CJ1 = SNATCH3 + 1;
	private static final int CJ2 = CJ1 + 1;
	private static final int CJ3 = CJ2 + 1;

	public AthleteCardFormFactory(Class<Athlete> domainType) {
		super(domainType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object, boolean,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete a, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		FormLayout formLayout = new FormLayout();
		formLayout.setSizeFull();
		if (this.responsiveSteps != null) {
			formLayout.setResponsiveSteps(this.responsiveSteps);
		}

		GridLayout gridLayout = new GridLayout();
		gridLayout.setTemplateRows(new Repeat(ACTUAL, new Flex(1)));
		gridLayout.setTemplateColumns(new Repeat(CJ3, new Flex(1)));
		
		// column headers
		withRowAndColumn(gridLayout, new Label("snatch 1"), HEADER, SNATCH1);
		withRowAndColumn(gridLayout, new Label("snatch 2"), HEADER, SNATCH2);
		withRowAndColumn(gridLayout, new Label("snatch 3"), HEADER, SNATCH3);
		withRowAndColumn(gridLayout, new Label("C&J 1"), HEADER, CJ1);
		withRowAndColumn(gridLayout, new Label("C&J 2"), HEADER, CJ2);
		withRowAndColumn(gridLayout, new Label("C&J 3"), HEADER, CJ3);
		
		// row headings
		withRowAndColumn(gridLayout, new Label("Declaration"), DECLARATION, LEFT);
		withRowAndColumn(gridLayout, new Label("Change 1"), CHANGE1, LEFT);
		withRowAndColumn(gridLayout, new Label("Change 2"), CHANGE2, LEFT);
		withRowAndColumn(gridLayout, new Label("Weight Lifted"), ACTUAL, LEFT);
		
		binder = buildBinder(operation, a);

		TextField snatch1Declaration = new TextField();
		binder.bind(snatch1Declaration, Athlete::getSnatch1Declaration, Athlete::setSnatch1Declaration);
		withRowAndColumn(gridLayout, snatch1Declaration, DECLARATION, SNATCH1);

		TextField cj1Declaration = new TextField();
		binder.bind(cj1Declaration, Athlete::getCleanJerk1Declaration, Athlete::setCleanJerk1Declaration);
		withRowAndColumn(gridLayout, cj1Declaration, DECLARATION, CJ1);

		binder.readBean(a);
		
		Component footerLayout = this
			.buildFooter(operation, a, cancelButtonClickListener, updateButtonClickListener, deleteButtonClickListener);

		com.vaadin.flow.component.orderedlayout.VerticalLayout mainLayout = new VerticalLayout(formLayout, gridLayout,
				footerLayout);
		gridLayout.setSizeFull();
		mainLayout.setFlexGrow(1, gridLayout);
		mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
		mainLayout.setMargin(false);
		mainLayout.setPadding(false);
		return mainLayout;
	}

	private void withRowAndColumn(GridLayout gridLayout, Component component, int i, int j) {
		gridLayout.add(component);
		gridLayout.setRowAndColumn(component, new Int(i), new Int(j), new Int(i), new Int(j));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.
	 * crudui.crud.CrudOperation, java.lang.Object,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener,
	 * com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	protected Component buildFooter(CrudOperation operation, Athlete domainObject,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
		return super.buildFooter(operation,
			domainObject,
			cancelButtonClickListener,
			updateButtonClickListener,
			null);
	}

}
