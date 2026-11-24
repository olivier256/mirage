package mirage.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Le fichier de projet (.mirage) : du texte, lisible et comparable dans git.
 * <pre>
 *   mirage 1
 *   palette
 *   4 lignes de 16 couleurs « RGB » (niveaux 0 à 7), séparées par des espaces
 *   tiles N
 *   N lignes de 64 chiffres hexadécimaux (les pixels, ligne par ligne)
 *   map W H
 *   planeA, puis H lignes de W entrées hexadécimales de 4 chiffres
 *   planeB, idem
 *   heights W D originX originY
 *   D lignes de W chiffres hexadécimaux (hauteurs), puis D lignes (types)
 *   end
 * </pre>
 */
public final class ProjectIO {
    private ProjectIO() {
    }

    public static void save(Project p, Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(p, w);
        }
    }

    public static Project load(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(r);
        }
    }

    public static void write(Project p, Writer w) throws IOException {
        StringBuilder s = new StringBuilder("mirage 1\npalette\n");
        for (int l = 0; l < Palette.LINES; l++) {
            for (int c = 0; c < Palette.COLORS; c++) {
                if (c > 0)
                    s.append(' ');
                for (int k = 0; k < 3; k++)
                    s.append(p.palette.level(l, c, k));
            }
            s.append('\n');
        }
        s.append("tiles ").append(p.tiles.count()).append('\n');
        for (int t = 0; t < p.tiles.count(); t++) {
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++)
                    s.append(Character.forDigit(p.tiles.pixel(t, x, y), 16));
            s.append('\n');
        }
        s.append("map ").append(p.mapWidth()).append(' ').append(p.mapHeight()).append('\n');
        for (int a = 1; a >= 0; a--) {
            Plane pl = p.plane(a == 1);
            s.append(a == 1 ? "planeA\n" : "planeB\n");
            for (int y = 0; y < pl.height; y++) {
                for (int x = 0; x < pl.width; x++) {
                    if (x > 0)
                        s.append(' ');
                    s.append(String.format("%04x", pl.get(x, y)));
                }
                s.append('\n');
            }
        }
        HeightMap h = p.heights;
        s.append("heights ").append(h.w).append(' ').append(h.d).append(' ')
            .append(h.originX).append(' ').append(h.originY).append('\n');
        for (int pass = 0; pass < 2; pass++)
            for (int j = 0; j < h.d; j++) {
                for (int i = 0; i < h.w; i++)
                    s.append(Character.forDigit(pass == 0 ? h.height(i, j) : h.type(i, j), 16));
                s.append('\n');
            }
        s.append("end\n");
        w.write(s.toString());
    }

    public static Project read(Reader in) throws IOException {
        BufferedReader r = new BufferedReader(in);
        expect(r, "mirage 1");
        expect(r, "palette");
        Palette pal = new Palette();
        for (int l = 0; l < Palette.LINES; l++) {
            String[] f = line(r).trim().split("\\s+");
            if (f.length != Palette.COLORS)
                throw new IOException("palette : 16 couleurs attendues");
            for (int c = 0; c < Palette.COLORS; c++) {
                if (f[c].length() != 3)
                    throw new IOException("couleur mal formée : " + f[c]);
                pal.set(l, c, digit(f[c].charAt(0)), digit(f[c].charAt(1)), digit(f[c].charAt(2)));
            }
        }
        String[] head = line(r).split("\\s+");
        if (head.length != 2 || !head[0].equals("tiles"))
            throw new IOException("« tiles N » attendu");
        int n = Integer.parseInt(head[1]);
        if (n < 1 || n > TileSet.MAX)
            throw new IOException("nombre de tiles refusé : " + n);
        TileSet tiles = new TileSet();
        tiles.ensure(n);
        for (int t = 0; t < n; t++) {
            String px = line(r).trim();
            if (px.length() != 64)
                throw new IOException("tile " + t + " : 64 chiffres attendus");
            for (int k = 0; k < 64; k++)
                tiles.setPixel(t, k % 8, k / 8, digit(px.charAt(k)));
        }
        head = line(r).split("\\s+");
        if (head.length != 3 || !head[0].equals("map"))
            throw new IOException("« map W H » attendu");
        int mw = Integer.parseInt(head[1]), mh = Integer.parseInt(head[2]);
        Plane[] planes = new Plane[2];
        for (int a = 0; a < 2; a++) {
            expect(r, a == 0 ? "planeA" : "planeB");
            Plane pl = new Plane(mw, mh);
            for (int y = 0; y < mh; y++) {
                String[] f = line(r).trim().split("\\s+");
                if (f.length != mw)
                    throw new IOException("plan : " + mw + " entrées attendues");
                for (int x = 0; x < mw; x++)
                    pl.set(x, y, Integer.parseInt(f[x], 16));
            }
            planes[a] = pl;
        }
        head = line(r).split("\\s+");
        if (head.length != 5 || !head[0].equals("heights"))
            throw new IOException("« heights W D originX originY » attendu");
        HeightMap h = new HeightMap(Integer.parseInt(head[1]), Integer.parseInt(head[2]),
            Integer.parseInt(head[3]), Integer.parseInt(head[4]));
        for (int pass = 0; pass < 2; pass++)
            for (int j = 0; j < h.d; j++) {
                String s = line(r).trim();
                if (s.length() != h.w)
                    throw new IOException("hauteurs : " + h.w + " chiffres attendus");
                for (int i = 0; i < h.w; i++) {
                    if (pass == 0)
                        h.setHeight(i, j, digit(s.charAt(i)));
                    else
                        h.setType(i, j, digit(s.charAt(i)));
                }
            }
        expect(r, "end");
        Project p = new Project(mw, mh, h);
        p.palette = pal;
        p.tiles = tiles;
        p.planeA = planes[0];
        p.planeB = planes[1];
        return p;
    }

    private static String line(BufferedReader r) throws IOException {
        String s = r.readLine();
        if (s == null)
            throw new IOException("fichier tronqué");
        return s;
    }

    private static void expect(BufferedReader r, String what) throws IOException {
        String s = line(r).trim();
        if (!s.equals(what))
            throw new IOException("« " + what + " » attendu, « " + s + " » trouvé");
    }

    private static int digit(char c) throws IOException {
        int d = Character.digit(c, 16);
        if (d < 0)
            throw new IOException("chiffre hexadécimal attendu : " + c);
        return d;
    }
}
