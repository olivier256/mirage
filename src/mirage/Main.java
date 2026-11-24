package mirage;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import mirage.model.DemoBuilder;
import mirage.model.Project;
import mirage.model.ProjectIO;
import mirage.ui.Editor;
import mirage.ui.MainFrame;

/**
 * Mirage : un éditeur de zones isométriques pour la Mega Drive, dans
 * l'esprit de « Mirage Type III » (Landstalker).
 *
 * Usage : java -jar mirage.jar [ZONE.mirage]
 * Sans fichier : le projet de démonstration.
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Mirage");
        Project p;
        Path file = null;
        if (args.length > 0) {
            file = Path.of(args[0]);
            p = ProjectIO.load(file);
        } else {
            p = DemoBuilder.build();
        }
        final Project project = p;
        final Path f = file;
        SwingUtilities.invokeLater(() -> {
            Editor ed = new Editor(project);
            ed.file = f;
            MainFrame frame = new MainFrame(ed);
            frame.setVisible(true);
            ed.changed();
            frame.showZone();
        });
    }
}
