package mirage.model;

/**
 * Les 4 lignes de 16 couleurs de la Mega Drive (la CRAM). Une couleur : trois
 * niveaux de 0 à 7 (rouge, vert, bleu), soit 512 couleurs possibles. La
 * couleur 0 d'une ligne est transparente dans les tiles ; la couleur 0 de la
 * ligne 0 sert de fond (registre 7 du VDP à 0).
 */
public final class Palette {
    public static final int LINES = 4;
    public static final int COLORS = 16;

    /** Niveaux par composante : [ligne][couleur][0 = R, 1 = G, 2 = B]. */
    private final int[][][] level = new int[LINES][COLORS][3];

    public int level(int line, int color, int component) {
        return level[line][color][component];
    }

    public void set(int line, int color, int r, int g, int b) {
        level[line][color][0] = clamp(r);
        level[line][color][1] = clamp(g);
        level[line][color][2] = clamp(b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(7, v));
    }

    /** Couleur 24 bits pour l'écran : niveau x 36 (0 à 252). */
    public int rgb(int line, int color) {
        int[] c = level[line][color];
        return 0xFF000000 | (c[0] * 36) << 16 | (c[1] * 36) << 8 | c[2] * 36;
    }

    /** Le mot de la CRAM : 0000 BBB0 GGG0 RRR0. */
    public int cramWord(int line, int color) {
        int[] c = level[line][color];
        return c[2] << 9 | c[1] << 5 | c[0] << 1;
    }

    public void setCramWord(int line, int color, int word) {
        set(line, color, (word >> 1) & 7, (word >> 5) & 7, (word >> 9) & 7);
    }

    public Palette copy() {
        Palette p = new Palette();
        for (int l = 0; l < LINES; l++)
            for (int c = 0; c < COLORS; c++)
                p.level[l][c] = level[l][c].clone();
        return p;
    }

    public boolean sameAs(Palette o) {
        for (int l = 0; l < LINES; l++)
            for (int c = 0; c < COLORS; c++)
                for (int k = 0; k < 3; k++)
                    if (level[l][c][k] != o.level[l][c][k])
                        return false;
        return true;
    }
}
