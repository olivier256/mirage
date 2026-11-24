package mirage.model;

import java.util.ArrayDeque;
import java.util.Deque;

/** Annuler et rétablir : des copies du projet (quelques centaines de Ko chacune). */
public final class History {
    private static final int MAX = 64;
    private final Deque<Project> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();

    /** À appeler juste avant une modification. */
    public void save(Project p) {
        undo.push(p.copy());
        if (undo.size() > MAX)
            undo.removeLast();
        redo.clear();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public Project undo(Project current) {
        if (undo.isEmpty())
            return current;
        redo.push(current.copy());
        return undo.pop();
    }

    public Project redo(Project current) {
        if (redo.isEmpty())
            return current;
        undo.push(current.copy());
        return redo.pop();
    }

    public void clear() {
        undo.clear();
        redo.clear();
    }
}
