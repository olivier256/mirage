package mirage.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.function.IntUnaryOperator;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import mirage.model.HeightMap;
import mirage.model.Plane;
import mirage.model.Render;
import mirage.model.TileSet;
import mirage.model.Wire;

/**
 * La tilemap : un onglet par plan (A ou B), qu'on voit et qu'on modifie
 * seul. On sélectionne des cases (rectangle à la souris, ⌘-clic pour en
 * ajouter ou en retirer) ; les champs de l'entrée, dans l'ordre des bits
 * pccvhnnn nnnnnnnn, s'appliquent à toute la sélection.
 */
public final class ScreenPanel extends JPanel {
    private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    private final Editor ed;
    private final MapView view = new MapView();
    private final JScrollPane scroll = new JScrollPane(view);
    private final JLabel status = new JLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel tabA = new JPanel(new BorderLayout()), tabB = new JPanel(new BorderLayout());
    private BufferedImage image;
    private boolean syncing;

    public ScreenPanel(Editor ed, Runnable openMap) {
        this.ed = ed;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Tilemap"));

        /* Rangée 1 : l'outil, le zoom, la carte, l'affichage */
        JPanel tools = row();
        ButtonGroup group = new ButtonGroup();
        Object[][] list = {{"Sélection", Editor.MapTool.SELECT}, {"Poser", Editor.MapTool.PLACE},
            {"Gommer", Editor.MapTool.ERASE}};
        for (Object[] o : list) {
            JToggleButton b = compact(new JToggleButton((String) o[0], o[1] == ed.mapTool));
            b.addActionListener(e -> {
                ed.mapTool = (Editor.MapTool) o[1];
                ed.changed();
            });
            group.add(b);
            tools.add(b);
        }
        tools.add(gap());
        ButtonGroup zooms = new ButtonGroup();
        for (int z = 1; z <= 3; z++) {
            final int zz = z;
            JToggleButton zb = compact(new JToggleButton("x" + z, z == ed.zoom));
            zb.addActionListener(e -> setZoom(zz));
            zooms.add(zb);
            tools.add(zb);
        }
        JButton map = compact(new JButton("Carte"));
        map.addActionListener(e -> openMap.run());
        tools.add(map);
        tools.add(gap());
        tools.add(check("grille", ed.showGrid, v -> ed.showGrid = v));
        tools.add(check("priorités", ed.showPriority, v -> ed.showPriority = v));

        /* Rangée 2 : l'entrée, dans l'ordre des bits pccvhnnn nnnnnnnn */
        JPanel entry = row();
        entry.setToolTipText("Entrée de la tilemap : pccvhnnn nnnnnnnn");
        JCheckBox prio = field(new JCheckBox("Priorité"));
        prio.addActionListener(e -> edit(en -> Plane.withPriority(en, prio.isSelected())));
        entry.add(prio);
        entry.add(new JLabel(" Palette"));
        JComboBox<String> palette = new JComboBox<>(new String[] {"0", "1", "2", "3"});
        palette.setFocusable(false);
        palette.addActionListener(e -> edit(en -> Plane.withLine(en, palette.getSelectedIndex())));
        entry.add(palette);
        JCheckBox vf = field(new JCheckBox("V-flip"));
        vf.addActionListener(e -> edit(en -> Plane.withVflip(en, vf.isSelected())));
        JCheckBox hf = field(new JCheckBox("H-flip"));
        hf.addActionListener(e -> edit(en -> Plane.withHflip(en, hf.isSelected())));
        entry.add(vf);
        entry.add(hf);
        entry.add(new JLabel(" Tile"));
        JTextField dec = new JTextField(5), hex = new JTextField(4);
        tileField(dec, 10);
        tileField(hex, 16);
        entry.add(dec);
        entry.add(new JLabel("$"));
        entry.add(hex);
        JLabel count = new JLabel();
        entry.add(count);

        /* Les champs suivent l'entrée en cours */
        ed.listen(() -> {
            syncing = true;
            int en = ed.entry;
            prio.setSelected(Plane.priority(en));
            palette.setSelectedIndex(Plane.line(en));
            vf.setSelected((Plane.flips(en) & 2) != 0);
            hf.setSelected((Plane.flips(en) & 1) != 0);
            dec.setText(Integer.toString(Plane.tile(en)));
            hex.setText(String.format("%03X", Plane.tile(en)));
            int n = ed.cells.cardinality();
            count.setText(n == 0 ? "   (aucune case)" : n == 1 ? "   (1 case)" : "   (" + n + " cases)");
            if (tabs.getSelectedIndex() != (ed.onA ? 0 : 1))
                tabs.setSelectedIndex(ed.onA ? 0 : 1);
            syncing = false;
        });

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(tools);
        north.add(entry);
        add(north, BorderLayout.NORTH);

        /* Les onglets : la vue passe dans l'onglet choisi */
        tabs.addTab("Plan A", tabA);
        tabs.addTab("Plan B", tabB);
        tabs.setSelectedIndex(ed.onA ? 0 : 1);
        (ed.onA ? tabA : tabB).add(scroll);
        tabs.addChangeListener(e -> {
            boolean a = tabs.getSelectedIndex() == 0;
            (a ? tabA : tabB).add(scroll);
            if (syncing || a == ed.onA)
                return;
            ed.onA = a;
            selectionChanged();
        });
        scroll.setPreferredSize(new Dimension(640 + 4, 448 + 4));
        scroll.getHorizontalScrollBar().setUnitIncrement(8 * ed.zoom);
        scroll.getVerticalScrollBar().setUnitIncrement(8 * ed.zoom);
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        ed.listen(() -> {
            image = null;
            view.revalidate();
            view.repaint();
        });
    }

