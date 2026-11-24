package mirage.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import mirage.model.Palette;

/**
 * Les 4 lignes de palette (16 couleurs). Un clic choisit la ligne et la
 * couleur courantes ; R, G, B (0 à 7) modifient la couleur choisie.
 */
public final class PalettePanel extends JPanel {
    private static final int CELL = 26;
    private final Editor ed;
    private final JLabel[] values = new JLabel[3];

    public PalettePanel(Editor ed) {
        this.ed = ed;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Palettes"));
        Swatches sw = new Swatches();
        add(sw, BorderLayout.CENTER);
        JPanel rgb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        String[] names = {"R", "G", "B"};
        for (int k = 0; k < 3; k++) {
            final int comp = k;
            rgb.add(new JLabel(names[k]));
            JButton minus = small("−"), plus = small("+");
            values[k] = new JLabel("0");
            values[k].setFont(values[k].getFont().deriveFont(Font.BOLD));
            minus.addActionListener(e -> bump(comp, -1));
            plus.addActionListener(e -> bump(comp, +1));
            rgb.add(minus);
            rgb.add(values[k]);
            rgb.add(plus);
        }
        add(rgb, BorderLayout.SOUTH);
        ed.listen(this::refresh);
        refresh();
    }

    private static JButton small(String s) {
        JButton b = new JButton(s);
        b.setMargin(new java.awt.Insets(0, 6, 0, 6));
        b.setFocusable(false);
        return b;
    }

    private void bump(int comp, int d) {
        Palette p = ed.project.palette;
        int[] c = {p.level(ed.line, ed.color, 0), p.level(ed.line, ed.color, 1),
            p.level(ed.line, ed.color, 2)};
        int v = c[comp] + d;
        if (v < 0 || v > 7)
            return;
        ed.beginEdit();
        c[comp] = v;
        ed.project.palette.set(ed.line, ed.color, c[0], c[1], c[2]);
        ed.changed();
    }

    private void refresh() {
        for (int k = 0; k < 3; k++)
            values[k].setText(Integer.toString(ed.project.palette.level(ed.line, ed.color, k)));
        repaint();
    }

    private final class Swatches extends JPanel {
        Swatches() {
            setPreferredSize(new Dimension(20 + 16 * CELL + 4, 18 + 4 * CELL + 4));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int c = (e.getX() - 20) / CELL, l = (e.getY() - 18) / CELL;
                    if (e.getX() >= 20 && e.getY() >= 18 && c < 16 && l < 4) {
                        ed.line = l;
                        ed.color = c;
                        ed.changed();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(getForeground());
            for (int c = 0; c < 16; c++)
                g.drawString(Integer.toHexString(c).toUpperCase(), 20 + c * CELL + 9, 13);
            for (int l = 0; l < 4; l++) {
                g.drawString(Integer.toString(l), 4, 18 + l * CELL + 18);
                for (int c = 0; c < 16; c++) {
                    int x = 20 + c * CELL, y = 18 + l * CELL;
                    g.setColor(new Color(ed.project.palette.rgb(l, c)));
                    g.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                    if (c == 0) { /* transparent */
                        g.setColor(Color.GRAY);
                        g.drawLine(x + 3, y + CELL - 4, x + CELL - 4, y + 3);
                    }
                }
            }
            int x = 20 + ed.color * CELL, y = 18 + ed.line * CELL;
            g.setColor(Color.WHITE);
            g.drawRect(x, y, CELL - 1, CELL - 1);
            g.setColor(Color.BLACK);
            g.drawRect(x - 1, y - 1, CELL + 1, CELL + 1);
        }
    }
}
