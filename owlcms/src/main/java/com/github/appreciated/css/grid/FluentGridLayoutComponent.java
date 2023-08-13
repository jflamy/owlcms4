package com.github.appreciated.css.grid;

public interface FluentGridLayoutComponent<T> extends GridLayoutComponent {

    @SuppressWarnings("unchecked")
	default T withSpacing(boolean spacing) {
        setSpacing(spacing);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
	default T withPadding(boolean padding) {
        setPadding(padding);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
	default T withMargin(boolean margin) {
        setMargin(margin);
        return (T) this;
    }

}
