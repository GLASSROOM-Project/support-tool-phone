package de.glassroom.gst;

/**
 * Filter for selections.
 * @param <E> Class for elements the filter should is applied to.
 */
interface SelectionFilter<E> {
    public abstract boolean accepts(E entry);
}
