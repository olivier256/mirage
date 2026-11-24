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
    /** Calque modifié : plan A (sinon plan B) ; calques visibles. */
    public boolean onA, showA = true, showB = true;
    /** Où vont ⌘C / ⌘V : le dernier panneau cliqué. */
    public enum Focus { TILES, MAP }
    public Focus focus = Focus.TILES;
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

    /** ⌘C : selon le panneau actif. */
    public void copy() {
        if (focus == Focus.MAP)
            copyCells();
        else
            copyTiles();
    }

    /** ⌘V : selon le panneau actif ; faux si le presse-papiers ne convient pas. */
    public boolean paste() {
        return focus == Focus.MAP ? pasteCells() : pasteTiles();
    }

    public void setFocus(Focus f) {
        if (focus != f) {
            focus = f;
            changed();
        }
    }

    private static void toClipboard(String text) {
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new java.awt.datatransfer.StringSelection(text), null);
    }

    private static String fromClipboard() {
        try {
            return (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            return null;
        }
    }

    /** Copie les cases sélectionnées du calque modifié (tile et attributs). */
    public void copyCells() {
        String text = mirage.model.MapClip.copy(project.plane(onA), cells);
        if (text != null)
            toClipboard(text);
    }

    /** Colle des cases : coin haut-gauche sur la case sélectionnée la plus en haut à gauche. */
    public boolean pasteCells() {
        String text = fromClipboard();
        if (text == null || cells.isEmpty())
            return false;
        Plane p = project.plane(onA);
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE;
        for (int i = cells.nextSetBit(0); i >= 0; i = cells.nextSetBit(i + 1)) {
            x0 = Math.min(x0, i % p.width);
            y0 = Math.min(y0, i / p.width);
        }
        if (mirage.model.MapClip.paste(p.copy(), text, x0, y0) == null)
            return false;
        beginEdit();
        BitSet done = mirage.model.MapClip.paste(project.plane(onA), text, x0, y0);
        cells.clear();
        cells.or(done);
        entry = p.get(x0, y0);
        selStart = Plane.tile(entry); /* comme un clic : le tile du coin dans la planche */
        selW = selH = 1;
        line = Plane.line(entry);
        changed();
        return true;
    }

    /** Nouvelle taille de carte (annulable) ; la sélection est vidée. */
    public void resize(int w, int h) {
        beginEdit();
        project.resize(w, h);
        cells.clear();
        changed();
    }

    /** Déplace le tile s en d et recalcule les plans (annulable). */
    public boolean moveTile(int s, int d) {
        Project before = project.copy();
        if (!project.moveTile(s, d))
            return false;
        history.save(before);
        dirty = true;
        selStart = d;
        selW = selH = 1;
        entry = Plane.withTile(entry, Project.movedIndex(Plane.tile(entry), s, d));
        changed();
        return true;
    }

    /** Copie le tile (ou le bloc) sélectionné dans la planche, en texte .s. */
    public void copyTiles() {
        toClipboard(mirage.model.TileClip.copy(project.tiles, selStart, selW, selH));
    }

    /** Colle le texte du presse-papiers à partir du tile sélectionné ; faux si illisible. */
    public boolean pasteTiles() {
        String text = fromClipboard();
        if (text == null)
            return false;
        TileSet copy = project.tiles.copy();
        int[] wh = mirage.model.TileClip.paste(copy, text, selStart);
        if (wh == null)
            return false;
        beginEdit();
        mirage.model.TileClip.paste(project.tiles, text, selStart);
        selW = wh[0];
        selH = wh[1];
        changed();
        return true;
    }

    public void undo() {
        int w = project.mapWidth();
        project = history.undo(project);
        if (project.mapWidth() != w)
            cells.clear();
        dirty = true;
        changed();
    }

    public void redo() {
        int w = project.mapWidth();
        project = history.redo(project);
        if (project.mapWidth() != w)
            cells.clear();
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
        focus = Focus.TILES;
        changed();
    }
}
