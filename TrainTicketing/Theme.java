import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ListCellRenderer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;


public final class Theme {
    private Theme() {}

    // ---------------- colour helpers ----------------
    static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static double luma(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    // ---------------- install ----------------
    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}

        FontUIResource body = new FontUIResource(Main.font(Font.PLAIN, 14));
        for (String k : new String[]{"Label.font", "TextField.font", "PasswordField.font", "TextArea.font",
                "ComboBox.font", "CheckBox.font", "List.font", "ToolTip.font", "OptionPane.messageFont"})
            UIManager.put(k, body);
        FontUIResource bold = new FontUIResource(Main.font(Font.BOLD, 14));
        UIManager.put("Button.font", bold);
        UIManager.put("OptionPane.buttonFont", bold);

        ColorUIResource text = new ColorUIResource(Main.TEXT), white = new ColorUIResource(Color.WHITE),
                bg = new ColorUIResource(Main.BACKGROUND), sel = new ColorUIResource(Main.SELECTION),
                muted = new ColorUIResource(Main.MUTED);

        UIManager.put("Panel.background", bg);
        UIManager.put("OptionPane.background", bg);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("OptionPane.border", new BorderUIResource.EmptyBorderUIResource(22, 26, 16, 26));
        UIManager.put("Label.foreground", text);
        UIManager.put("CheckBox.background", bg);
        UIManager.put("CheckBox.foreground", text);
        UIManager.put("CheckBox.icon", new IconUIResource(new CheckIcon()));

        for (String f : new String[]{"TextField", "PasswordField", "TextArea"}) {
            UIManager.put(f + ".background", white);
            UIManager.put(f + ".foreground", text);
            UIManager.put(f + ".caretForeground", text);
            UIManager.put(f + ".selectionBackground", sel);
            UIManager.put(f + ".selectionForeground", text);
            UIManager.put(f + ".inactiveBackground", new ColorUIResource(Main.GRAY));
        }
        RoundedBorder fieldBorder = new RoundedBorder(Main.LINE, 10, new Insets(8, 12, 8, 12));
        UIManager.put("TextField.border", fieldBorder);
        UIManager.put("PasswordField.border", fieldBorder);
        UIManager.put("TextArea.border", new BorderUIResource.EmptyBorderUIResource(8, 10, 8, 10));
        UIManager.put("ScrollPane.border", new RoundedBorder(Main.LINE, 8, new Insets(1, 1, 1, 1)));

        UIManager.put("ComboBox.background", white);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.selectionBackground", sel);
        UIManager.put("ComboBox.selectionForeground", text);
        UIManager.put("ComboBox.disabledBackground", new ColorUIResource(Main.GRAY));
        UIManager.put("ComboBox.disabledForeground", muted);
        UIManager.put("ComboBox.border", new RoundedBorder(Main.LINE, 10, new Insets(2, 2, 2, 2)));

        UIManager.put("Button.background", new ColorUIResource(Main.GRAY));
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.disabledText", muted);
        UIManager.put("Button.margin", new InsetsUIResource(0, 0, 0, 0));
        UIManager.put("Button.border", new BorderUIResource.EmptyBorderUIResource(10, 20, 10, 20));

        UIManager.put("TabbedPane.background", white);
        UIManager.put("TabbedPane.foreground", muted);
        UIManager.put("TabbedPane.selectedForeground", new ColorUIResource(Main.GREEN));

        UIManager.put("ToolTip.background", new ColorUIResource(Main.DARK_GREEN));
        UIManager.put("ToolTip.foreground", white);
        UIManager.put("ToolTip.border", new BorderUIResource.EmptyBorderUIResource(6, 10, 6, 10));

        UIManager.put("OptionPane.informationIcon", new IconUIResource(new GlyphIcon(Main.GREEN, Color.WHITE, "i")));
        UIManager.put("OptionPane.questionIcon", new IconUIResource(new GlyphIcon(Main.DARK_GREEN, Color.WHITE, "?")));
        UIManager.put("OptionPane.warningIcon", new IconUIResource(new GlyphIcon(Main.YELLOW, Main.TEXT, "!")));
        UIManager.put("OptionPane.errorIcon", new IconUIResource(new GlyphIcon(Main.RED, Color.WHITE, "\u00D7")));

        UIManager.put("ButtonUI", ModernButtonUI.class.getName());
        UIManager.put("ComboBoxUI", ModernComboBoxUI.class.getName());
        UIManager.put("ScrollBarUI", ModernScrollBarUI.class.getName());
        UIManager.put("TabbedPaneUI", ModernTabbedPaneUI.class.getName());

