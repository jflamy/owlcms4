package com.github.appreciated.css.grid.exception;

import java.util.Arrays;

@SuppressWarnings("serial")
public class NegativeOrZeroValueException extends RuntimeException {
    public NegativeOrZeroValueException(int... value) {
        super("One the passed values \"" + Arrays.stream(value).mapToObj(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("") + "\" is negative or zero. This Constructor requires the parameter to be positive");
    }

    public NegativeOrZeroValueException(double... value) {
        super("One the passed values \"" + Arrays.stream(value).mapToObj(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("") + "\" is negative or zero. This Constructor requires the parameter to be positive");
    }
}
