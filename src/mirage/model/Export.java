package mirage.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * L'export pour la Mega Drive, des fichiers binaires prêts pour INCBIN :
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

    public static byte[] tiles(Project p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int t = 0; t < p.tiles.count(); t++)
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

    public static void write(Project p, Path dir) throws IOException {
        Files.createDirectories(dir);
        Files.write(dir.resolve("tiles.bin"), tiles(p));
        Files.write(dir.resolve("plane_a.bin"), plane(p.planeA));
        Files.write(dir.resolve("plane_b.bin"), plane(p.planeB));
        Files.write(dir.resolve("cram.bin"), cram(p.palette));
        Files.write(dir.resolve("heights.bin"), heights(p.heights));
        String inc = "; Exporté par Mirage : ne pas modifier.\n"
            + "TILE_COUNT\tEQU\t" + p.tiles.count() + "\n"
            + "MAP_W\t\tEQU\t" + p.mapWidth() + "\n"
            + "MAP_H\t\tEQU\t" + p.mapHeight() + "\n"
            + "ZONE_W\t\tEQU\t" + p.heights.w + "\n"
            + "ZONE_D\t\tEQU\t" + p.heights.d + "\n"
            + "ORIGIN_X\tEQU\t" + p.heights.originX + "\n"
            + "ORIGIN_Y\tEQU\t" + p.heights.originY + "\n";
        Files.writeString(dir.resolve("map.inc"), inc);
    }
}
