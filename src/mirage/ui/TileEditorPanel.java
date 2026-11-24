package mirage.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import mirage.model.TileSet;

/**
 * L'éditeur d'un tile de 8 x 8, agrandi. Bouton gauche : la couleur
 * courante ; bouton droit (ou Ctrl-clic) : la couleur 0 (transparent). Le
 * tile 0 reste vide : il n'est pas modifiable.
 */
public final class TileEditorPanel extends JPanel {
    private static final int Z = 40;
    private final Editor ed;
    private final JLabel info = new JLabel();
    private byte[] clipboard;

    public TileEditorPanel(Editor ed) {
        this.ed = ed;
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createTitledBorder("Tile"));
        add(new Grid(), BorderLayout.CENTER);

        JPanel tools = new JPanel(new GridLayout(0, 2, 4, 4));
        ButtonGroup group = new ButtonGroup();
        Object[][] list = {{"Crayon", Editor.TileTool.PENCIL}, {"Ligne", Editor.TileTool.LINE},
            {"Rect", Editor.TileTool.RECT}, {"Rect plein", Editor.TileTool.FILLED_RECT},
            {"Remplir", Editor.TileTool.FILL}, {"Pipette", Editor.TileTool.PICK}};
        for (Object[] o : list) {
            JToggleButton b = new JToggleButton((String) o[0], o[1] == ed.tileTool);
            b.setFocusable(false);
            b.addActionListener(e -> ed.tileTool = (Editor.TileTool) o[1]);
            group.add(b);
            tools.add(b);
        }
        tools.add(action("↔ Miroir", () -> transform(px -> TileSet.flip(px, 1))));
        tools.add(action("↕ Miroir", () -> transform(px -> TileSet.flip(px, 2))));
        tools.add(action("⟳ Quart", () -> transform(TileSet::rotate)));
        tools.add(action("Effacer", () -> transform(px -> new byte[64])));
        tools.add(action("Copier", () -> clipboard = ed.project.tiles.pixels(ed.tile())));
        tools.add(action("Coller", () -> {
            if (clipboard != null)
                transform(px -> clipboard.clone());
        }));
        JPanel east = new JPanel(new BorderLayout());
        east.add(tools, BorderLayout.NORTH);
        east.add(info, BorderLayout.SOUTH);
        add(east, BorderLayout.EAST);
        ed.listen(this::refresh);
        refresh();
    }

    private JButton action(String name, Runnable r) {
        JButton b = new JButton(name);
        b.setFocusable(false);
        b.addActionListener(e -> r.run());
        return b;
    }

    private interface Op {
        byte[] apply(byte[] px);
    }

    private void transform(Op op) {
        int t = ed.tile();
        if (t == 0)
            return;
        ed.beginEdit();
        ed.project.tiles.setPixels(t, op.apply(ed.project.tiles.pixels(t)));
        ed.changed();
    }

    private void refresh() {
        int t = ed.tile();
        info.setText("<html>Tile " + t + String.format(" ($%03X)", t)
            + "<br>palette " + ed.line
            + (t == 0 ? "<br><i>tile 0 : toujours vide</i>" : "") + "</html>");
        repaint();
    }

    private final class Grid extends JPanel {
        private int x0 = -1, y0, x1, y1, button;
        private byte[] before;

        Grid() {
            setPreferredSize(new Dimension(8 * Z + 2, 8 * Z + 2));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int t = ed.tile();
                    if (t == 0)
                        return;
                    int x = e.getX() / Z, y = e.getY() / Z;
                    if (x > 7 || y > 7)
                        return;
                    boolean erase = SwingUtilities.isRightMouseButton(e) || e.isControlDown();
                    int c = erase ? 0 : ed.color;
                    switch (ed.tileTool) {
                    case PICK:
                        ed.color = ed.project.tiles.pixel(t, x, y);
                        ed.changed();
                        return;
                    case FILL:
                        ed.beginEdit();
                        fill(t, x, y, c);
                        ed.changed();
                        return;
                    default:
                        ed.beginEdit();
                        before = ed.project.tiles.pixels(t);
                        button = c;
                        x0 = x1 = x;
                        y0 = y1 = y;
                        if (ed.tileTool == Editor.TileTool.PENCIL)
                            ed.project.tiles.setPixel(t, x, y, c);
                        ed.changed();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (x0 < 0)
                        return;
                    int t = ed.tile();
                    x1 = Math.max(0, Math.min(7, e.getX() / Z));
                    y1 = Math.max(0, Math.min(7, e.getY() / Z));
                    if (ed.tileTool == Editor.TileTool.PENCIL) {
                        ed.project.tiles.setPixel(t, x1, y1, button);
                    } else { /* aperçu : depuis l'état d'avant */
                        ed.project.tiles.setPixels(t, before);
                        shape(t);
                    }
                    ed.changed();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    x0 = -1;
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private void shape(int t) {
            TileSet ts = ed.project.tiles;
            switch (ed.tileTool) {
            case LINE: {
                int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
                for (int k = 0; k <= n; k++) {
                    int x = n == 0 ? x0 : x0 + Math.round((float) (x1 - x0) * k / n);
                    int y = n == 0 ? y0 : y0 + Math.round((float) (y1 - y0) * k / n);
                    ts.setPixel(t, x, y, button);
                }
                break;
            }
            case RECT:
            case FILLED_RECT:
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                    for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                        boolean border = x == x0 || x == x1 || y == y0 || y == y1;
                        if (border || ed.tileTool == Editor.TileTool.FILLED_RECT)
                            ts.setPixel(t, x, y, button);
                    }
                break;
            default:
                break;
            }
        }

        private void fill(int t, int x, int y, int c) {
            TileSet ts = ed.project.tiles;
            int old = ts.pixel(t, x, y);
            if (old == c)
                return;
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[] {x, y});
            while (!q.isEmpty()) {
                int[] p = q.poll();
                if (p[0] < 0 || p[1] < 0 || p[0] > 7 || p[1] > 7 || ts.pixel(t, p[0], p[1]) != old)
                    continue;
                ts.setPixel(t, p[0], p[1], c);
                q.add(new int[] {p[0] + 1, p[1]});
                q.add(new int[] {p[0] - 1, p[1]});
                q.add(new int[] {p[0], p[1] + 1});
                q.add(new int[] {p[0], p[1] - 1});
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int t = ed.tile();
            for (int y = 0; y < 8; y++)
                for (int x = 0; x < 8; x++) {
                    int c = ed.project.tiles.pixel(t, x, y);
                    if (c == 0) { /* damier : transparent */
                        g.setColor(((x + y) & 1) == 0 ? new Color(70, 70, 70) : new Color(90, 90, 90));
                    } else {
                        g.setColor(new Color(ed.project.palette.rgb(ed.line, c)));
                    }
                    g.fillRect(x * Z, y * Z, Z, Z);
                }
            g.setColor(new Color(0, 0, 0, 90));
            for (int k = 0; k <= 8; k++) {
                g.drawLine(k * Z, 0, k * Z, 8 * Z);
                g.drawLine(0, k * Z, 8 * Z, k * Z);
            }
        }
    }
}
