/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import app.owlcms.nui.crudui.OwlcmsCrudGrid;

public interface IAthleteEditing {

    void closeDialog();

    OwlcmsCrudGrid<?> getEditingGrid();
}