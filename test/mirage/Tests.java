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
        freeTiles();
        fields();
        asm();
        selection();
        clip();
        version();
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

    static void freeTiles() {
        TileSet t = new TileSet();
        check(t.pixel(1500, 3, 3) == 0 && t.isEmpty(1500), "tile jamais dessiné : vide");
        t.setPixel(1500, 3, 3, 9);
        check(t.count() == 1501 && t.pixel(1500, 3, 3) == 9, "dessiner crée le tile");
        check(t.firstEmptyAfter(0) == 1 && t.firstEmptyAfter(1499) == 1501, "premier tile libre");
    }

    static void fields() {
        int e = Plane.entry(5, 1, false, false, false);
        e = Plane.withPriority(e, true);
        e = Plane.withLine(e, 3);
        e = Plane.withVflip(e, true);
        e = Plane.withHflip(e, true);
        e = Plane.withTile(e, 0x7FF);
        check(e == 0xFFFF, "pccvhnnn nnnnnnnn : tous les champs");
        e = Plane.withLine(Plane.withVflip(e, false), 0);
        check(e == 0x8FFF, "champs retirés un à un");
        check(Plane.withTile(e, 0x800) == 0x8800, "numéro de tile sur 11 bits");
    }

    static void asm() throws Exception {
        Project p = Project.empty();
        p.tiles.setPixel(1, 0, 0, 0xA);
        p.tiles.setPixel(1, 7, 0, 3);
        p.planeB.set(2, 0, Plane.entry(40, 2, false, true, true));
        check(Export.tileCount(p) == 41, "tiles exportés jusqu'au plus grand référencé");
        String t = Export.tilesAsm(p);
        check(t.contains("; tile 1 ($001)\n\tdc.l\t$A0000003\n"), "tile en dc.l, un chiffre par pixel");
        check(Export.planeAsm("PlaneB", p.planeB).contains("\tdc.w\t$0000,$0000,$D028,"),
            "plan en dc.w");
        check(Export.cramAsm(p.palette).startsWith("; Exporté par Mirage"), "en-tête");
        Path dir = Files.createTempDirectory("mirage-s");
        Export.write(p, dir, Export.Format.ASM);
        for (String f : new String[] {"tiles.s", "plane_a.s", "plane_b.s", "cram.s", "heights.s", "map.inc"})
            check(Files.exists(dir.resolve(f)), "export " + f);
        check(!Files.exists(dir.resolve("tiles.bin")), "pas de .bin en export .s");
    }

    static void selection() {
        mirage.ui.Editor ed = new mirage.ui.Editor(Project.empty());
        ed.onA = true;
        ed.project.planeA.set(1, 1, Plane.entry(7, 0, false, false, false));
        ed.project.planeA.set(2, 1, Plane.entry(8, 1, true, false, false));
        ed.cells.set(1 * 64 + 1);
        ed.cells.set(1 * 64 + 2);
        ed.entry = ed.project.planeA.get(1, 1);
        ed.editEntry(e -> Plane.withLine(e, 2));
        check(Plane.line(ed.project.planeA.get(1, 1)) == 2 && Plane.line(ed.project.planeA.get(2, 1)) == 2,
            "palette changée sur toute la sélection");
        check(Plane.tile(ed.project.planeA.get(2, 1)) == 8 && Plane.flips(ed.project.planeA.get(2, 1)) == 1,
            "les autres champs ne bougent pas");
        check(ed.project.planeB.get(1, 1) == 0, "seul le plan de l'onglet change");
        ed.undo();
        check(Plane.line(ed.project.planeA.get(1, 1)) == 0, "annuler");
    }

    static void version() throws Exception {
        String last = null;
        for (String l : Files.readAllLines(Path.of("VERSION.md")))
            if (l.startsWith("## "))
                last = l;
        check(last != null && last.startsWith("## " + Version.NUMBER + " "),
            "Version.NUMBER = dernière section de VERSION.md (" + last + ")");
        check(Version.PATCH.equals(String.format("%04d", Integer.parseInt(Version.NUMBER.split("\\.")[1]))),
            "numéro de patch");
    }

    static void clip() {
        TileSet t = new TileSet();
        t.setPixel(5, 0, 0, 0xA);
        t.setPixel(6, 7, 7, 3);
        t.setPixel(5 + 32, 1, 0, 0xF);
        String text = mirage.model.TileClip.copy(t, 5, 2, 2);
        check(text.startsWith("; Mirage : bloc 2 x 2\n; tile 5 ($005)\n\tdc.l\t$A0000000\n"), "copie en .s");
        int[] wh = mirage.model.TileClip.paste(t, text, 100);
        check(wh != null && wh[0] == 2 && wh[1] == 2, "forme du bloc gardée");
        check(t.pixel(100, 0, 0) == 0xA && t.pixel(101, 7, 7) == 3 && t.pixel(132, 1, 0) == 0xF,
            "bloc collé au même dessin");
        check(mirage.model.TileClip.paste(t, "bonjour", 100) == null, "texte illisible refusé");
        /* un tile venu d'un source quelconque, avec commentaires et virgules */
        String src = "Gfx:  dc.l $11111111,$22222222 ; deux lignes\n"
            + " DC.L $33333333,$44444444,$55555555,$66666666,$77777777,$88888888\n";
        check(mirage.model.TileClip.paste(t, src, 0) != null && t.isEmpty(0), "le tile 0 reste vide");
        mirage.model.TileClip.paste(t, src, 200);
        check(t.pixel(200, 3, 0) == 1 && t.pixel(200, 0, 7) == 8, "dc.l de n'importe quel source");
        Project p = Project.empty();
        p.tiles.setPixel(9, 2, 2, 7);
        check(mirage.model.TileClip.copy(p.tiles, 9, 1, 1).contains(Export.tilesAsm(p).split("; tile 9 \\(\\$009\\)\n")[1].substring(0, 80)),
            "même texte que l'export .s");
    }
}
