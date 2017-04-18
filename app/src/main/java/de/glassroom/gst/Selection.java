package de.glassroom.gst;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Selection<E> {

    private int cursor;
    private final List<E> entries;

    public final Set<SelectionHandler> handlers;

    public Selection(List<E> entries) throws IllegalArgumentException {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("A selection requires an non-empty list of entries.");
        }
        this.entries = entries;
        cursor = 0;
        handlers = new HashSet<>();
    }

    public E reset() {
        cursor = 0;
        return entries.get(cursor);
    }

    public boolean isFirst() {
        return cursor == 0;
    }

    public boolean isLast() {
        return cursor == entries.size() - 1;
    }

    public E next() throws IllegalStateException {
        if (isLast()) {
            throw new IllegalStateException("Last entry is already selected.");
        }
        cursor += 1;
        notifyHandlers();
        return entries.get(cursor);
    }

    public E previous() throws IllegalStateException {
        if (isFirst()) {
            throw new IllegalStateException("The first element is selected.");
        }
        cursor -= 1;
        notifyHandlers();
        return entries.get(cursor);
    }

    public E getCurrentSelection() {
        return entries.get(cursor);
    }

    public double getProgress() {
        return (cursor + 1.0) / entries.size();
    }

    public void addHandler(SelectionHandler<E> handler) {
        handlers.add(handler);
        handler.selectionChanged(getCurrentSelection());
    }

    private void notifyHandlers() {
        for (SelectionHandler<E> handler : handlers) {
            handler.selectionChanged(getCurrentSelection());
        }
    }

    public int getCursor() {
        return cursor;
    }

    public int size() {
        return entries.size();
    }
}
