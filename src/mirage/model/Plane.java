package mirage.model;

import java.util.Arrays;

/**
 * Une table de plan (plan A ou plan B) : une entrée de 16 bits par case de
 * 8 x 8 pixels, comme dans la VRAM :
 * <pre>
 *   bit 15      priorité
 *   bits 14-13  ligne de palette
 *   bit 12      retournement vertical
 *   bit 11      retournement horizontal
 *   bits 10-0   numéro du tile
 * </pre>
 */
public final class Plane {
    public static final int PRIORITY = 0x8000;
    public static final int VFLIP = 0x1000;
    public static final int HFLIP = 0x0800;

    public final int width, height;
    private final int[] cells;

    public Plane(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new int[width * height];
    }

    public int get(int x, int y) {
        return cells[y * width + x];
    }

    public void set(int x, int y, int entry) {
        cells[y * width + x] = entry & 0xFFFF;
    }

    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public static int entry(int tile, int line, boolean hflip, boolean vflip,
                            boolean priority) {
        return (priority ? PRIORITY : 0) | (line & 3) << 13 | (vflip ? VFLIP : 0)
            | (hflip ? HFLIP : 0) | (tile & 0x7FF);
    }

    public static int tile(int e) {
        return e & 0x7FF;
    }

    public static int line(int e) {
        return (e >> 13) & 3;
    }

    public static boolean priority(int e) {
        return (e & PRIORITY) != 0;
    }

    /** bit 0 : horizontal, bit 1 : vertical (comme TileSet.flip). */
    public static int flips(int e) {
        return ((e & HFLIP) != 0 ? 1 : 0) | ((e & VFLIP) != 0 ? 2 : 0);
    }

    public Plane copy() {
        Plane p = new Plane(width, height);
        System.arraycopy(cells, 0, p.cells, 0, cells.length);
        return p;
    }

    public boolean sameAs(Plane o) {
        return o.width == width && o.height == height && Arrays.equals(cells, o.cells);
    }
}
