package mirage.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * L'export pour la Mega Drive, au choix en binaire (INCBIN) ou en source
 * asm68k (.s, lisible : un tile = 8 lignes dc.l, un chiffre par pixel) :
 * <pre>
 *   tiles.bin    32 octets par tile (4 bits par pixel)
 *   plane_a.bin  la table du plan A, une entrée de 16 bits par case
 *   plane_b.bin  la table du plan B (gros-boutiste, ligne par ligne)
 *   cram.bin     les 64 mots de la CRAM
 *   heights.bin  W x D octets : hauteur (bits 0-3) et type (bits 4-7)
 *   map.inc      les dimensions, pour l'assembleur
 * </pre>
 */
public final class Export {
    private Export() {
    }

    /** Tiles exportés : jusqu'au dernier dessiné ou référencé par un plan. */
    public static int tileCount(Project p) {
        int n = p.tiles.count();
        for (Plane pl : new Plane[] {p.planeA, p.planeB})
            for (int y = 0; y < pl.height; y++)
                for (int x = 0; x < pl.width; x++)
                    n = Math.max(n, Plane.tile(pl.get(x, y)) + 1);
        return n;
    }

    public static byte[] tiles(Project p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int t = 0; t < tileCount(p); t++)
            out.writeBytes(p.tiles.pack(t));
        return out.toByteArray();
    }

    public static byte[] plane(Plane pl) {
        byte[] out = new byte[pl.width * pl.height * 2];
        for (int y = 0; y < pl.height; y++)
            for (int x = 0; x < pl.width; x++) {
                int e = pl.get(x, y), o = (y * pl.width + x) * 2;
                out[o] = (byte) (e >> 8);
                out[o + 1] = (byte) e;
            }
        return out;
    }

    public static byte[] cram(Palette pal) {
        byte[] out = new byte[128];
        for (int l = 0; l < Palette.LINES; l++)
            for (int c = 0; c < Palette.COLORS; c++) {
                int w = pal.cramWord(l, c), o = (l * 16 + c) * 2;
                out[o] = (byte) (w >> 8);
                out[o + 1] = (byte) w;
            }
        return out;
    }

    public static byte[] heights(HeightMap h) {
        byte[] out = new byte[h.w * h.d];
        for (int j = 0; j < h.d; j++)
            for (int i = 0; i < h.w; i++)
                out[j * h.w + i] = (byte) (h.type(i, j) << 4 | h.height(i, j));
        return out;
    }

    public enum Format { BIN, ASM }

    public static void write(Project p, Path dir) throws IOException {
        write(p, dir, Format.BIN);
    }

    public static void write(Project p, Path dir, Format f) throws IOException {
        Files.createDirectories(dir);
        if (f == Format.BIN) {
            Files.write(dir.resolve("tiles.bin"), tiles(p));
            Files.write(dir.resolve("plane_a.bin"), plane(p.planeA));
            Files.write(dir.resolve("plane_b.bin"), plane(p.planeB));
            Files.write(dir.resolve("cram.bin"), cram(p.palette));
            Files.write(dir.resolve("heights.bin"), heights(p.heights));
        } else {
            Files.writeString(dir.resolve("tiles.s"), tilesAsm(p));
            Files.writeString(dir.resolve("plane_a.s"), planeAsm("PlaneA", p.planeA));
            Files.writeString(dir.resolve("plane_b.s"), planeAsm("PlaneB", p.planeB));
            Files.writeString(dir.resolve("cram.s"), cramAsm(p.palette));
            Files.writeString(dir.resolve("heights.s"), heightsAsm(p.heights));
        }
        Files.writeString(dir.resolve("map.inc"), inc(p));
    }

    static final String HEAD = "; Exporté par Mirage : ne pas modifier.\n";

    public static String inc(Project p) {
        return HEAD
            + "TILE_COUNT\tEQU\t" + tileCount(p) + "\n"
            + "MAP_W\t\tEQU\t" + p.mapWidth() + "\n"
            + "MAP_H\t\tEQU\t" + p.mapHeight() + "\n"
            + "ZONE_W\t\tEQU\t" + p.heights.w + "\n"
            + "ZONE_D\t\tEQU\t" + p.heights.d + "\n"
            + "ORIGIN_X\tEQU\t" + p.heights.originX + "\n"
            + "ORIGIN_Y\tEQU\t" + p.heights.originY + "\n";
    }

    /** Un tile = 8 lignes « dc.l $xxxxxxxx » : chaque chiffre est un pixel. */
    public static String tilesAsm(Project p) {
        StringBuilder b = new StringBuilder(HEAD).append("Tiles:\n");
        for (int t = 0; t < tileCount(p); t++) {
            b.append(String.format("; tile %d ($%03X)%n", t, t));
            for (int y = 0; y < 8; y++) {
                b.append("\tdc.l\t$");
                for (int x = 0; x < 8; x++)
                    b.append(Integer.toHexString(p.tiles.pixel(t, x, y)).toUpperCase());
                b.append('\n');
            }
        }
        return b.toString();
    }

    /** Une rangée de la table = une ligne dc.w (entrées pccvhnnn nnnnnnnn). */
    public static String planeAsm(String label, Plane pl) {
        StringBuilder b = new StringBuilder(HEAD).append(label).append(":\n");
        for (int y = 0; y < pl.height; y++) {
            b.append(String.format("; rangée %d%n", y));
            for (int x0 = 0; x0 < pl.width; x0 += 16) {
                b.append("\tdc.w\t");
                for (int x = x0; x < Math.min(x0 + 16, pl.width); x++)
                    b.append(x > x0 ? "," : "").append(String.format("$%04X", pl.get(x, y)));
                b.append('\n');
            }
        }
        return b.toString();
    }

    public static String cramAsm(Palette pal) {
        StringBuilder b = new StringBuilder(HEAD).append("Cram:\n");
        for (int l = 0; l < Palette.LINES; l++) {
            b.append(String.format("; palette %d%n\tdc.w\t", l));
            for (int c = 0; c < Palette.COLORS; c++)
                b.append(c > 0 ? "," : "").append(String.format("$%04X", pal.cramWord(l, c)));
            b.append('\n');
        }
        return b.toString();
    }

    public static String heightsAsm(HeightMap h) {
        StringBuilder b = new StringBuilder(HEAD).append("Heights:\n");
        for (int j = 0; j < h.d; j++) {
            b.append("\tdc.b\t");
            for (int i = 0; i < h.w; i++)
                b.append(i > 0 ? "," : "").append(String.format("$%02X", h.type(i, j) << 4 | h.height(i, j)));
            b.append('\n');
        }
        return b.toString();
    }
}
