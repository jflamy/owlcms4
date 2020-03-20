/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import app.owlcms.ui.crudui.OwlcmsCrudGrid;

public interface IAthleteEditing {

    void closeDialog();

    OwlcmsCrudGrid<?> getEditingGrid();
}