package mirage.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import mirage.model.Palette;

/**
 * Les 4 lignes de palette (16 couleurs). Un clic choisit la palette et la
 * couleur courantes ; les curseurs R, G, B (0 à 7) modifient la couleur.
 */
public final class PalettePanel extends JPanel {
    private static final int CELL = 26;
    private final Editor ed;
    private final JLabel[] values = new JLabel[3];
    private final JSlider[] sliders = new JSlider[3];
    private boolean refreshing, sliding;

    public PalettePanel(Editor ed) {
        this.ed = ed;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Palettes"));
        Swatches sw = new Swatches();
        add(sw, BorderLayout.CENTER);
        JPanel rgb = new JPanel(new java.awt.GridLayout(1, 3, 10, 0));
        rgb.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        String[] names = {"R", "G", "B"};
        for (int k = 0; k < 3; k++) {
            final int comp = k;
            sliders[k] = new JSlider(0, 7, 0);
            sliders[k].setMajorTickSpacing(1);
            sliders[k].setPaintTicks(true);
            sliders[k].setSnapToTicks(true);
            sliders[k].setFocusable(false);
            sliders[k].setPreferredSize(new Dimension(120, sliders[k].getPreferredSize().height));
            values[k] = new JLabel("0");
            values[k].setFont(values[k].getFont().deriveFont(Font.BOLD));
            sliders[k].addChangeListener(e -> slide(comp));
            JPanel one = new JPanel(new BorderLayout(4, 0));
            one.add(new JLabel(names[k]), BorderLayout.WEST);
            one.add(sliders[k], BorderLayout.CENTER);
            one.add(values[k], BorderLayout.EAST);
            rgb.add(one);
        }
        add(rgb, BorderLayout.SOUTH);
        ed.listen(this::refresh);
        refresh();
    }

    /**
     * Un glissé complet ne compte que pour un pas d'annulation : l'état est
     * mémorisé au premier mouvement, puis on attend que le curseur soit lâché.
     */
    private void slide(int comp) {
        if (refreshing)
            return;
        Palette p = ed.project.palette;
        int[] c = {p.level(ed.line, ed.color, 0), p.level(ed.line, ed.color, 1),
            p.level(ed.line, ed.color, 2)};
        int v = sliders[comp].getValue();
        if (v != c[comp]) {
            if (!sliding) {
                ed.beginEdit();
                sliding = true;
            }
            c[comp] = v;
            p.set(ed.line, ed.color, c[0], c[1], c[2]);
            ed.changed();
        }
        if (!sliders[comp].getValueIsAdjusting())
            sliding = false;
    }

    private void refresh() {
        refreshing = true;
        for (int k = 0; k < 3; k++) {
            int v = ed.project.palette.level(ed.line, ed.color, k);
            if (sliders[k].getValue() != v)
                sliders[k].setValue(v);
            values[k].setText(Integer.toString(v));
        }
        refreshing = false;
        repaint();
    }

    private final class Swatches extends JPanel {
        Swatches() {
            setPreferredSize(new Dimension(20 + 16 * CELL + 6, 18 + 4 * CELL + 6));
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
                g.setColor(getForeground());
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
            /* la palette choisie : un cadre rouge épais autour de la ligne */
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(220, 30, 30));
            g2.setStroke(new java.awt.BasicStroke(3f));
            g2.drawRect(1, 18 + ed.line * CELL - 1, 20 + 16 * CELL, CELL + 1);
            g2.setStroke(new java.awt.BasicStroke(1f));
            int x = 20 + ed.color * CELL, y = 18 + ed.line * CELL;
            g.setColor(Color.WHITE);
            g.drawRect(x, y, CELL - 1, CELL - 1);
            g.setColor(Color.BLACK);
            g.drawRect(x - 1, y - 1, CELL + 1, CELL + 1);
        }
    }
}
