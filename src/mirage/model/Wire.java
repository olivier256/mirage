package mirage.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Le fil de fer de la 3D (comme l'écran de Mirage Type III) : pour chaque
 * case, le losange du dessus à sa hauteur, et les arêtes des faces visibles
 * (gauche, vers +j, et droite, vers +i) là où la case voisine de devant est
 * plus basse. Coordonnées : pixels de l'image de la carte.
 */
public final class Wire {
    private Wire() {
    }

    /** Les segments {x1, y1, x2, y2}. */
    public static List<int[]> segments(HeightMap m) {
        List<int[]> s = new ArrayList<>();
        for (int j = 0; j < m.d; j++)
            for (int i = 0; i < m.w; i++) {
                int h = m.height(i, j), x = m.topX(i, j), y = m.topY(i, j, h);
                /* Losange : haut, droite, bas, gauche */
                s.add(new int[] {x, y, x + 16, y + 8});
                s.add(new int[] {x + 16, y + 8, x, y + 16});
                s.add(new int[] {x, y + 16, x - 16, y + 8});
                s.add(new int[] {x - 16, y + 8, x, y});
                int hl = Math.max(0, m.height(i, j + 1)); /* devant à gauche */
                int hr = Math.max(0, m.height(i + 1, j)); /* devant à droite */
                if (h > hl) { /* face gauche : de la gauche au bas */
                    int dy = 16 * (h - hl);
                    s.add(new int[] {x - 16, y + 8, x - 16, y + 8 + dy});
                    s.add(new int[] {x, y + 16, x, y + 16 + dy});
                    s.add(new int[] {x - 16, y + 8 + dy, x, y + 16 + dy});
                }
                if (h > hr) { /* face droite : du bas à la droite */
                    int dy = 16 * (h - hr);
                    s.add(new int[] {x, y + 16, x, y + 16 + dy});
                    s.add(new int[] {x + 16, y + 8, x + 16, y + 8 + dy});
                    s.add(new int[] {x, y + 16 + dy, x + 16, y + 8 + dy});
                }
            }
        return s;
    }

    /**
     * La case dont le losange du dessus contient le point (x, y) de l'image
     * de la carte ; la plus proche de l'œil s'il y en a plusieurs. {i, j} ou
     * null.
     */
    public static int[] pick(HeightMap m, int x, int y) {
        int[] best = null;
        int bestDepth = -1;
        for (int j = 0; j < m.d; j++)
            for (int i = 0; i < m.w; i++) {
                int h = m.height(i, j);
                int dx = Math.abs(x - m.topX(i, j));
                int dy = Math.abs(y - (m.topY(i, j, h) + 8));
                /* Dans le losange de 32 x 16 : |dx| / 16 + |dy| / 8 < 1 */
                if (dx + 2 * dy < 16) {
                    int depth = 16 * (i + j) + h;
                    if (depth > bestDepth) {
                        bestDepth = depth;
                        best = new int[] {i, j};
                    }
                }
            }
        return best;
    }
}
