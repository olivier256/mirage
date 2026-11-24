package mirage.model;

/**
 * Ce qu'afficherait le VDP (sans sprites) : les deux plans superposés, dans
 * l'ordre du VDP, du fond vers l'avant :
 * <pre>
 *   fond (couleur 0 de la ligne 0), plan B sans priorité, plan A sans
 *   priorité, [sprites sans priorité], plan B prioritaire, plan A
 *   prioritaire, [sprites prioritaires]
 * </pre>
 * Les pixels transparents (couleur 0) laissent voir ce qui est derrière.
 */
public final class Render {
    private Render() {
    }

    /** Options d'affichage. */
    public static final class Options {
        public boolean showA = true, showB = true;
        /** Teinte rouge sur les cases prioritaires (du plan affiché). */
        public boolean showPriority;
    }

    /** Pixel d'un tile tel que posé par l'entrée e (retournements appliqués). */
    public static int pixel(TileSet t, int e, int x, int y) {
        int tile = Plane.tile(e);
        if (tile >= t.count())
            return 0;
        int f = Plane.flips(e);
        return t.pixel(tile, (f & 1) != 0 ? 7 - x : x, (f & 2) != 0 ? 7 - y : y);
    }

    /** L'image de toute la carte, ARGB, mapWidth*8 x mapHeight*8. */
    public static int[] map(Project p, Options o) {
        int w = p.mapWidth() * 8, h = p.mapHeight() * 8;
        int[] out = new int[w * h];
        int backdrop = p.palette.rgb(0, 0);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int ea = p.planeA.get(x >> 3, y >> 3), eb = p.planeB.get(x >> 3, y >> 3);
                int ca = o.showA ? pixel(p.tiles, ea, x & 7, y & 7) : 0;
                int cb = o.showB ? pixel(p.tiles, eb, x & 7, y & 7) : 0;
                boolean pa = Plane.priority(ea), pb = Plane.priority(eb);
                int rgb = backdrop;
                boolean prio = false;
                /* Du plus prioritaire au moins prioritaire */
                if (ca != 0 && pa) {
                    rgb = p.palette.rgb(Plane.line(ea), ca);
                    prio = true;
                } else if (cb != 0 && pb) {
                    rgb = p.palette.rgb(Plane.line(eb), cb);
                    prio = true;
                } else if (ca != 0) {
                    rgb = p.palette.rgb(Plane.line(ea), ca);
                } else if (cb != 0) {
                    rgb = p.palette.rgb(Plane.line(eb), cb);
                }
                if (o.showPriority && prio)
                    rgb = tint(rgb);
                out[y * w + x] = rgb;
            }
        return out;
    }

    private static int tint(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return 0xFF000000 | ((r + 255) / 2) << 16 | (g / 2) << 8 | b / 2;
    }
}
