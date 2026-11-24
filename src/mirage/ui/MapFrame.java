package mirage.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * La carte entière, en quasi plein écran, avec le fil de fer et le cadre de
 * l'écran. Un clic (ou un glissé) y déplace la vue de l'écran.
 */
public final class MapFrame extends JFrame {
    private final Editor ed;
    private final ScreenPanel screen;
    private BufferedImage both;
    private final Overview overview = new Overview();
    private int zoom = 1;

    public MapFrame(Editor ed, ScreenPanel screen) {
        super("Mirage — carte");
        this.ed = ed;
        this.screen = screen;
        JPanel holder = new JPanel(new java.awt.GridBagLayout()); /* carte centrée */
        holder.setBackground(java.awt.Color.DARK_GRAY);
        holder.add(overview);
        JScrollPane scroll = new JScrollPane(holder);
        add(scroll);
        setSize(1360, 840);
        /* le plus grand zoom entier qui tient en hauteur */
        zoom = Math.max(1, Math.min(3, 780 / (ed.project.mapHeight() * 8)));
        ed.listen(() -> {
            both = null;
            overview.repaint();
        });
        screen.onScroll(overview::repaint);
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyChar() >= '1' && e.getKeyChar() <= '3') {
                    zoom = e.getKeyChar() - '0';
                    overview.revalidate();
                    overview.repaint();
                }
            }
        });
    }

    private final class Overview extends JPanel {
        Overview() {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    screen.centerOn(e.getX() / zoom, e.getY() / zoom);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mousePressed(e);
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            setToolTipText("Clic : déplace l'écran ; touches 1, 2, 3 : zoom");
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(ed.project.mapWidth() * 8 * zoom, ed.project.mapHeight() * 8 * zoom);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            if (both == null)
                both = screen.render(new mirage.model.Render.Options()); /* les deux plans */
            BufferedImage im = both;
            g.drawImage(im, 0, 0, im.getWidth() * zoom, im.getHeight() * zoom, null);
            if (ed.showWire)
                ScreenPanel.drawWire(g, ed.project.heights, zoom);
            Rectangle r = screen.visibleMapRect();
            g.setColor(Color.WHITE);
            g.drawRect(r.x * zoom, r.y * zoom, r.width * zoom - 1, r.height * zoom - 1);
        }
    }
}
