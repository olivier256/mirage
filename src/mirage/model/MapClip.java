package mirage.model;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Le copier-coller de cases de la tilemap, en texte asm68k : le rectangle
 * qui englobe la sélection, une rangée par ligne « dc.w » (entrées
 * pccvhnnn nnnnnnnn), et, si la sélection n'est pas un rectangle, un
 * masque en commentaire (# = case copiée, . = case laissée telle quelle).
 */
public final class MapClip {
    private static final Pattern SHAPE = Pattern.compile(";\\s*Mirage\\s*:\\s*cases\\s+(\\d+)\\s*x\\s*(\\d+)");
    private static final Pattern MASK = Pattern.compile(";\\s*masque\\s+([#.]+)");
    private static final Pattern WORD = Pattern.compile("\\$([0-9A-Fa-f]{1,4})");
    private static final Pattern DCW = Pattern.compile("(?i)\\bdc\\.w\\b(.*)");

    private MapClip() {
    }

    /** Les cases sélectionnées du plan (indices y * largeur + x), en texte ; null si rien. */
    public static String copy(Plane pl, BitSet cells) {
        if (cells.isEmpty())
            return null;
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = -1, y1 = -1;
        for (int i = cells.nextSetBit(0); i >= 0; i = cells.nextSetBit(i + 1)) {
            int x = i % pl.width, y = i / pl.width;
            x0 = Math.min(x0, x);
            y0 = Math.min(y0, y);
            x1 = Math.max(x1, x);
            y1 = Math.max(y1, y);
        }
        int w = x1 - x0 + 1, h = y1 - y0 + 1;
        boolean full = cells.cardinality() == w * h;
        StringBuilder b = new StringBuilder();
        b.append("; Mirage : cases ").append(w).append(" x ").append(h).append('\n');
        if (!full)
            for (int y = y0; y <= y1; y++) {
                b.append("; masque ");
                for (int x = x0; x <= x1; x++)
                    b.append(cells.get(y * pl.width + x) ? '#' : '.');
                b.append('\n');
            }
        for (int y = y0; y <= y1; y++) {
            b.append("\tdc.w\t");
            for (int x = x0; x <= x1; x++)
                b.append(x > x0 ? "," : "").append(String.format("$%04X", pl.get(x, y)));
            b.append('\n');
        }
        return b.toString();
    }

    /**
     * Colle le texte avec son coin haut-gauche en (x0, y0). Renvoie les cases
     * écrites (pour en faire la nouvelle sélection), ou null si le texte n'est
     * pas un bloc de cases. Ce qui sort de la carte est ignoré.
     */
    public static BitSet paste(Plane pl, String text, int x0, int y0) {
        Matcher s = SHAPE.matcher(text);
        if (!s.find())
            return null;
        int w = Integer.parseInt(s.group(1)), h = Integer.parseInt(s.group(2));
        List<Integer> words = new ArrayList<>();
        List<String> mask = new ArrayList<>();
        for (String line : text.split("\\R")) {
            Matcher m = MASK.matcher(line);
            if (m.find()) {
                mask.add(m.group(1));
                continue;
            }
            String code = line.contains(";") ? line.substring(0, line.indexOf(';')) : line;
            Matcher d = DCW.matcher(code);
            if (!d.find())
                continue;
            Matcher v = WORD.matcher(d.group(1));
            while (v.find())
                words.add(Integer.parseInt(v.group(1), 16));
        }
        if (w <= 0 || h <= 0 || words.size() != w * h || (!mask.isEmpty() && mask.size() != h))
            return null;
        BitSet out = new BitSet();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if (!mask.isEmpty() && (x >= mask.get(y).length() || mask.get(y).charAt(x) != '#'))
                    continue;
                int tx = x0 + x, ty = y0 + y;
                if (!pl.inside(tx, ty))
                    continue;
                pl.set(tx, ty, words.get(y * w + x));
                out.set(ty * pl.width + tx);
            }
        return out;
    }
}
