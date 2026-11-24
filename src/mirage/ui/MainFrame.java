package mirage.ui;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import mirage.model.DemoBuilder;
import mirage.model.Export;
import mirage.model.Project;
import mirage.model.ProjectIO;

/**
 * La fenêtre principale, pensée pour 1440 x 900 : à gauche les palettes et
 * le tile en cours ; à droite la planche des tiles et l'écran.
 */
public final class MainFrame extends JFrame {
    private final Editor ed;
    private final ScreenPanel screen;
    private MapFrame map;

    public MainFrame(Editor ed) {
        super("Mirage");
        this.ed = ed;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.add(new PalettePanel(ed), BorderLayout.NORTH);
        left.add(new TileEditorPanel(ed), BorderLayout.CENTER);

        screen = new ScreenPanel(ed, this::openMap);
        JPanel right = new JPanel(new BorderLayout());
        TileSheetPanel sheet = new TileSheetPanel(ed);
        sheet.setPreferredSize(new java.awt.Dimension(820, 260));
        right.add(sheet, BorderLayout.NORTH);
        right.add(screen, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(6, 0));
        root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        setJMenuBar(menus());
        ed.listen(this::title);
        title();
        setSize(1440, 870);
        setLocationRelativeTo(null);
    }

    private void load(Project p, Path f) {
        ed.replace(p, f);
        showZone();
    }

    /** À appeler une fois la fenêtre affichée. */
    public void showZone() {
        javax.swing.SwingUtilities.invokeLater(screen::centerOnZone);
    }

    private JMenuBar menus() {
        int cmd = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Fichier");
        file.add(item("Nouveau (vide)", KeyStroke.getKeyStroke(KeyEvent.VK_N, cmd),
            () -> { if (confirmLoss()) load(Project.empty(), null); }));
        file.add(item("Nouveau (démo)", null,
            () -> { if (confirmLoss()) load(DemoBuilder.build(), null); }));
        file.add(item("Ouvrir…", KeyStroke.getKeyStroke(KeyEvent.VK_O, cmd), this::open));
        file.add(item("Enregistrer", KeyStroke.getKeyStroke(KeyEvent.VK_S, cmd), () -> save(false)));
        file.add(item("Enregistrer sous…", KeyStroke.getKeyStroke(KeyEvent.VK_S,
            cmd | java.awt.event.InputEvent.SHIFT_DOWN_MASK), () -> save(true)));
        file.addSeparator();
        file.add(item("Exporter pour la Mega Drive (.bin)…", KeyStroke.getKeyStroke(KeyEvent.VK_E, cmd),
            () -> export(Export.Format.BIN)));
        file.add(item("Exporter pour la Mega Drive (.s)…", KeyStroke.getKeyStroke(KeyEvent.VK_E,
            cmd | java.awt.event.InputEvent.SHIFT_DOWN_MASK), () -> export(Export.Format.ASM)));
        file.addSeparator();
        file.add(item("Quitter", KeyStroke.getKeyStroke(KeyEvent.VK_Q, cmd), this::quit));
        bar.add(file);
        JMenu edit = new JMenu("Édition");
        edit.add(item("Annuler", KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmd), ed::undo));
        edit.add(item("Rétablir", KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            cmd | java.awt.event.InputEvent.SHIFT_DOWN_MASK), ed::redo));
        bar.add(edit);
        JMenu view = new JMenu("Affichage");
        view.add(item("Carte", KeyStroke.getKeyStroke(KeyEvent.VK_M, cmd), this::openMap));
        bar.add(view);
        JMenu help = new JMenu("Aide");
        help.add(item("À propos de Mirage", null, this::about));
        bar.add(help);
        /* sur Mac, le menu « Mirage > À propos » */
        if (java.awt.Desktop.isDesktopSupported()
            && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.APP_ABOUT))
            java.awt.Desktop.getDesktop().setAboutHandler(e -> about());
        return bar;
    }

    private static JMenuItem item(String name, KeyStroke key, Runnable r) {
        JMenuItem it = new JMenuItem(name);
        if (key != null)
            it.setAccelerator(key);
        it.addActionListener(e -> r.run());
        return it;
    }

    private void title() {
        String name = ed.file == null ? "sans titre" : ed.file.getFileName().toString();
        setTitle("Mirage " + mirage.Version.NUMBER + " — " + name + (ed.dirty ? " *" : ""));
    }

    private void openMap() {
        if (map == null)
            map = new MapFrame(ed, screen);
        map.setVisible(true);
        map.toFront();
    }

    private boolean confirmLoss() {
        if (!ed.dirty)
            return true;
        return JOptionPane.showConfirmDialog(this, "Abandonner les modifications non enregistrées ?",
            "Mirage", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private void open() {
        if (!confirmLoss())
            return;
        FileDialog d = new FileDialog(this, "Ouvrir un projet", FileDialog.LOAD);
        d.setFilenameFilter((dir, n) -> n.endsWith(".mirage"));
        d.setVisible(true);
        if (d.getFile() == null)
            return;
        Path p = new File(d.getDirectory(), d.getFile()).toPath();
        try {
            load(ProjectIO.load(p), p);
        } catch (IOException | RuntimeException e) {
            error("Impossible d'ouvrir " + p.getFileName() + " :\n" + e.getMessage());
        }
    }

    private void save(boolean as) {
        Path p = ed.file;
        if (as || p == null) {
            FileDialog d = new FileDialog(this, "Enregistrer le projet", FileDialog.SAVE);
            d.setFile(p == null ? "zone.mirage" : p.getFileName().toString());
            d.setVisible(true);
            if (d.getFile() == null)
                return;
            String n = d.getFile().endsWith(".mirage") ? d.getFile() : d.getFile() + ".mirage";
            p = new File(d.getDirectory(), n).toPath();
        }
        try {
            ProjectIO.save(ed.project, p);
            ed.file = p;
            ed.dirty = false;
            ed.changed();
        } catch (IOException e) {
            error("Impossible d'enregistrer :\n" + e.getMessage());
        }
    }

    private void about() {
        JOptionPane.showMessageDialog(this, "<html><b>" + mirage.Version.text() + "</b><br><br>"
            + "Éditeur de zones isométriques pour la Mega Drive,<br>"
            + "dans l'esprit de Mirage Type III (Landstalker).<br><br>"
            + "Java " + System.getProperty("java.version") + "</html>",
            "À propos de Mirage", JOptionPane.INFORMATION_MESSAGE);
    }

    private void export(Export.Format f) {
        JFileChooser c = new JFileChooser();
        c.setDialogTitle(f == Export.Format.BIN ? "Dossier d'export (tiles.bin, plane_a.bin...)"
            : "Dossier d'export (tiles.s, plane_a.s...)");
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            Export.write(ed.project, c.getSelectedFile().toPath(), f);
            JOptionPane.showMessageDialog(this, "Exporté dans " + c.getSelectedFile());
        } catch (IOException e) {
            error("Impossible d'exporter :\n" + e.getMessage());
        }
    }

    private void quit() {
        if (confirmLoss())
            System.exit(0);
    }

    private void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Mirage", JOptionPane.ERROR_MESSAGE);
    }
}
