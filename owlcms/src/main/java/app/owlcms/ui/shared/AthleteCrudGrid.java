package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;

import com.vaadin.flow.component.grid.Grid;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class attempts to locate the selected athlete from the in the lifting order.
 * 
 * <p>
 * This is a workaround for two issues:
 * <li> Why does getValue() return a different object than that in the lifting order (initialization issue?)
 * <li> Why do we have to get the same object anyway (spurious comparison with == instead of getId() or .equals)
 * </p>
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class AthleteCrudGrid extends OwlcmsCrudGrid<Athlete> {

	final private Logger logger = (Logger) LoggerFactory.getLogger(AthleteCrudGrid.class);
	{
		logger.setLevel(Level.INFO);
	}

	public AthleteCrudGrid(Class<Athlete> domainType, OwlcmsGridLayout crudLayout,
			OwlcmsCrudFormFactory<Athlete> owlcmsCrudFormFactory, Grid<Athlete> grid) {
		super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
	}

	Athlete match = null;

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.impl.GridCrud#updateButtonClicked() */
	@Override
	protected void updateButtonClicked() {
		// FIXME: getValue() the first time around does not return the same object as present in lifting
		// order.
		Athlete domainObject = grid.asSingleSelect().getValue();
		Athlete sought = domainObject;
		// if available we want the exact object from the lifting order and not a copy
		OwlcmsSession.withFop((fop) -> {
			Long id = sought.getId();
			found: for (Athlete a : fop.getLiftingOrder()) {
				logger.debug("checking for {} : {} {}", id, a, a.getId());
				if (a.getId().equals(id)) {
					match = a;
					break found;
				}
			}
			;
		});
		logger.debug("domainObject = {} {}", (domainObject != match ? "!!!!" : ""), domainObject, match);
		if (match != null)
			domainObject = match;

		// show both an update and a delete button.
		this.showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
			try {
				logger.trace("before updateOperation, {}, after binder validation {}", match, LoggerUtils.whereFrom());
				Athlete updatedObject = updateOperation.perform(match);
				grid.asSingleSelect().clear();
				refreshGrid();
				grid.asSingleSelect().setValue(updatedObject);
			} catch (IllegalArgumentException ignore) {
				logger.error(LoggerUtils.stackTrace(ignore));
			} catch (CrudOperationException e1) {
				logger.error(LoggerUtils.stackTrace(e1));
				refreshGrid();
			} catch (Exception e2) {
				logger.error(LoggerUtils.stackTrace(e2));
				refreshGrid();
				throw e2;
			}
		});
	}
}
