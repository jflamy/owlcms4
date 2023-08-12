package com.github.appreciated.css.grid.sizes;

import com.github.appreciated.css.grid.interfaces.RowOrColUnit;

/**
 * The implementation of <a href="https://developer.mozilla.org/de/docs/Web/CSS/custom-ident">custom-ident</a>
 */
public class CustomIdent implements RowOrColUnit {

    private Integer length;
    private String identifier;


    public CustomIdent(String identifier) {
        this(null, identifier);
    }

    public CustomIdent(Integer length, String identifier) {
        this.length = length;
        this.identifier = identifier;
    }

    @Override
    public String getValue() {
        return length == null ? identifier : length + " " + identifier;
    }
}
