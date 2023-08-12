package com.github.appreciated.css.grid;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;

import java.util.Arrays;

public interface GridLayoutComponent extends HasElement, ThemableLayout, HasSize, HasStyle, HasOrderedComponents {

    default void setColumnAlign(Component component, ColumnAlign align) {
        for (String cssProperty : ColumnAlign.cssProperties) {
            component.getElement().getStyle().set(cssProperty, align.getAlignValue());
        }
    }

    default void setRowAlign(Component component, RowAlign align) {
        for (String cssProperty : RowAlign.cssProperties) {
            component.getElement().getStyle().set(cssProperty, align.getAlignValue());
        }
    }

    default void setOverflow(Overflow overflow) {
        if (overflow == null) {
            getElement().getStyle().remove(Overflow.cssProperty);
        } else {
            getElement().getStyle().set(Overflow.cssProperty, overflow.getOverflowValue());
        }
    }

    enum ColumnAlign {
        START("start"),
        END("end"),
        CENTER("center"),
        STRETCH("stretch");

        public static final String[] cssProperties = new String[]{"justify-self"};
        private final String alignValue;

        ColumnAlign(String alignValue) {
            this.alignValue = alignValue;
        }

        static ColumnAlign toColumnAlign(String alignValue, ColumnAlign defaultValue) {
            return Arrays.stream(values())
                    .filter((justifyContent) -> justifyContent.getAlignValue().equals(alignValue))
                    .findFirst()
                    .orElse(defaultValue);
        }

        String getAlignValue() {
            return this.alignValue;
        }
    }

    enum RowAlign {
        START("start"),
        END("end"),
        CENTER("center"),
        STRETCH("stretch");

        public static final String[] cssProperties = new String[]{"align-self"};
        private final String alignValue;

        RowAlign(String alignValue) {
            this.alignValue = alignValue;
        }

        static RowAlign toAlignment(String alignValue, RowAlign defaultValue) {
            return Arrays.stream(values())
                    .filter((alignment) -> alignment.getAlignValue().equals(alignValue))
                    .findFirst()
                    .orElse(defaultValue);
        }

        String getAlignValue() {
            return this.alignValue;
        }
    }


    enum AutoFlow {
        ROW("row"),
        COLUMN("column"),
        ROW_DENSE("row dense"),
        COLUMN_DENSE("column dense");

        public static final String cssProperty = "grid-auto-flow";
        private final String autoFlowValue;

        AutoFlow(String autoFlowValue) {
            this.autoFlowValue = autoFlowValue;
        }

        public static AutoFlow toAutoFlow(String autoFlowValue) {
            return toAutoFlow(autoFlowValue, ROW);
        }

        public static AutoFlow toAutoFlow(String autoFlowValue, AutoFlow defaultValue) {
            return Arrays.stream(values())
                    .filter((alignment) -> alignment.getAutoFlowValue().equals(autoFlowValue))
                    .findFirst()
                    .orElse(defaultValue);
        }

        public String getAutoFlowValue() {
            return this.autoFlowValue;
        }
    }

    enum Overflow {
        VISIBLE("visible"),
        HIDDEN("hidden"),
        SCROLL("scroll"),
        AUTO("auto");

        public static final String cssProperty = "overflow";
        private final String overflowValue;

        Overflow(String overflowValue) {
            this.overflowValue = overflowValue;
        }

        public static Overflow toOverflow(String autoFlowValue) {
            return toOverflow(autoFlowValue, VISIBLE);
        }

        public static Overflow toOverflow(String overFlowValue, Overflow defaultValue) {
            return Arrays.stream(values())
                    .filter((alignment) -> alignment.getOverflowValue().equals(overFlowValue))
                    .findFirst()
                    .orElse(defaultValue);
        }

        public String getOverflowValue() {
            return this.overflowValue;
        }
    }


}
