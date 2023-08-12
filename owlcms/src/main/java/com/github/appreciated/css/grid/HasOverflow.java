package com.github.appreciated.css.grid;

public interface HasOverflow<T> extends GridLayoutComponent {

    default T withOverflow(Overflow overflow) {
        setOverflow(overflow);
        return (T) this;
    }

}
