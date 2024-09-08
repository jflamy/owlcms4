package app.owlcms.nui.results;

import java.util.Set;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;

import app.owlcms.data.group.Group;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;

@SuppressWarnings("serial")
final class SessionSelectionGrid extends OwlcmsCrudGrid<Group> {
	SessionSelectionGrid(Class<Group> domainType, OwlcmsGridLayout crudLayout, OwlcmsCrudFormFactory<Group> owlcmsCrudFormFactory, Grid<Group> grid) {
		super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
	}

	@Override
	protected void updateButtons() {
	}

	public Set<Group> getSelectedItems() {
		return grid.getSelectedItems();
	}
	
	@Override
	protected void initLayoutGrid() {
		initToolbar();

		this.grid.setSizeFull();
		this.grid.setSelectionMode(SelectionMode.MULTI);

		// We do not use a selection listener; instead we handle clicks explicitely.
		// grid.addSelectionListener(e -> gridSelectionChanged());
		this.grid.addItemClickListener((e) -> {
		});
		this.grid.addItemDoubleClickListener((e) -> {
		});

		this.crudLayout.setMainComponent(this.grid);
	}


	@Override
	protected void findAllButtonClicked() {
		refreshGrid();
	}

	@Override
	protected void cancelCallback() {
		this.getOwlcmsGridLayout().hideForm();
	}
}