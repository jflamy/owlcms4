package com.github.appreciated.css.grid;

public interface HasOverflow<T> extends GridLayoutComponent {

    @SuppressWarnings("unchecked")
	default T withOverflow(Overflow overflow) {
        setOverflow(overflow);
        return (T) this;
    }

}
