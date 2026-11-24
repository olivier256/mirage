package mirage.model;

import java.util.Arrays;

/**
 * La 3D, comme le DDS520 de Landstalker : une grille de losanges (cases) de
 * W x D, chacune d'une hauteur de 0 à 15 et d'un type (0 : normal ; les
 * autres valeurs serviront aux portes, escaliers, eau...).
 *
 * Repère (celui du DDS520) : la case (i, j) ; i va vers le bas à droite de
 * l'écran, j vers le bas à gauche, la hauteur h vers le haut. Une case est un
 * losange de 32 x 16 pixels ; un niveau de hauteur fait 16 pixels. Le sommet
 * du haut du losange de la case (i, j) à la hauteur h tombe, dans l'image de
 * la carte, en
 * <pre>
 *   x = originX + 16 (i - j),   y = originY + 8 (i + j) - 16 h.
 * </pre>
 */
public final class HeightMap {
    public static final int MAX_HEIGHT = 15;

    public final int w, d;
    public int originX, originY;
    private final int[] height, type;

    public HeightMap(int w, int d, int originX, int originY) {
        this.w = w;
        this.d = d;
        this.originX = originX;
        this.originY = originY;
        height = new int[w * d];
        type = new int[w * d];
    }

    /** La grille qui remplit une carte de 64 x 64 tiles (512 x 512 pixels). */
    public static HeightMap standard() {
        return new HeightMap(16, 16, 256, 240);
    }

    public boolean inside(int i, int j) {
        return i >= 0 && j >= 0 && i < w && j < d;
    }

    /** Hauteur ; -1 hors de la grille. */
    public int height(int i, int j) {
        return inside(i, j) ? height[j * w + i] : -1;
    }

    public void setHeight(int i, int j, int h) {
        if (inside(i, j))
            height[j * w + i] = Math.max(0, Math.min(MAX_HEIGHT, h));
    }

    public int type(int i, int j) {
        return inside(i, j) ? type[j * w + i] : 0;
    }

    public void setType(int i, int j, int t) {
        if (inside(i, j))
            type[j * w + i] = t & 15;
    }

    /** Sommet du haut du losange de la case (i, j) à la hauteur h. */
    public int topX(int i, int j) {
        return originX + 16 * (i - j);
    }

    public int topY(int i, int j, int h) {
        return originY + 8 * (i + j) - 16 * h;
    }

    public HeightMap copy() {
        HeightMap m = new HeightMap(w, d, originX, originY);
        System.arraycopy(height, 0, m.height, 0, height.length);
        System.arraycopy(type, 0, m.type, 0, type.length);
        return m;
    }

    public boolean sameAs(HeightMap o) {
        return o.w == w && o.d == d && o.originX == originX && o.originY == originY
            && Arrays.equals(height, o.height) && Arrays.equals(type, o.type);
    }
}
