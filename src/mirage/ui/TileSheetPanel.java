package mirage.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mirage.model.TileSet;

/**
 * La planche des tiles, 32 par rangée, dans la ligne de palette courante.
 * Un clic choisit un tile ; un glissé choisit un bloc, qu'on pose d'un coup
 * sur l'écran.
 */
public final class TileSheetPanel extends JPanel {
    private static final int Z = 3, CELL = 8 * Z + 1;
    private final Editor ed;
    private final Sheet sheet = new Sheet();
    private final JLabel info = new JLabel();

    public TileSheetPanel(Editor ed) {
        this.ed = ed;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Tiles"));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        bar.add(button("Nouveau", () -> {
            ed.beginEdit();
            int t = ed.project.tiles.add();
            if (t >= 0)
                select(t);
            ed.changed();
        }));
        bar.add(button("Dupliquer", () -> {
            ed.beginEdit();
            int t = ed.project.tiles.add(ed.project.tiles.pixels(ed.tile()));
            if (t >= 0)
                select(t);
            ed.changed();
        }));
        bar.add(info);
        add(bar, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(sheet);
        scroll.getVerticalScrollBar().setUnitIncrement(CELL);
        add(scroll, BorderLayout.CENTER);
        ed.listen(() -> {
            sheet.revalidate();
            sheet.repaint();
            info.setText(ed.project.tiles.count() + " tiles ; bloc de " + ed.selW + " x "
                + ed.selH + " à partir du tile " + ed.selStart);
        });
    }

    private void select(int t) {
        ed.selStart = t;
        ed.selW = ed.selH = 1;
    }

    private static JButton button(String name, Runnable r) {
        JButton b = new JButton(name);
        b.setFocusable(false);
        b.addActionListener(e -> r.run());
        return b;
    }

    private final class Sheet extends JPanel {
        private int ax = -1, ay;

        Sheet() {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ax = Math.min(Editor.SHEET_COLS - 1, e.getX() / CELL);
                    ay = e.getY() / CELL;
                    update(ax, ay);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (ax >= 0)
                        update(Math.max(0, Math.min(Editor.SHEET_COLS - 1, e.getX() / CELL)),
                            Math.max(0, e.getY() / CELL));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    ax = -1;
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private void update(int bx, int by) {
            int x0 = Math.min(ax, bx), y0 = Math.min(ay, by);
            int start = y0 * Editor.SHEET_COLS + x0;
            if (start >= ed.project.tiles.count())
                return;
            ed.selStart = start;
            ed.selW = Math.abs(bx - ax) + 1;
            ed.selH = Math.abs(by - ay) + 1;
            ed.changed();
        }

        @Override
        public Dimension getPreferredSize() {
            int rows = (ed.project.tiles.count() + Editor.SHEET_COLS - 1) / Editor.SHEET_COLS;
            return new Dimension(Editor.SHEET_COLS * CELL, Math.max(rows, 1) * CELL);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(40, 40, 40));
            g.fillRect(0, 0, getWidth(), getHeight());
            TileSet ts = ed.project.tiles;
            BufferedImage im = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            for (int t = 0; t < ts.count(); t++) {
                for (int y = 0; y < 8; y++)
                    for (int x = 0; x < 8; x++) {
                        int c = ts.pixel(t, x, y);
                        im.setRGB(x, y, c == 0 ? 0xFF202020 : ed.project.palette.rgb(ed.line, c));
                    }
                int x = (t % Editor.SHEET_COLS) * CELL, y = (t / Editor.SHEET_COLS) * CELL;
                g.drawImage(im, x, y, 8 * Z, 8 * Z, null);
            }
            int sx = (ed.selStart % Editor.SHEET_COLS) * CELL, sy = (ed.selStart / Editor.SHEET_COLS) * CELL;
            g.setColor(Color.YELLOW);
            g.drawRect(sx - 1, sy - 1, ed.selW * CELL, ed.selH * CELL);
        }
    }
}
