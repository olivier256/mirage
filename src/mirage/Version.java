package mirage;

/** La version de Mirage : à mettre à jour avec chaque section de VERSION.md. */
public final class Version {
    private Version() {
    }

    public static final String NUMBER = "0.5";
    public static final String PATCH = "0005";

    public static String text() {
        return "Mirage " + NUMBER + " — patch " + PATCH;
    }
}
