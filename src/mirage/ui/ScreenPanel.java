package mirage.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import mirage.model.HeightMap;
import mirage.model.Plane;
import mirage.model.Render;
import mirage.model.Wire;

/**
 * La vue de l'écran : 320 x 224 pixels de la carte (au zoom choisi), qu'on
 * fait défiler. On y pose les tiles sur le plan A ou B, on règle leur bit de
 * priorité, on modifie les hauteurs de la 3D (fil de fer).
 */
public final class ScreenPanel extends JPanel {
    private final Editor ed;
    private final MapView view = new MapView();
    private final JScrollPane scroll = new JScrollPane(view);
    private final JLabel status = new JLabel(" ");
    private BufferedImage image;

    public ScreenPanel(Editor ed, Runnable openMap) {
        this.ed = ed;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Écran"));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        ButtonGroup group = new ButtonGroup();
        Object[][] list = {{"Poser", Editor.MapTool.PLACE}, {"Gommer", Editor.MapTool.ERASE},
            {"P on", Editor.MapTool.PRIO_ON}, {"P off", Editor.MapTool.PRIO_OFF},
            {"Hauteur", Editor.MapTool.HEIGHT}, {"Pipette", Editor.MapTool.PICK}};
        for (Object[] o : list) {
            JToggleButton b = new JToggleButton((String) o[0], o[1] == ed.mapTool);
            b.setFocusable(false);
            b.addActionListener(e -> {
                ed.mapTool = (Editor.MapTool) o[1];
                ed.changed();
            });
            group.add(b);
            tools.add(b);
        }
        tools.add(new JLabel("   Plan"));
        ButtonGroup planes = new ButtonGroup();
        JRadioButton a = new JRadioButton("A", ed.onA), b = new JRadioButton("B", !ed.onA);
        a.addActionListener(e -> ed.onA = true);
        b.addActionListener(e -> ed.onA = false);
        planes.add(a);
        planes.add(b);
        tools.add(a);
        tools.add(b);
        tools.add(check("↔", ed.hflip, v -> ed.hflip = v));
        tools.add(check("↕", ed.vflip, v -> ed.vflip = v));
        tools.add(check("Priorité", ed.priority, v -> ed.priority = v));

        JPanel shows = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        shows.add(new JLabel("Voir :"));
        shows.add(check("plan A", ed.showA, v -> ed.showA = v));
        shows.add(check("plan B", ed.showB, v -> ed.showB = v));
        shows.add(check("fil de fer", ed.showWire, v -> ed.showWire = v));
        shows.add(check("grille", ed.showGrid, v -> ed.showGrid = v));
        shows.add(check("priorités", ed.showPriority, v -> ed.showPriority = v));
        ButtonGroup zooms = new ButtonGroup();
        for (int z = 1; z <= 3; z++) {
            final int zz = z;
            JToggleButton zb = new JToggleButton("x" + z, z == ed.zoom);
            zb.setFocusable(false);
            zb.addActionListener(e -> setZoom(zz));
            zooms.add(zb);
            shows.add(zb);
        }
        JButton map = new JButton("Carte");
        map.setFocusable(false);
        map.addActionListener(e -> openMap.run());
        shows.add(map);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(tools);
        north.add(shows);
        add(north, BorderLayout.NORTH);
        scroll.setPreferredSize(new Dimension(640 + 4, 448 + 4));
        scroll.getHorizontalScrollBar().setUnitIncrement(8 * ed.zoom);
        scroll.getVerticalScrollBar().setUnitIncrement(8 * ed.zoom);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        ed.listen(() -> {
            image = null;
            view.revalidate();
            view.repaint();
        });
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
        view.scrollRectToVisible(new Rectangle(vx, vy, r.width, r.height));
        scroll.getViewport().setViewPosition(new Point(
            Math.min(vx, Math.max(0, view.getWidth() - r.width)),
            Math.min(vy, Math.max(0, view.getHeight() - r.height))));
    }

    /** La partie de la carte visible, en pixels de la carte. */
    public Rectangle visibleMapRect() {
        Rectangle r = scroll.getViewport().getViewRect();
        return new Rectangle(r.x / ed.zoom, r.y / ed.zoom, r.width / ed.zoom, r.height / ed.zoom);
    }

    public void onScroll(Runnable r) {
        scroll.getViewport().addChangeListener(e -> r.run());
    }

    BufferedImage image() {
        if (image == null) {
            Render.Options o = new Render.Options();
            o.showA = ed.showA;
            o.showB = ed.showB;
            o.showPriority = ed.showPriority;
            int w = ed.project.mapWidth() * 8, h = ed.project.mapHeight() * 8;
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, w, h, Render.map(ed.project, o), 0, w);
        }
        return image;
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
        private boolean dragging;

