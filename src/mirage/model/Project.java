package mirage.model;

/**
 * Un projet : une zone de jeu. La palette, les tiles, les deux plans (A
 * devant, B derrière) et la 3D (la carte des hauteurs). La carte fait 64 x 64
 * tiles par défaut, soit 512 x 512 pixels : la taille d'un plan de la Mega
 * Drive, qui défile sans rien recharger.
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
        Project p = new Project(64, 64, HeightMap.standard());
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