        // Fields and combo boxes draw a green outline while focused, so repaint them when focus moves.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", e -> {
            for (Object o : new Object[]{e.getOldValue(), e.getNewValue()})
                if (o instanceof JComponent) ((JComponent) o).repaint();
        });
    }

    // ---------------- header badge ----------------
    public static JLabel badge(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setBackground(Main.YELLOW);
        l.setForeground(Main.DARK_GREEN);
        l.setFont(Main.font(Font.BOLD, 20));
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        return l;
    }

    // ---------------- rounded card panel ----------------
    public static class Card extends JPanel {
        private final Color fill, line;
        private final int arc;

        public Card(LayoutManager lm) { this(lm, Color.WHITE, Main.LINE, 14); }

        public Card(LayoutManager lm, Color fill, Color line, int arc) {
            super(lm);
            this.fill = fill; this.line = line; this.arc = arc;
            setOpaque(false);
            setBackground(fill);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
        }

        @Override protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.clip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
            super.paintChildren(g2);
            g2.dispose();
        }

        @Override public void paint(Graphics g) {
            super.paint(g);
            if (line == null) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }

    // ---------------- rounded border (text fields, combo boxes, scroll panes) ----------------
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int arc;
        private final Insets pad;

        public RoundedBorder(Color color, int arc, Insets pad) { this.color = color; this.arc = arc; this.pad = pad; }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = smooth(g);
            // Hide the square corners of the component's own background behind the curve.
            Component parent = c.getParent();
            Color pb = parent != null ? parent.getBackground() : Main.BACKGROUND;
            if (pb.getAlpha() == 255) {
                Area corners = new Area(new Rectangle2D.Double(x, y, w, h));
                corners.subtract(new Area(new RoundRectangle2D.Double(x, y, w, h, arc, arc)));
                g2.setColor(pb);
                g2.fill(corners);
            }
            boolean focus = c.isEnabled() && c.hasFocus();
            double o = focus ? 1.0 : 0.5;
            g2.setColor(focus ? Main.GREEN : color);
            g2.setStroke(new BasicStroke(focus ? 2f : 1f));
            g2.draw(new RoundRectangle2D.Double(x + o, y + o, w - 2 * o, h - 2 * o, arc, arc));
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return (Insets) pad.clone(); }

        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(pad.top, pad.left, pad.bottom, pad.right);
            return i;
        }
    }

    // ---------------- icons ----------------
    public static class GlyphIcon implements Icon {
        private final Color fill, ink;
        private final String glyph;

        GlyphIcon(Color fill, Color ink, String glyph) { this.fill = fill; this.ink = ink; this.glyph = glyph; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = smooth(g);
            g2.setColor(fill);
            g2.fillOval(x, y, 38, 38);
            g2.setColor(ink);
            g2.setFont(Main.font(Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(glyph, x + (38 - fm.stringWidth(glyph)) / 2, y + (38 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }

        @Override public int getIconWidth() { return 38; }
        @Override public int getIconHeight() { return 38; }
    }

    public static class CheckIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = smooth(g);
            boolean on = b.isSelected();
            g2.setColor(on ? Main.GREEN : Color.WHITE);
            g2.fillRoundRect(x, y, 18, 18, 6, 6);
            g2.setColor(on ? Main.GREEN : Main.MUTED);
            g2.drawRoundRect(x, y, 17, 17, 6, 6);
            if (on) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 4, x + 8, x + 14}, new int[]{y + 9, y + 13, y + 5}, 3);
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
    }

    // ---------------- buttons ----------------
    /** Flat rounded buttons with hover/pressed/disabled states. Colour comes from the button's background. */
    public static class ModernButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent c) { return new ModernButtonUI(); }

        @Override protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);
            b.setOpaque(false);
            b.setRolloverEnabled(true);
        }

        @Override public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Color bg = b.getBackground();
            boolean see = bg.getAlpha() < 255;   // translucent "ghost" buttons
            Color fill;
            if (!b.isEnabled()) fill = see ? alpha(bg, bg.getAlpha() / 2) : mix(bg, Color.WHITE, 0.6);
            else if (m.isPressed() && m.isArmed()) fill = see ? alpha(bg, bg.getAlpha() + 50) : mix(bg, Color.BLACK, 0.16);
            else if (m.isRollover()) fill = see ? alpha(bg, bg.getAlpha() + 26) : mix(bg, Color.BLACK, 0.07);
            else fill = bg;

            Graphics2D g2 = smooth(g);
            int w = c.getWidth(), h = c.getHeight();
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            if (!see && luma(bg) > 0.82) {
                g2.setColor(Main.LINE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            g2.dispose();
            super.paint(g, c);
        }

        @Override protected void paintText(Graphics g, JComponent c, Rectangle r, String text) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = g.getFontMetrics();
            g.setColor(b.isEnabled() ? b.getForeground() : mix(b.getForeground(), b.getBackground(), 0.55));
            BasicGraphicsUtils.drawStringUnderlineCharAt(g, text, b.getDisplayedMnemonicIndex(), r.x, r.y + fm.getAscent());
        }

        @Override protected void paintFocus(Graphics g, AbstractButton b, Rectangle v, Rectangle t, Rectangle i) {
            Graphics2D g2 = smooth(g);
            g2.setColor(alpha(b.getForeground(), 150));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(2, 2, b.getWidth() - 5, b.getHeight() - 5, 10, 10);
            g2.dispose();
        }
    }

    // ---------------- combo box ----------------
    public static class ModernComboBoxUI extends BasicComboBoxUI {
        public static ComponentUI createUI(JComponent c) { return new ModernComboBoxUI(); }

        @Override protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override public void paint(Graphics g) {
                    Graphics2D g2 = smooth(g);
                    g2.setColor(Main.MUTED);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
                    g2.dispose();
                }
            };
            b.setBorder(BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            b.setOpaque(false);
            return b;
        }

        @Override public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean hasFocus) {
            g.setColor(comboBox.isEnabled() ? comboBox.getBackground() : Main.GRAY);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override public void paintCurrentValue(Graphics g, Rectangle r, boolean hasFocus) {
            ListCellRenderer<Object> renderer = comboBox.getRenderer();
            Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? comboBox.getForeground() : Main.MUTED);
            c.setBackground(comboBox.isEnabled() ? comboBox.getBackground() : Main.GRAY);
            currentValuePane.paintComponent(g, c, comboBox, r.x, r.y, r.width, r.height, false);
        }

        @Override protected ListCellRenderer<Object> createRenderer() {
            return new DefaultListCellRenderer.UIResource() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                         boolean isSelected, boolean cellHasFocus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
                    l.setBorder(new EmptyBorder(6, 10, 6, 10));
                    return l;
                }
            };
        }

        @Override protected ComboPopup createPopup() {
            return new BasicComboPopup(comboBox) {
                @Override protected void configurePopup() {
                    super.configurePopup();
                    setBorder(BorderFactory.createLineBorder(Main.LINE));
                }
            };
        }
    }

    // ---------------- scroll bar ----------------
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        public static ComponentUI createUI(JComponent c) { return new ModernScrollBarUI(); }

        private JButton none() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
            return b;
        }

        @Override protected JButton createDecreaseButton(int orientation) { return none(); }
        @Override protected JButton createIncreaseButton(int orientation) { return none(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}

        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = smooth(g);
            g2.setColor(isDragging || isThumbRollover() ? Main.MUTED : Theme.mix(Main.MUTED, Color.WHITE, 0.45));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }

        @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(12, 12); }
    }

    // ---------------- tabs ----------------
    
    public static class ModernTabbedPaneUI extends BasicTabbedPaneUI {
        public static ComponentUI createUI(JComponent c) { return new ModernTabbedPaneUI(); }

        @Override protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(12, 22, 12, 22);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(0, 14, 0, 14);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabPane.setOpaque(true);
        }

        @Override protected void paintTabBackground(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {}

        @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
            if (!sel) return;
            g.setColor(Main.GREEN);
            g.fillRect(x, y + h - 3, w, 3);
        }

        @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] rects, int i, Rectangle icon, Rectangle text, boolean sel) {}

        @Override protected void paintContentBorder(Graphics g, int p, int sel) {
            int y = tabPane.getInsets().top + calculateTabAreaHeight(p, runCount, maxTabHeight);
            g.setColor(Main.LINE);
            g.drawLine(0, y - 1, tabPane.getWidth(), y - 1);
        }

        @Override protected int getTabLabelShiftX(int p, int i, boolean sel) { return 0; }
        @Override protected int getTabLabelShiftY(int p, int i, boolean sel) { return 0; }
    }
}
