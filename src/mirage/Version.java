package mirage;

/** La version de Mirage : à mettre à jour avec chaque section de VERSION.md. */
public final class Version {
    private Version() {
    }

    public static final String NUMBER = "0.6";
    public static final String PATCH = "0006";

    public static String text() {
        return "Mirage " + NUMBER + " — patch " + PATCH;
    }
}
