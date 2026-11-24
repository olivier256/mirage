package mirage.model;

/**
 * Un projet : une zone de jeu. La palette, les tiles, les deux plans (A
 * devant, B derrière) et la 3D (la carte des hauteurs). La carte fait 64 x 64
 * tiles par défaut (un plan de la Mega Drive), mais elle peut être plus
 * grande : le jeu recopiera en VRAM ce qui entre à l'écran.
 */
public final class Project {
    public Palette palette = new Palette();
    public TileSet tiles = new TileSet();
    public Plane planeA, planeB;
    public HeightMap heights;

    public Project(int mapW, int mapH, HeightMap heights) {
        planeA = new Plane(mapW, mapH);
        planeB = new Plane(mapW, mapH);
        this.heights = heights;
    }

    public static Project empty() {
        return empty(64, 64);
    }

    /** Un projet vide de w x h tiles ; la 3D est un carré centré, comme en 64 x 64. */
    public static Project empty(int w, int h) {
        int n = Math.max(1, Math.min(w, h) / 4);
        Project p = new Project(w, h, new HeightMap(n, n, w * 4, h * 4 - 16));
        p.palette.set(0, 0, 0, 0, 1);
        p.palette.set(0, 1, 7, 7, 7);
        return p;
    }

    public int mapWidth() {
        return planeA.width;
    }

    public int mapHeight() {
        return planeA.height;
    }

    public Plane plane(boolean a) {
        return a ? planeA : planeB;
    }

    /** Change la taille de la carte ; le contenu garde sa place en haut à gauche. */
    public void resize(int w, int h) {
        planeA = planeA.resized(w, h);
        planeB = planeB.resized(w, h);
    }

    /**
     * Déplace le tile s à la position d, comme on déplace un élément dans une
     * liste : les tiles entre les deux glissent d'une place, et les entrées
     * des deux plans sont recalculées. Le tile 0 ne bouge pas.
     */
    public boolean moveTile(int s, int d) {
        if (s == d || s <= 0 || d <= 0 || s >= TileSet.MAX || d >= TileSet.MAX)
            return false;
        tiles.move(s, d);
        for (Plane pl : new Plane[] {planeA, planeB})
            for (int y = 0; y < pl.height; y++)
                for (int x = 0; x < pl.width; x++) {
                    int e = pl.get(x, y), t = Plane.tile(e);
                    pl.set(x, y, Plane.withTile(e, movedIndex(t, s, d)));
                }
        return true;
    }

    /** Où va le tile t quand on déplace s vers d. */
    public static int movedIndex(int t, int s, int d) {
        if (t == s)
            return d;
        if (s < d && t > s && t <= d)
            return t - 1;
        if (d < s && t >= d && t < s)
            return t + 1;
        return t;
    }

    public Project copy() {
        Project p = new Project(1, 1, heights.copy());
        p.palette = palette.copy();
        p.tiles = tiles.copy();
        p.planeA = planeA.copy();
        p.planeB = planeB.copy();
        return p;
    }

    public boolean sameAs(Project o) {
        return palette.sameAs(o.palette) && tiles.sameAs(o.tiles)
            && planeA.sameAs(o.planeA) && planeB.sameAs(o.planeB)
            && heights.sameAs(o.heights);
    }
}
