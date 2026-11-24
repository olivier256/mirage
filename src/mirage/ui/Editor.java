package mirage.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import mirage.model.History;
import mirage.model.Project;

/** L'état de l'éditeur, partagé par tous les panneaux. */
public final class Editor {
    /** Tiles par rangée dans la planche. */
    public static final int SHEET_COLS = 32;

    public enum TileTool { PENCIL, LINE, RECT, FILLED_RECT, FILL, PICK }

    public enum MapTool { PLACE, ERASE, PRIO_ON, PRIO_OFF, HEIGHT, PICK }

    public Project project;
    public final History history = new History();
    public Path file;
    public boolean dirty;

    /** Couleur courante (0 à 15) et ligne de palette courante (0 à 3). */
    public int color = 1, line;
    /** Bloc sélectionné dans la planche : premier tile, largeur, hauteur. */
    public int selStart = 1, selW = 1, selH = 1;
    public TileTool tileTool = TileTool.PENCIL;
    public MapTool mapTool = MapTool.PLACE;
    /** Pose : sur le plan A (sinon B), retournements, priorité. */
    public boolean onA, hflip, vflip, priority;
    /** Affichage. */
    public boolean showA = true, showB = true, showWire = true, showGrid, showPriority;
    public int zoom = 2;

    private final List<Runnable> listeners = new ArrayList<>();

    public Editor(Project p) {
        project = p;
    }

    public void listen(Runnable r) {
        listeners.add(r);
    }

    /** Tout le monde se redessine. */
    public void changed() {
        for (Runnable r : listeners)
            r.run();
    }

    /** Juste avant une modification du projet (pour Annuler). */
    public void beginEdit() {
        history.save(project);
        dirty = true;
    }

    /** Le tile en cours d'édition : le premier du bloc sélectionné. */
    public int tile() {
        return Math.min(selStart, project.tiles.count() - 1);
    }

    /** Numéro du tile (x, y) du bloc sélectionné ; -1 s'il n'existe pas. */
    public int blockTile(int x, int y) {
        int t = selStart + y * SHEET_COLS + x;
        return t < project.tiles.count() ? t : -1;
    }

    public void undo() {
        project = history.undo(project);
        dirty = true;
        changed();
    }

    public void redo() {
        project = history.redo(project);
        dirty = true;
        changed();
    }

    public void replace(Project p, Path f) {
        project = p;
        file = f;
        dirty = false;
        history.clear();
        selStart = Math.min(1, p.tiles.count() - 1);
        selW = selH = 1;
        changed();
    }
}
