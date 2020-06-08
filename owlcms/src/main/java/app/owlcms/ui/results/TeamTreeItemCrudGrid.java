/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.results;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.grid.Grid;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * This class attempts to locate the selected athlete from the in the lifting order.
 *
 * <p>
 * This is a workaround for two issues:
 * <li>Why does getValue() return a different object than that in the lifting order (initialization issue?)
 * <li>Why do we have to get the same object anyway (spurious comparison with == instead of getId() or .equals)
 * </p>
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
public class TeamTreeItemCrudGrid extends OwlcmsCrudGrid<TeamTreeItem> {

    final private Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeItemCrudGrid.class);
    {
        logger.setLevel(Level.INFO);
    }

    Athlete match = null;

    public TeamTreeItemCrudGrid(Class<TeamTreeItem> domainType, OwlcmsGridLayout crudLayout,
            OwlcmsCrudFormFactory<TeamTreeItem> owlcmsCrudFormFactory, Grid<TeamTreeItem> grid) {
        super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
    }

//    /*
//     * (non-Javadoc)
//     *
//     * @see org.vaadin.crudui.crud.impl.GridCrud#updateButtonClicked()
//     */
//    @Override
//    protected void updateButtonClicked() {
//        Athlete domainObject = grid.asSingleSelect().getValue();
//        Athlete sought = domainObject;
//        // if available we want the exact object from the lifting order and not a copy
//        OwlcmsSession.withFop((fop) -> {
//            Long id = sought.getId();
//            found: for (Athlete a : fop.getLiftingOrder()) {
//                logger.debug("checking for {} : {} {}", id, a, a.getId());
//                if (a.getId().equals(id)) {
//                    match = a;
//                    break found;
//                }
//            }
//        });
//        logger.trace("domainObject = {} {}", (domainObject != match ? "!!!!" : ""), domainObject, match);
//        if (match != null) {
//            domainObject = match;
//        }
//
//        // show both an update and a delete button.
//        this.showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, null);
//    }

//    protected void showForm(CrudOperation operation, TeamTreeItem domainObject, boolean readOnly, String successMessage,
//            ComponentEventListener<ClickEvent<Button>> unused) {
//        Component form = this.owlcmsCrudFormFactory.buildNewForm(operation, domainObject.getAthlete(), readOnly,
//                cancelButtonClickEvent -> {
//                    logger.debug("cancelButtonClickEvent");
//                    owlcmsGridLayout.hideForm();
//                    grid.asSingleSelect().clear();
//                },
//                operationButtonClickEvent -> {
//                    try {
//                        logger.debug("postOperation");
//                        grid.asSingleSelect().clear();
//                        owlcmsGridLayout.hideForm();
//                        refreshGrid();
//                        Notification.show(successMessage);
//                        logger.trace("operation performed");
//                    } catch (Exception e) {
//                        logger.error(LoggerUtils.stackTrace(e));
//                    }
//                },
//                deleteButtonClickEvent -> {
//                    logger.debug("preDelete");
//                    owlcmsGridLayout.hideForm();
//                    this.deleteButtonClicked();
//                });
//
//        String caption = this.owlcmsCrudFormFactory.buildCaption(operation, domainObject);
//        owlcmsGridLayout.showForm(operation, form, caption);
//    }
}
