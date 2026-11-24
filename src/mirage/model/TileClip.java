package mirage.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le copier-coller de tiles, sous forme de texte asm68k : celui de l'export
 * .s (8 lignes « dc.l » par tile, un chiffre par pixel). Il passe par le
 * presse-papiers du système, on peut donc le lire, le retoucher dans un
 * éditeur de texte, ou coller un tile venu d'un source.
 */
public final class TileClip {
    /** Les tiles sont rangés 32 par rangée dans la planche. */
    public static final int COLS = 32;

    private static final Pattern SHAPE = Pattern.compile(";\\s*Mirage\\s*:\\s*bloc\\s+(\\d+)\\s*x\\s*(\\d+)");
    private static final Pattern LONG = Pattern.compile("\\$([0-9A-Fa-f]{8})");
    private static final Pattern DCL = Pattern.compile("(?i)\\bdc\\.l\\b(.*)");

    private TileClip() {
    }

    /** Le bloc w x h de la planche qui commence au tile « start », en texte. */
    public static String copy(TileSet ts, int start, int w, int h) {
        StringBuilder b = new StringBuilder();
        b.append("; Mirage : bloc ").append(w).append(" x ").append(h).append('\n');
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int t = start + y * COLS + x;
                if (t >= TileSet.MAX)
                    continue;
                b.append(String.format("; tile %d ($%03X)%n", t, t));
                for (int r = 0; r < 8; r++) {
                    b.append("\tdc.l\t$");
                    for (int c = 0; c < 8; c++)
                        b.append(Integer.toHexString(ts.pixel(t, c, r)).toUpperCase());
                    b.append('\n');
                }
            }
        return b.toString();
    }

    /** Les tiles lus dans le texte (64 pixels chacun) ; vide si rien de lisible. */
    public static List<byte[]> parse(String text) {
        List<Integer> longs = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String code = line.contains(";") ? line.substring(0, line.indexOf(';')) : line;
            Matcher d = DCL.matcher(code);
            if (!d.find())
                continue;
            Matcher m = LONG.matcher(d.group(1));
            while (m.find())
                longs.add((int) Long.parseLong(m.group(1), 16));
        }
        List<byte[]> tiles = new ArrayList<>();
        for (int i = 0; i + 8 <= longs.size(); i += 8) {
            byte[] px = new byte[64];
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++)
                    px[r * 8 + c] = (byte) (longs.get(i + r) >>> (28 - 4 * c) & 15);
            tiles.add(px);
        }
        return tiles;
    }

    /**
     * Colle le texte à partir du tile « start », avec la forme du bloc copié
     * (sinon par rangées de 32). Le tile 0, toujours vide, n'est pas touché.
     * Renvoie {largeur, hauteur} du bloc collé, ou null si rien n'a été lu.
     */
    public static int[] paste(TileSet ts, String text, int start) {
        List<byte[]> tiles = parse(text);
        if (tiles.isEmpty())
            return null;
        int w = Math.min(tiles.size(), COLS), h = (tiles.size() + w - 1) / w;
        Matcher s = SHAPE.matcher(text);
        if (s.find()) {
            int sw = Integer.parseInt(s.group(1)), sh = Integer.parseInt(s.group(2));
            if (sw > 0 && sh > 0 && sw * sh == tiles.size()) {
                w = sw;
                h = sh;
            }
        }
        for (int i = 0; i < tiles.size(); i++) {
            int t = start + i / w * COLS + i % w;
            if (t > 0 && t < TileSet.MAX)
                ts.setPixels(t, tiles.get(i));
        }
        return new int[] {w, h};
    }
}
