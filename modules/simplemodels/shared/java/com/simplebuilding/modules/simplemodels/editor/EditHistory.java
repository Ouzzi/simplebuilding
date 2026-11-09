package com.simplebuilding.modules.simplemodels.editor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Editor stage 1 (b-models12): undo/redo over immutable states (an {@link EditableModel} per step). The caller pushes once
 * per user action (a whole gizmo drag, one code edit taken over), not per mouse move. Pushing the current state again is
 * no step; a new step clears redo; past {@code limit} the oldest step is dropped.
 */
public final class EditHistory<T> {
    public static final int DEFAULT_LIMIT = 200;
    private final Deque<T> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final int limit;
    private T current;

    public EditHistory(T initial) { this(initial, DEFAULT_LIMIT); }
    public EditHistory(T initial, int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit < 1");
        this.current = Objects.requireNonNull(initial);
        this.limit = limit;
    }

    public T current() { return current; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public int undoSteps() { return undo.size(); }

    /** Records {@code next} as a new step; returns false (and changes nothing) when it equals the current state. */
    public boolean push(T next) {
        Objects.requireNonNull(next);
        if (next.equals(current)) return false;
        undo.push(current);
        if (undo.size() > limit) undo.removeLast();
        redo.clear();
        current = next;
        return true;
    }

    public T undo() {
        if (undo.isEmpty()) return current;
        redo.push(current);
        return current = undo.pop();
    }

    public T redo() {
        if (redo.isEmpty()) return current;
        undo.push(current);
        return current = redo.pop();
    }
}
