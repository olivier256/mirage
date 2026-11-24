package mirage.model;

/**
 * Le projet de démonstration : une carte des hauteurs (un sol, un mur au
 * fond, une estrade, un escalier, un pilier), « habillée » de tiles
 * provisoires gris et bleus, posés dans le plan B. Le plan A est vide : c'est
 * à vous d'y poser ce qui doit passer devant les personnages, et d'en régler
 * la priorité.
 *
 * L'habillage dessine chaque case de l'arrière vers l'avant (par i + j
 * croissant) : le dessus à sa hauteur, puis ses deux faces jusqu'au sol ; ce
 * qui est caché est recouvert par les cases de devant. L'image est ensuite
 * découpée en tiles de 8 x 8, sans doublon (retournements compris).
 */
public final class DemoBuilder {
    private DemoBuilder() {
    }

    static final int[][] GRIS = {{1, 1, 1}, {2, 2, 2}, {2, 2, 3}, {3, 3, 3}, {3, 3, 4},
        {4, 4, 4}, {4, 4, 5}, {5, 5, 5}, {5, 5, 6}, {6, 6, 6}, {7, 7, 7}};
    static final int[][] BLEU = {{0, 0, 1}, {0, 0, 2}, {1, 1, 2}, {1, 1, 3}, {1, 2, 3},
        {1, 2, 4}, {2, 2, 4}, {2, 3, 5}, {2, 3, 6}, {3, 4, 6}, {4, 5, 7}};
    static final int[][] ROUGE = {{0, 0, 0}, {1, 0, 0}, {3, 0, 0}, {5, 0, 0}, {6, 1, 1},
        {7, 2, 2}, {7, 4, 4}};
    static final int[][] RAYON = {{7, 5, 0}, {7, 7, 0}, {7, 7, 4}, {7, 7, 7}};

    public static Project build() {
        Project p = Project.empty();
        Palette pal = p.palette;
        pal.set(0, 0, 0, 0, 1);
        for (int k = 0; k < 11; k++) {
            pal.set(0, 1 + k, GRIS[k][0], GRIS[k][1], GRIS[k][2]);
            pal.set(1, 1 + k, BLEU[k][0], BLEU[k][1], BLEU[k][2]);
        }
        for (int k = 0; k < ROUGE.length; k++)
            pal.set(2, 1 + k, ROUGE[k][0], ROUGE[k][1], ROUGE[k][2]);
        for (int l = 0; l < 3; l++)
            for (int k = 0; k < 4; k++)
                pal.set(l, 12 + k, RAYON[k][0], RAYON[k][1], RAYON[k][2]);

        HeightMap m = p.heights;
        for (int i = 0; i < m.w; i++)
            m.setHeight(i, 0, 3); /* mur du fond */
        for (int j = 1; j < m.d; j++)
            m.setHeight(0, j, 3);
        for (int i = 3; i <= 6; i++)
            for (int j = 3; j <= 6; j++)
                m.setHeight(i, j, 2); /* estrade */
        for (int k = 0; k < 4; k++)
            for (int j = 9; j <= 10; j++)
                m.setHeight(9 + k, j, 1 + k); /* escalier */
        m.setHeight(12, 4, 4); /* pilier */
        m.setHeight(13, 12, 1);

        int w = p.mapWidth() * 8, h = p.mapHeight() * 8;
        int[] img = new int[w * h]; /* ligne x 16 + couleur ; 0 = rien */
        for (int s = 0; s <= m.w + m.d - 2; s++)
            for (int i = 0; i < m.w; i++) {
                int j = s - i;
                if (j < 0 || j >= m.d)
                    continue;
                drawCell(m, i, j, img, w, h);
            }
        cut(p, img, w, h);
        return p;
    }

    /** Matière : ligne 0 (gris) au sol et aux murs, ligne 1 (bleu) au-dessus. */
    private static int line(HeightMap m, int i, int j) {
        int hh = m.height(i, j);
        return (hh == 2 || hh == 4 || (hh >= 1 && i >= 9 && i <= 12)) ? 1 : 0;
    }

