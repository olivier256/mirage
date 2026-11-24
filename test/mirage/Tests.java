package mirage;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import mirage.model.DemoBuilder;
import mirage.model.Export;
import mirage.model.HeightMap;
import mirage.model.History;
import mirage.model.Palette;
import mirage.model.Plane;
import mirage.model.Project;
import mirage.model.ProjectIO;
import mirage.model.Render;
import mirage.model.TileSet;
import mirage.model.Wire;

/** Tests du modèle, sans bibliothèque : java mirage.Tests (sortie 1 si échec). */
public final class Tests {
    private static int run, failed;

    private static void check(boolean ok, String what) {
        run++;
        if (!ok) {
            failed++;
            System.out.println("ÉCHEC : " + what);
        }
    }

    public static void main(String[] args) throws Exception {
        palette();
        tiles();
        entries();
        render();
        wire();
        io();
        export();
        history();
        demo();
        System.out.println(run + " vérifications, " + failed + " échec(s)");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void palette() {
        Palette p = new Palette();
        p.set(1, 5, 7, 3, 9);
        check(p.level(1, 5, 2) == 7, "niveau borné à 7");
        check(p.cramWord(1, 5) == (7 << 9 | 3 << 5 | 7 << 1), "mot CRAM 0000BBB0GGG0RRR0");
        check(p.rgb(1, 5) == (0xFF000000 | 252 << 16 | 108 << 8 | 252), "RGB = niveau x 36");
        Palette q = new Palette();
        q.setCramWord(2, 3, p.cramWord(1, 5));
        check(q.cramWord(2, 3) == p.cramWord(1, 5), "aller-retour CRAM");
    }

    static void tiles() {
        byte[] px = new byte[64];
        px[0] = 1; /* (0,0) */
        px[7] = 2; /* (7,0) */
        check(TileSet.flip(px, 1)[7] == 1 && TileSet.flip(px, 1)[0] == 2, "miroir horizontal");
        check(TileSet.flip(px, 2)[56] == 1, "miroir vertical");
        byte[] r = TileSet.rotate(px);
        check(r[7] == 1 && r[63] == 2, "quart de tour horaire");
        check(java.util.Arrays.equals(TileSet.rotate(TileSet.rotate(TileSet.rotate(r))), px), "4 quarts = identité");
        TileSet t = new TileSet();
        check(t.count() == 1, "tile 0 vide d'office");
        int n = t.add(px);
        int[] f = t.find(TileSet.flip(px, 3));
        check(f != null && f[0] == n && f[1] == 3, "retrouvé avec retournements");
        t.setPixel(n, 1, 0, 0xA);
        byte[] packed = t.pack(n);
        check(packed.length == 32 && (packed[0] & 255) == 0x1A, "4 bpp, pixel de gauche en poids fort");
    }

    static void entries() {
        int e = Plane.entry(0x5A3, 2, true, false, true);
        check(e == (0x8000 | 2 << 13 | 0x0800 | 0x5A3), "bits de l'entrée de plan");
        check(Plane.tile(e) == 0x5A3 && Plane.line(e) == 2 && Plane.priority(e) && Plane.flips(e) == 1,
            "lecture des champs");
    }

    static void render() {
        Project p = new Project(2, 1, new HeightMap(1, 1, 0, 0));
        p.palette.set(0, 0, 0, 0, 1);
        p.palette.set(0, 1, 7, 0, 0);
        p.palette.set(1, 1, 0, 7, 0);
        byte[] full = new byte[64];
        java.util.Arrays.fill(full, (byte) 1);
        int t = p.tiles.add(full);
        /* case 0 : A sans priorité, B prioritaire -> B ; case 1 : A seul -> A */
        p.planeA.set(0, 0, Plane.entry(t, 0, false, false, false));
        p.planeB.set(0, 0, Plane.entry(t, 1, false, false, true));
        p.planeA.set(1, 0, Plane.entry(t, 0, false, false, false));
        int[] img = Render.map(p, new Render.Options());
        check(img[0] == p.palette.rgb(1, 1), "B haut passe devant A bas");
        check(img[8] == p.palette.rgb(0, 1), "A bas devant B vide");
        Render.Options o = new Render.Options();
        o.showA = false;
        o.showB = false;
        check(Render.map(p, o)[0] == p.palette.rgb(0, 0), "fond = palette 0 couleur 0");
    }

    static void wire() {
        HeightMap m = new HeightMap(4, 4, 100, 50);
        m.setHeight(1, 1, 2);
        check(m.topX(1, 1) == 100 && m.topY(1, 1, 2) == 50 + 16 - 32, "projection DDS520");
        int[] c = Wire.pick(m, m.topX(1, 1), m.topY(1, 1, 2) + 8);
        check(c != null && c[0] == 1 && c[1] == 1, "pick sur le dessus surélevé");
        check(Wire.pick(m, 0, 0) == null, "pick hors carte");
        List<int[]> s = Wire.segments(m);
        check(s.size() == 16 * 4 + 2 * 3, "4 côtés par losange + 2 faces de 3 segments");
        m.setHeight(0, 0, 99);
        check(m.height(0, 0) == HeightMap.MAX_HEIGHT, "hauteur bornée à 15");
    }

    static void io() throws Exception {
        Project p = DemoBuilder.build();
        p.planeA.set(3, 4, Plane.entry(7, 1, true, true, true));
        p.heights.setType(2, 3, 5);
        StringWriter w = new StringWriter();
        ProjectIO.write(p, w);
        Project q = ProjectIO.read(new StringReader(w.toString()));
        check(q.sameAs(p), "aller-retour du format texte");
        boolean refused = false;
        try {
            ProjectIO.read(new StringReader("pas un projet"));
        } catch (java.io.IOException e) {
            refused = true;
        }
        check(refused, "fichier invalide refusé");
    }

    static void export() throws Exception {
        Project p = DemoBuilder.build();
        check(Export.tiles(p).length == 32 * p.tiles.count(), "taille de tiles.bin");
        byte[] a = Export.plane(p.planeB);
        int e = p.planeB.get(0, 0);
        check(a.length == 2 * 64 * 64 && (a[0] & 255) == e >> 8 && (a[1] & 255) == (e & 255),
            "plan en gros-boutiste");
        check(Export.cram(p.palette).length == 128, "CRAM de 128 octets");
        Path dir = Files.createTempDirectory("mirage");
        Export.write(p, dir);
        for (String f : new String[] {"tiles.bin", "plane_a.bin", "plane_b.bin", "cram.bin", "heights.bin", "map.inc"})
            check(Files.exists(dir.resolve(f)), "export " + f);
        check(Files.readString(dir.resolve("map.inc")).contains("TILE_COUNT"), "map.inc");
    }

    static void history() {
        Project p = Project.empty();
        History h = new History();
        h.save(p);
        Project edited = p.copy();
        edited.palette.set(0, 2, 1, 2, 3);
        Project back = h.undo(edited);
        check(back.sameAs(p), "annuler");
        check(h.redo(back).sameAs(edited), "rétablir");
    }

    static void demo() {
        long t0 = System.nanoTime();
        Project p = DemoBuilder.build();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        check(p.tiles.count() <= TileSet.MAX, "démo : nombre de tiles");
        int used = 0;
        for (int y = 0; y < p.mapHeight(); y++)
            for (int x = 0; x < p.mapWidth(); x++)
                if (Plane.tile(p.planeB.get(x, y)) >= p.tiles.count())
                    used++;
        check(used == 0, "démo : entrées valides");
        System.out.println("démo : " + p.tiles.count() + " tiles en " + ms + " ms");
    }
}