        MapView() {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    boolean back = SwingUtilities.isRightMouseButton(e) || e.isControlDown();
                    int mx = e.getX() / ed.zoom, my = e.getY() / ed.zoom;
                    switch (ed.mapTool) {
                    case HEIGHT: {
                        int[] c = Wire.pick(ed.project.heights, mx, my);
                        if (c == null)
                            return;
                        ed.beginEdit();
                        HeightMap h = ed.project.heights;
                        h.setHeight(c[0], c[1], h.height(c[0], c[1]) + (back ? -1 : 1));
                        ed.changed();
                        return;
                    }
                    case PICK: {
                        Plane p = ed.project.plane(ed.onA);
                        int tx = mx / 8, ty = my / 8;
                        if (!p.inside(tx, ty))
                            return;
                        int en = p.get(tx, ty);
                        ed.selStart = Plane.tile(en);
                        ed.selW = ed.selH = 1;
                        ed.line = Plane.line(en);
                        ed.changed();
                        return;
                    }
                    default:
                        ed.beginEdit();
                        dragging = true;
                        apply(mx / 8, my / 8, back);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    hover(e);
                    if (dragging)
                        apply(e.getX() / ed.zoom / 8, e.getY() / ed.zoom / 8,
                            SwingUtilities.isRightMouseButton(e) || e.isControlDown());
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

        private void hover(MouseEvent e) {
            hoverX = e.getX() / ed.zoom;
            hoverY = e.getY() / ed.zoom;
            int tx = hoverX / 8, ty = hoverY / 8;
            StringBuilder s = new StringBuilder("pixel " + hoverX + ", " + hoverY + "   case " + tx + ", " + ty);
            if (ed.project.planeA.inside(tx, ty)) {
                int ea = ed.project.planeA.get(tx, ty), eb = ed.project.planeB.get(tx, ty);
                s.append(String.format("   A : tile %d ligne %d%s   B : tile %d ligne %d%s",
                    Plane.tile(ea), Plane.line(ea), Plane.priority(ea) ? " P" : "",
                    Plane.tile(eb), Plane.line(eb), Plane.priority(eb) ? " P" : ""));
            }
            int[] c = Wire.pick(ed.project.heights, hoverX, hoverY);
            if (c != null)
                s.append("   3D : (" + c[0] + ", " + c[1] + ") hauteur "
                    + ed.project.heights.height(c[0], c[1]));
            status.setText(s.toString());
            repaint();
        }

        /** Applique l'outil à la case (tx, ty) du plan courant. */
        private void apply(int tx, int ty, boolean back) {
            Plane p = ed.project.plane(ed.onA);
            switch (ed.mapTool) {
            case PLACE:
                if (back) {
                    if (p.inside(tx, ty))
                        p.set(tx, ty, 0);
                    break;
                }
                for (int by = 0; by < ed.selH; by++)
                    for (int bx = 0; bx < ed.selW; bx++) {
                        int sx = ed.hflip ? ed.selW - 1 - bx : bx;
                        int sy = ed.vflip ? ed.selH - 1 - by : by;
                        int t = ed.blockTile(sx, sy);
                        if (t < 0 || !p.inside(tx + bx, ty + by))
                            continue;
                        p.set(tx + bx, ty + by, Plane.entry(t, ed.line, ed.hflip, ed.vflip, ed.priority));
                    }
                break;
            case ERASE:
                if (p.inside(tx, ty))
                    p.set(tx, ty, 0);
                break;
            case PRIO_ON:
            case PRIO_OFF:
                if (p.inside(tx, ty)) {
                    boolean on = (ed.mapTool == Editor.MapTool.PRIO_ON) != back;
                    int en = p.get(tx, ty);
                    p.set(tx, ty, on ? en | Plane.PRIORITY : en & ~Plane.PRIORITY);
                }
                break;
            default:
                break;
            }
            ed.changed();
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
            int z = ed.zoom;
            BufferedImage im = image();
            g.drawImage(im, 0, 0, im.getWidth() * z, im.getHeight() * z, null);
            if (ed.showGrid) {
                g.setColor(new Color(255, 255, 255, 40));
                for (int x = 0; x <= ed.project.mapWidth(); x++)
                    g.drawLine(x * 8 * z, 0, x * 8 * z, getHeight());
                for (int y = 0; y <= ed.project.mapHeight(); y++)
                    g.drawLine(0, y * 8 * z, getWidth(), y * 8 * z);
            }
            if (ed.showWire)
                drawWire(g, ed.project.heights, z);
            if (hoverX < 0)
                return;
            if (ed.mapTool == Editor.MapTool.HEIGHT) {
                int[] c = Wire.pick(ed.project.heights, hoverX, hoverY);
                if (c != null) {
                    HeightMap h = ed.project.heights;
                    int x = h.topX(c[0], c[1]), y = h.topY(c[0], c[1], h.height(c[0], c[1]));
                    Polygon d = new Polygon(new int[] {x * z, (x + 16) * z, x * z, (x - 16) * z},
                        new int[] {y * z, (y + 8) * z, (y + 16) * z, (y + 8) * z}, 4);
                    g.setColor(new Color(255, 80, 80, 120));
                    g.fill(d);
                }
                return;
            }
            int tx = hoverX / 8, ty = hoverY / 8;
            int w = ed.mapTool == Editor.MapTool.PLACE ? ed.selW : 1;
            int h = ed.mapTool == Editor.MapTool.PLACE ? ed.selH : 1;
            g.setColor(ed.onA ? new Color(80, 255, 120) : new Color(80, 180, 255));
            g.drawRect(tx * 8 * z, ty * 8 * z, w * 8 * z - 1, h * 8 * z - 1);
        }
    }
}
