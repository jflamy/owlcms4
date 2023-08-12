package com.github.appreciated.css.grid.exception;

import java.util.Arrays;

public class NegativeValueException extends RuntimeException {
    public NegativeValueException(int... value) {
        super("One the passed values \"" + Arrays.stream(value).mapToObj(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("") + "\" is negative. This Constructor requires the parameter to be positive");
    }

    public NegativeValueException(double... value) {
        super("One the passed values \"" + Arrays.stream(value).mapToObj(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("") + "\" is negative. This Constructor requires the parameter to be positive");
    }
}
