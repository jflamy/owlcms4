package com.github.appreciated.css.grid.sizes;

import java.util.Arrays;

import com.github.appreciated.css.grid.interfaces.TemplateRowsAndColsUnit;
import com.github.appreciated.css.interfaces.CssUnit;

public class Repeat implements TemplateRowsAndColsUnit {

    public static final String FUNCTION_NAME = "repeat";

    private Integer times;
    private RepeatMode mode;
    private CssUnit[] sizes;

    public Repeat(int times, TemplateRowsAndColsUnit... sizes) {
        this(sizes);
        this.times = times;
    }


    private Repeat(TemplateRowsAndColsUnit... sizes) {
        this.sizes = sizes;
    }

    public Repeat(RepeatMode mode, TemplateRowsAndColsUnit... sizes) {
        this(sizes);
        this.mode = mode;
    }

    @Override
    public String getValue() {
        return Arrays.stream(sizes).map(CssUnit::getCssValue).reduce((unit, unit2) -> unit + " " + unit2).orElse("");
    }

    @Override
    public boolean hasSuffix() {
        return true;
    }

    @Override
    public String getSuffixValue() {
        return ")";
    }

    @Override
    public boolean hasPrefix() {
        return true;
    }

    @Override
    public String getPrefixValue() {
        return FUNCTION_NAME + "(" + (times == null ? mode.getRepeatModeValue() : times.toString()) + ", ";
    }

    public enum RepeatMode {
        AUTO_FILL("auto-fill"),
        AUTO_FIT("auto-fit");

        private final String repeatValue;

        RepeatMode(String repeatValue) {
            this.repeatValue = repeatValue;
        }

        static RepeatMode toRepeatMode(String repeatValue, RepeatMode defaultValue) {
            return Arrays.stream(values())
                    .filter((repeatMode) -> repeatMode.getRepeatModeValue().equals(repeatValue))
                    .findFirst()
                    .orElse(defaultValue);
        }

        String getRepeatModeValue() {
            return this.repeatValue;
        }
    }
}