    private static void drawCell(HeightMap m, int i, int j, int[] img, int w, int h) {
        int hh = m.height(i, j), x0 = m.topX(i, j), y0 = m.topY(i, j, hh);
        int line = line(m, i, j) << 4;
        /* Les faces, jusqu'au sol (les cases de devant recouvrent le reste) */
        int depth = 16 * hh + 16;
        for (int dx = 0; dx < 16; dx++)
            for (int dy = 0; dy < depth; dy++) {
                boolean edge = dy == depth - 1 || dx == 0 || dy % 16 == 0;
                put(img, w, h, x0 - 16 + dx, y0 + 8 + dx / 2 + dy,
                    line | (edge ? 2 : (dy / 4 + dx / 4) % 2 == 0 ? 6 : 5));
                put(img, w, h, x0 + dx, y0 + 16 - dx / 2 + dy,
                    line | (edge ? 1 : (dy / 4 + dx / 4) % 2 == 0 ? 4 : 3));
            }
        /* Le dessus : un losange de 32 x 16 */
        for (int r = 0; r < 16; r++) {
            int half = r < 8 ? 2 * (r + 1) : 2 * (15 - r);
            for (int dx = -half; dx < half; dx++) {
                boolean edge = dx == -half || dx == half - 1 || r == 0 || r == 15;
                int u = (dx + 2 * r) & 15, v = (2 * r - dx) & 15;
                int c = edge ? 3 : (u < 1 || v < 1) ? 6 : ((u / 4 + v / 4) % 2 == 0 ? 9 : 8);
                put(img, w, h, x0 + dx, y0 + r, line | c);
            }
        }
    }

    private static void put(int[] img, int w, int h, int x, int y, int v) {
        if (x >= 0 && y >= 0 && x < w && y < h)
            img[y * w + x] = v;
    }

    /** Découpe l'image en tiles du plan B (une ligne de palette par tile). */
    private static void cut(Project p, int[] img, int w, int h) {
        for (int ty = 0; ty < h / 8; ty++)
            for (int tx = 0; tx < w / 8; tx++) {
                int[] count = new int[4];
                for (int y = 0; y < 8; y++)
                    for (int x = 0; x < 8; x++) {
                        int v = img[(ty * 8 + y) * w + tx * 8 + x];
                        if (v != 0)
                            count[v >> 4]++;
                    }
                int line = 0;
                for (int l = 1; l < 4; l++)
                    if (count[l] > count[line])
                        line = l;
                if (count[line] == 0)
                    continue;
                byte[] px = new byte[64];
                for (int y = 0; y < 8; y++)
                    for (int x = 0; x < 8; x++) {
                        int v = img[(ty * 8 + y) * w + tx * 8 + x];
                        if (v == 0)
                            continue;
                        int c = v & 15;
                        if (v >> 4 != line)
                            c = nearest(p.palette, v >> 4, c, line);
                        px[y * 8 + x] = (byte) c;
                    }
                int[] found = p.tiles.find(px);
                int tile, flips = 0;
                if (found != null) {
                    tile = found[0];
                    flips = found[1];
                } else {
                    tile = p.tiles.add(px);
                    if (tile < 0)
                        throw new IllegalStateException("trop de tiles");
                }
                p.planeB.set(tx, ty, Plane.entry(tile, line, (flips & 1) != 0,
                    (flips & 2) != 0, false));
            }
    }

    private static int nearest(Palette pal, int fromLine, int color, int toLine) {
        int best = 1, bestD = Integer.MAX_VALUE;
        for (int c = 1; c < 12; c++) {
            int d = 0;
            for (int k = 0; k < 3; k++) {
                int e = pal.level(fromLine, color, k) - pal.level(toLine, c, k);
                d += e * e;
            }
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }
}
