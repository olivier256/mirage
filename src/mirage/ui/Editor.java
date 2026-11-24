package mirage.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import mirage.model.Plane;
import mirage.model.TileSet;
import mirage.model.History;
import mirage.model.Project;

/** L'état de l'éditeur, partagé par tous les panneaux. */
public final class Editor {
    /** Tiles par rangée dans la planche. */
    public static final int SHEET_COLS = 32;

    public enum TileTool { PENCIL, LINE, RECT, FILLED_RECT, FILL, PICK }

    public enum MapTool { SELECT, PLACE, ERASE }

    public Project project;
    public final History history = new History();
    public Path file;
    public boolean dirty;

    /** Couleur et palette (0 à 3) pour dessiner et voir les tiles. */
    public int color = 1, line;
    /** Bloc sélectionné dans la planche : premier tile, largeur, hauteur. */
    public int selStart = 1, selW = 1, selH = 1;
    public TileTool tileTool = TileTool.PENCIL;
    public MapTool mapTool = MapTool.SELECT;
    /** Onglet de la tilemap : plan A (sinon plan B). */
    public boolean onA;
    /**
     * L'entrée en cours, pccvhnnn nnnnnnnn : celle de la case de référence
     * de la sélection ; ses champs s'appliquent à toutes les cases
     * sélectionnées, et l'outil Poser s'en sert pour les nouvelles cases.
     */
    public int entry = 1;
    /** Cases sélectionnées dans la tilemap (indice y * largeur + x). */
    public final BitSet cells = new BitSet();
    /** Affichage (le fil de fer ne sert que dans la carte, pour l'instant). */
    public boolean showWire = true, showGrid, showPriority;
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
        return Math.min(selStart, TileSet.MAX - 1);
    }

    /** Numéro du tile (x, y) du bloc sélectionné ; -1 s'il n'existe pas. */
    public int blockTile(int x, int y) {
        int t = selStart + y * SHEET_COLS + x;
        return t < TileSet.MAX ? t : -1;
    }

    /** Applique la modification f à l'entrée en cours et aux cases sélectionnées. */
    public void editEntry(java.util.function.IntUnaryOperator f) {
        beginEdit();
        entry = f.applyAsInt(entry);
        Plane p = project.plane(onA);
        for (int i = cells.nextSetBit(0); i >= 0; i = cells.nextSetBit(i + 1)) {
            int x = i % p.width, y = i / p.width;
            p.set(x, y, f.applyAsInt(p.get(x, y)));
        }
        changed();
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
        selStart = 1;
        selW = selH = 1;
        cells.clear();
        changed();
    }
}
