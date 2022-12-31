/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftDefinition.Stage;

public class LiftInfo {

    int liftNo;

    Integer value;

    private int changeNo;

    private Stage stage;

    private String stringValue;

    LiftInfo(Stage stage, int liftNo, int changeNo, String stringValue) {
        this.stage = stage;
        this.liftNo = liftNo;
        this.changeNo = changeNo;
        this.stringValue = stringValue;
    }

    public String getChangeName() {
        if (changeNo < 0) {
            return "";
        }
        Changes changes = LiftDefinition.Changes.values()[changeNo];
        return changes.name();
    }

    public int getChangeNo() {
        return changeNo;
    }

    public int getLiftNo() {
        return liftNo;
    }

    public Stage getStage() {
        return stage;
    }

    public String getStringValue() {
        return stringValue;
    }

    public Integer getValue() {
        if (stringValue == null) {
            return null;
        }
        return Integer.parseInt(stringValue);
    }
}