    private void edit(IntUnaryOperator f) {
        if (!syncing)
            ed.editEntry(f);
    }

    /** Numéro de tile éditable en base 10 ou 16 (Entrée ou sortie du champ). */
    private void tileField(JTextField t, int radix) {
        t.setHorizontalAlignment(JTextField.RIGHT);
        Runnable apply = () -> {
            String s = t.getText().trim().replaceFirst("^(\\$|0[xX])", "");
            try {
                int v = Integer.parseInt(s, radix);
                if (v >= 0 && v < TileSet.MAX && v != Plane.tile(ed.entry)) {
                    edit(en -> Plane.withTile(en, v));
                    return;
                }
            } catch (NumberFormatException e) {
                /* valeur refusée : on réaffiche l'ancienne */
            }
            ed.changed();
        };
        t.addActionListener(e -> {
            apply.run();
            view.requestFocusInWindow(); /* ⌘C / ⌘V reviennent aux tiles */
        });
        t.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                apply.run();
            }
        });
    }

    /**
     * La sélection a changé : l'entrée en cours devient celle de la case de
     * référence. Avec l'outil Sélection, son tile est aussi choisi dans la
     * planche et ouvert dans l'éditeur de tile, dans sa palette.
     */
    private void selectionChanged() {
        Plane p = ed.project.plane(ed.onA);
        int i = view.anchor;
        if (i >= 0 && ed.cells.get(i)) {
            ed.entry = p.get(i % p.width, i / p.width);
            if (ed.mapTool == Editor.MapTool.SELECT) {
                ed.selStart = Plane.tile(ed.entry);
                ed.selW = ed.selH = 1;
                ed.line = Plane.line(ed.entry);
            }
        }
        ed.changed();
    }

    private static JPanel row() {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        r.setAlignmentX(LEFT_ALIGNMENT);
        return r;
    }

    private static <T extends AbstractButton> T compact(T b) {
        b.setFocusable(false);
        b.setMargin(new Insets(2, 6, 2, 6));
        return b;
    }

    private static <T extends AbstractButton> T field(T b) {
        b.setFocusable(false);
        return b;
    }

    /** Un séparateur vertical entre deux groupes. */
    private static JComponent gap() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(9, 20));
        return s;
    }

    private interface Setter {
        void set(boolean v);
    }

    private JCheckBox check(String name, boolean value, Setter s) {
        JCheckBox c = new JCheckBox(name, value);
        c.setFocusable(false);
        c.addActionListener(e -> {
            s.set(c.isSelected());
            ed.changed();
        });
        return c;
    }

    private void setZoom(int z) {
        Rectangle r = scroll.getViewport().getViewRect();
        int cx = (r.x + r.width / 2) / ed.zoom, cy = (r.y + r.height / 2) / ed.zoom;
        ed.zoom = z;
        scroll.getHorizontalScrollBar().setUnitIncrement(8 * z);
        scroll.getVerticalScrollBar().setUnitIncrement(8 * z);
        ed.changed();
        SwingUtilities.invokeLater(() -> centerOn(cx, cy));
    }

    /** Centre la vue sur le milieu de la zone 3D. */
    public void centerOnZone() {
        var h = ed.project.heights;
        centerOn(h.topX(h.w / 2, h.d / 2), h.topY(h.w / 2, h.d / 2, 0));
    }

    /** Centre la vue sur le point (x, y) de la carte (en pixels de la carte). */
    public void centerOn(int x, int y) {
        Rectangle r = scroll.getViewport().getViewRect();
        int vx = Math.max(0, x * ed.zoom - r.width / 2), vy = Math.max(0, y * ed.zoom - r.height / 2);
        scroll.getViewport().setViewPosition(new Point(
            Math.min(vx, Math.max(0, view.getWidth() - r.width)),
            Math.min(vy, Math.max(0, view.getHeight() - r.height))));
    }

    /** La partie de la carte visible, en pixels de la carte. */
    public Rectangle visibleMapRect() {
        Rectangle r = scroll.getViewport().getViewRect();
        return new Rectangle(r.x / ed.zoom, r.y / ed.zoom, r.width / ed.zoom, r.height / ed.zoom);
    }

    /** Donne le clavier à la vue (et non aux champs du numéro de tile). */
    public void focusView() {
        view.requestFocusInWindow();
    }

    public void onScroll(Runnable r) {
        scroll.getViewport().addChangeListener(e -> r.run());
    }

    private BufferedImage image() {
        if (image == null) {
            Render.Options o = new Render.Options();
            o.showA = ed.onA;
            o.showB = !ed.onA;
            o.showPriority = ed.showPriority;
            image = render(o);
        }
        return image;
    }

    BufferedImage render(Render.Options o) {
        int w = ed.project.mapWidth() * 8, h = ed.project.mapHeight() * 8;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        im.setRGB(0, 0, w, h, Render.map(ed.project, o), 0, w);
        return im;
    }

    /** Dessine le fil de fer (coordonnées de la carte, mises à l'échelle z). */
    static void drawWire(Graphics2D g, HeightMap m, double z) {
        g.setColor(new Color(255, 230, 80));
        g.setStroke(new BasicStroke(1f));
        for (int[] s : Wire.segments(m))
            g.drawLine((int) (s[0] * z), (int) (s[1] * z), (int) (s[2] * z), (int) (s[3] * z));
    }

    private final class MapView extends JPanel {
        private int hoverX = -1, hoverY = -1;
        /** Case de référence de la sélection, début du glissé, sélection de départ. */
        int anchor = -1;
        private int startX, startY;
        private BitSet base;
        private boolean toggle, dragging;

        MapView() {
            setFocusable(true);
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    Plane p = ed.project.plane(ed.onA);
                    int tx = e.getX() / ed.zoom / 8, ty = e.getY() / ed.zoom / 8;
                    if (!p.inside(tx, ty))
                        return;
                    startX = tx;
                    startY = ty;
                    switch (ed.mapTool) {
                    case SELECT:
                        toggle = (e.getModifiersEx() & MENU_MASK) != 0;
                        base = toggle ? (BitSet) ed.cells.clone() : new BitSet();
                        anchor = ty * p.width + tx;
                        dragging = true;
                        select(tx, ty);
                        break;
                    case PLACE:
                        ed.beginEdit();
                        dragging = true;
                        place(tx, ty);
                        break;
                    case ERASE:
                        ed.beginEdit();
                        dragging = true;
                        erase(tx, ty);
                        break;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    hover(e);
                    if (!dragging)
                        return;
                    Plane p = ed.project.plane(ed.onA);
                    int tx = Math.max(0, Math.min(p.width - 1, e.getX() / ed.zoom / 8));
                    int ty = Math.max(0, Math.min(p.height - 1, e.getY() / ed.zoom / 8));
                    switch (ed.mapTool) {
                    case SELECT:
                        select(tx, ty);
                        break;
                    case PLACE:
                        place(tx, ty);
                        break;
                    case ERASE:
                        erase(tx, ty);
                        break;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    hover(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverX = -1;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        /** Sélection : rectangle du début du glissé à (tx, ty) ; ⌘-clic inverse une case. */
        private void select(int tx, int ty) {
            Plane p = ed.project.plane(ed.onA);
            BitSet s = (BitSet) base.clone();
            if (toggle && tx == startX && ty == startY) {
                s.flip(ty * p.width + tx);
            } else {
                for (int y = Math.min(startY, ty); y <= Math.max(startY, ty); y++)
                    for (int x = Math.min(startX, tx); x <= Math.max(startX, tx); x++)
                        s.set(y * p.width + x);
            }
            ed.cells.clear();
            ed.cells.or(s);
            selectionChanged();
        }

        /** Pose le bloc de la planche en (tx, ty) avec les champs de l'entrée en cours. */
        private void place(int tx, int ty) {
            Plane p = ed.project.plane(ed.onA);
            int f = Plane.flips(ed.entry);
            ed.cells.clear();
            for (int by = 0; by < ed.selH; by++)
                for (int bx = 0; bx < ed.selW; bx++) {
                    int sx = (f & 1) != 0 ? ed.selW - 1 - bx : bx;
                    int sy = (f & 2) != 0 ? ed.selH - 1 - by : by;
                    int t = ed.blockTile(sx, sy);
                    if (t < 0 || !p.inside(tx + bx, ty + by))
                        continue;
                    p.set(tx + bx, ty + by, Plane.withTile(ed.entry, t));
                    ed.cells.set((ty + by) * p.width + tx + bx);
                }
            anchor = ty * p.width + tx;
            selectionChanged();
        }

        private void erase(int tx, int ty) {
            ed.project.plane(ed.onA).set(tx, ty, 0);
            ed.changed();
        }

        private void hover(MouseEvent e) {
            hoverX = e.getX() / ed.zoom;
            hoverY = e.getY() / ed.zoom;
            int tx = hoverX / 8, ty = hoverY / 8;
            Plane p = ed.project.plane(ed.onA);
            String s = "pixel " + hoverX + ", " + hoverY + "   case " + tx + ", " + ty;
            if (p.inside(tx, ty)) {
                int en = p.get(tx, ty);
                s += String.format("   %s : $%04X  tile %d ($%03X)  palette %d%s%s%s",
                    ed.onA ? "A" : "B", en, Plane.tile(en), Plane.tile(en), Plane.line(en),
                    Plane.priority(en) ? "  priorité" : "", (Plane.flips(en) & 2) != 0 ? "  V" : "",
                    (Plane.flips(en) & 1) != 0 ? "  H" : "");
            }
            status.setText(s);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(ed.project.mapWidth() * 8 * ed.zoom, ed.project.mapHeight() * 8 * ed.zoom);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int z = ed.zoom, c = 8 * z;
            BufferedImage im = image();
            g.drawImage(im, 0, 0, im.getWidth() * z, im.getHeight() * z, null);
            if (ed.showGrid) {
                g.setColor(new Color(255, 255, 255, 40));
                for (int x = 0; x <= ed.project.mapWidth(); x++)
                    g.drawLine(x * c, 0, x * c, getHeight());
                for (int y = 0; y <= ed.project.mapHeight(); y++)
                    g.drawLine(0, y * c, getWidth(), y * c);
            }
            /* sélection : voile et bord de chaque case */
            int w = ed.project.mapWidth();
            for (int i = ed.cells.nextSetBit(0); i >= 0; i = ed.cells.nextSetBit(i + 1)) {
                int x = i % w * c, y = i / w * c;
                g.setColor(new Color(80, 160, 255, 70));
                g.fillRect(x, y, c, c);
                g.setColor(new Color(120, 200, 255));
                g.drawRect(x, y, c - 1, c - 1);
            }
            if (hoverX < 0)
                return;
            int tx = hoverX / 8, ty = hoverY / 8;
            int bw = ed.mapTool == Editor.MapTool.PLACE ? ed.selW : 1;
            int bh = ed.mapTool == Editor.MapTool.PLACE ? ed.selH : 1;
            g.setColor(Color.WHITE);
            g.drawRect(tx * c, ty * c, bw * c - 1, bh * c - 1);
        }
    }
}
