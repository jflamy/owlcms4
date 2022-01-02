/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.lang.reflect.Method;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 *
 * Describe each lift so introspection can be used on the Athlete class.
 *
 * Used to avoid repetitive error-prone boiler-plate code.
 *
 * LiftDefinition ls1 = liftDefinitions[0] contains a {@link #LiftDefinition(int, Stage)} with a list of getters and a
 * list of setters, one for each Change (AUTOMATIC, DECLARATION, etc.).
 *
 * There is a LiftDefinition for each lift, in the array {@link #lifts}
 *
 * ls1.stage returns Stage.SNATCH ls1.getters[1] is a reference to Athlete.getSnatch1Declaration, so that
 * Method.invoke(ls1.getters[1]) returns the value.
 *
 * It is also possible to iterate over the athlete's card by using the changeGetters[lifts][changes] methods (and the
 * corresponding changeSetters).
 *
 * @author Jean-François Lamy
 *
 */
public class LiftDefinition {
    public enum Changes {
        AUTOMATIC("AutomaticProgression"), DECLARATION("Declaration"), CHANGE1("Change1"), CHANGE2("Change2"),
        ACTUAL("ActualLift");

        public String methodSuffix;

        Changes(String methodSuffix) {
            this.methodSuffix = methodSuffix;
        }
    }

    public enum Stage {
        SNATCH(0, 2, "Snatch"), CLEANJERK(3, 5, "CleanJerk");

        public int inclLower;
        public int inclUpper;
        public String label;

        Stage(int inclLower, int inclUpper, String label) {
            this.inclLower = inclLower;
            this.inclUpper = inclUpper;
            this.label = label;
        }
    }

    private static Logger logger = (Logger) LoggerFactory.getLogger(LiftDefinition.class);

    public final static int NBCHANGES = Changes.values().length;
    public final static int NBLIFTS = 6;
    public static Method[][] changeGetters = new Method[NBLIFTS][NBCHANGES];
    public static Method[][] changeSetters = new Method[NBLIFTS][NBCHANGES];

    /**
     * For each lift, there is a lift definition that allows getting its methods
     */
    public static LiftDefinition[] lifts = new LiftDefinition[NBLIFTS];

    static {
        initStageMethods(Stage.SNATCH);
        initStageMethods(Stage.CLEANJERK);
        initLiftDefinitions(Stage.SNATCH);
        initLiftDefinitions(Stage.CLEANJERK);
    }

    private static void initLiftDefinitions(Stage stage) {
        for (int lift = stage.inclLower; lift <= stage.inclUpper; lift++) {
            lifts[lift] = new LiftDefinition(lift, stage);
        }
    }

    private static void initStageMethods(Stage stage) throws SecurityException {
        for (int lift = stage.inclLower; lift <= stage.inclUpper; lift++) {
            int stageLift = (stage == Stage.SNATCH ? lift + 1 : (lift - 3) + 1);
            for (Changes vals : Changes.values()) {
                String getterName = "get" + stage.label + stageLift + vals.methodSuffix;
                try {
                    // changeGetters[0][Changes.AUTOMATIC] == getSnatch1AutomaticProgression
                    Method method = Athlete.class.getMethod(getterName);
                    logger.debug("changeGetters[{}][{}]= {} = {} ", lift, vals.ordinal(), getterName, method);
                    changeGetters[lift][vals.ordinal()] = method;
                } catch (NoSuchMethodException e) {
                    logger.error("cannot locate getter {}", getterName, e);
                }
            }
            for (Changes vals : Changes.values()) {
                String setterName = "set" + stage.label + stageLift + vals.methodSuffix;
                try {
                    changeSetters[lift][vals.ordinal()] = Athlete.class.getMethod(setterName, String.class);
                } catch (NoSuchMethodException e) {
                    logger.error("cannot locate getter {}", setterName, e);
                }
            }
        }
    }

    public int ordinal;
    public Stage stage;
    public Method[] setters;
    public Method[] getters;

    public LiftDefinition(int ordinal, Stage stage) {
        this.ordinal = ordinal;
        this.stage = stage;
        this.setters = changeSetters[ordinal];
        this.getters = changeGetters[ordinal];
    }

}
