package mirage.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Les tiles : des carrés de 8 x 8 pixels, 4 bits par pixel (un index de 0 à
 * 15 dans une ligne de palette ; 0 = transparent). La ligne de palette n'est
 * pas dans le tile : elle est dans l'entrée de la table du plan. Le tile 0 est
 * vide par convention. Au plus 2048 tiles (11 bits dans une entrée) ; la VRAM
 * de la Mega Drive en tient 2048 au mieux, moins les tables.
 */
public final class TileSet {
    public static final int MAX = 2048;
    public static final int SIZE = 8;

    private final List<byte[]> tiles = new ArrayList<>();

    public TileSet() {
        tiles.add(new byte[64]);
    }

    public int count() {
        return tiles.size();
    }

    public int pixel(int tile, int x, int y) {
        return tiles.get(tile)[y * 8 + x];
    }

    public void setPixel(int tile, int x, int y, int color) {
        tiles.get(tile)[y * 8 + x] = (byte) (color & 15);
    }

    public byte[] pixels(int tile) {
        return tiles.get(tile).clone();
    }

    public void setPixels(int tile, byte[] px) {
        tiles.set(tile, px.clone());
    }

    /** Ajoute un tile vide ; renvoie son numéro, -1 si c'est plein. */
    public int add() {
        if (tiles.size() >= MAX)
            return -1;
        tiles.add(new byte[64]);
        return tiles.size() - 1;
    }

    public int add(byte[] px) {
        int t = add();
        if (t >= 0)
            setPixels(t, px);
        return t;
    }

    /** S'assure qu'il existe au moins n tiles. */
    public void ensure(int n) {
        while (tiles.size() < Math.min(n, MAX))
            tiles.add(new byte[64]);
    }

    /** Pixels retournés : bit 0 horizontal, bit 1 vertical. */
    public static byte[] flip(byte[] px, int flips) {
        byte[] out = new byte[64];
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++) {
                int sx = (flips & 1) != 0 ? 7 - x : x;
                int sy = (flips & 2) != 0 ? 7 - y : y;
                out[y * 8 + x] = px[sy * 8 + sx];
            }
        return out;
    }

    /** Tourne le tile d'un quart de tour (sens des aiguilles d'une montre). */
    public static byte[] rotate(byte[] px) {
        byte[] out = new byte[64];
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                out[y * 8 + x] = px[(7 - x) * 8 + y];
        return out;
    }

    /** Cherche un tile identique (retournements compris) : {numéro, flips} ou null. */
    public int[] find(byte[] px) {
        for (int f = 0; f < 4; f++) {
            byte[] o = flip(px, f);
            for (int t = 0; t < tiles.size(); t++)
                if (Arrays.equals(tiles.get(t), o))
                    return new int[] {t, f};
        }
        return null;
    }

    /** 32 octets, 4 bits par pixel, pixel de gauche dans les bits de poids fort. */
    public byte[] pack(int tile) {
        byte[] px = tiles.get(tile), out = new byte[32];
        for (int n = 0; n < 32; n++)
            out[n] = (byte) (px[2 * n] << 4 | px[2 * n + 1]);
        return out;
    }

    public TileSet copy() {
        TileSet t = new TileSet();
        t.tiles.clear();
        for (byte[] px : tiles)
            t.tiles.add(px.clone());
        return t;
    }

    public boolean sameAs(TileSet o) {
        if (o.tiles.size() != tiles.size())
            return false;
        for (int i = 0; i < tiles.size(); i++)
            if (!Arrays.equals(tiles.get(i), o.tiles.get(i)))
                return false;
        return true;
    }
}
