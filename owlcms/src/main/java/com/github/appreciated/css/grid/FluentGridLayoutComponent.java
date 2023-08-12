package com.github.appreciated.css.grid;

public interface FluentGridLayoutComponent<T> extends GridLayoutComponent {

    default T withSpacing(boolean spacing) {
        setSpacing(spacing);
        return (T) this;
    }

    default T withPadding(boolean padding) {
        setPadding(padding);
        return (T) this;
    }

    default T withMargin(boolean margin) {
        setMargin(margin);
        return (T) this;
    }

}
