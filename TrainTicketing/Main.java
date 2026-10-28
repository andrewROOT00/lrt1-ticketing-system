import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends JFrame {

    // ---------------- Shared palette & constants ----------------
    public static final String PESO = "\u20B1";
    public static final Color GREEN = new Color(0x0E7A4B),        // signal green: primary actions
            DARK_GREEN = new Color(0x0B3D2E),                      // deep transit green: header, emphasis
            YELLOW = new Color(0xF5B82E),                          // signage amber: accent
            BACKGROUND = new Color(0xF2F5F3),                      // app background
            GRAY = new Color(0xE6EBE8),                            // neutral / secondary buttons
            RED = new Color(0xC8402F),                             // destructive
            RED_SOFT = new Color(0xFBE6E2),                        // destructive, low emphasis
            TEXT = new Color(0x14231C),                            // body text
            MUTED = new Color(0x5E6E66),                           // secondary text
            LINE = new Color(0xD5DCD8),                            // hairlines, borders
            SELECTION = new Color(0xD3EBDD),                       // selected rows, hover tints
            NOTICE = new Color(0xFFF4D6),                          // instruction bar
            ON_DARK = new Color(0xBFE0CF),                         // secondary text on dark green
            GHOST = new Color(255, 255, 255, 38);                  // translucent button on dark green

    // ---------------- Shared UI Helpers ----------------
    private static String family, monoFamily;

    private static void loadFamilies() {
        if (family != null) return;
        Set<String> have = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        family = Font.SANS_SERIF;
        for (String f : new String[]{"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Roboto", "Noto Sans", "Ubuntu", "Open Sans"})
            if (have.contains(f)) { family = f; break; }
        monoFamily = have.contains("Consolas") ? "Consolas" : Font.MONOSPACED;
    }

    public static Font font(int style, int size) { loadFamilies(); return new Font(family, style, size); }
    public static Font mono(int size) { loadFamilies(); return new Font(monoFamily, Font.PLAIN, size); }

    public static JButton btn(String txt, Color bg, Color fg, String tip) {
        JButton b = new JButton(txt);
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(font(Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setToolTipText(tip == null || tip.isEmpty() ? null : tip);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** A low-key text button, for secondary links such as "Forgot password?". */
    public static JButton link(String txt, String tip) {
        JButton b = btn(txt, new Color(0, 0, 0, 0), GREEN, tip);
        b.setFont(font(Font.BOLD, 13));
        return b;
    }

    public static JLabel label(String txt, int style, int size, Color c) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setFont(font(style, size));
        if (c != null) l.setForeground(c);
        return l;
    }

    public static Border title(String text) {
        TitledBorder b = BorderFactory.createTitledBorder(new Theme.RoundedBorder(LINE, 12, new Insets(1, 1, 1, 1)), text);
        b.setTitleFont(font(Font.BOLD, 13));
        b.setTitleColor(MUTED);
        return new CompoundBorder(b, new EmptyBorder(8, 12, 12, 12));
    }

    public static JPanel makeHeader(String sub) {
        JPanel p = new JPanel(new BorderLayout(18, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(YELLOW);
                g.fillRect(0, getHeight() - 3, getWidth(), 3);   // signage stripe
            }
        };
        p.setBackground(DARK_GREEN);
        p.setBorder(new EmptyBorder(14, 28, 17, 28));

        JPanel logo = new JPanel(new GridBagLayout());
        logo.setOpaque(false);
        logo.add(Theme.badge("LRT-1"));
        p.add(logo, BorderLayout.WEST);

        JLabel s = label(sub, Font.PLAIN, 15, ON_DARK);
        s.setHorizontalAlignment(SwingConstants.LEFT);
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    public static void msg(Component c, String txt, int type) {
        JOptionPane.showMessageDialog(c, txt, type == 1 ? "Info" : type == 2 ? "Warning" : "Error", type);
    }

    // ---------------- Tables ----------------
    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, false, r, c);
            String col = t.getColumnName(c), s = String.valueOf(v);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setBackground(sel ? SELECTION : r % 2 == 0 ? Color.WHITE : new Color(0xF8FAF9));
            setForeground(TEXT);
            setFont(font(Font.PLAIN, 13));
            setHorizontalAlignment(col.equals("Fare") ? SwingConstants.RIGHT : col.equals("No.") ? SwingConstants.CENTER : SwingConstants.LEFT);
            setToolTipText(s.length() > 28 ? s : null);
            if (col.equals("Status")) {
                Color k = s.equals("Active") || s.equals("Done") ? GREEN : s.equals("BLOCKED") ? RED : new Color(0x9A6700);
                setForeground(k);
                setFont(font(Font.BOLD, 13));
                setText("\u25CF  " + s);
            }
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            String col = t.getColumnName(c);
            setBackground(new Color(0xE9EFEB));
            setForeground(TEXT);
            setFont(font(Font.BOLD, 13));
            setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(0, 12, 0, 12)));
            setHorizontalAlignment(col.equals("Fare") ? SwingConstants.RIGHT : col.equals("No.") ? SwingConstants.CENTER : SwingConstants.LEFT);
            setOpaque(true);
            return this;
        }
    }

    public static JTable table(String... cols) {
        JTable t = new JTable(new DefaultTableModel(cols, 0)) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        t.setRowHeight(40);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(0xE8EDEA));
        t.setFont(font(Font.PLAIN, 13));
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(SELECTION);
        t.setSelectionForeground(TEXT);
        t.setDefaultRenderer(Object.class, new CellRenderer());

        t.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 42));
        t.getTableHeader().setBorder(null);
        return t;
    }

    public static void fill(JTable t, List<Object[]> rows) {
        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);
        rows.forEach(m::addRow);
    }

    // ---------------- Program Start ----------------
    public static void showMenu() { new MenuWindow().setVisible(true); }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(Main::showMenu);
    }

    /** First screen: choose passenger kiosk or staff back office. */
    private static class MenuWindow extends JFrame {
        MenuWindow() {
            super("LRT-1 Ticketing System");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(BACKGROUND);
            add(makeHeader("Ticketing system"), BorderLayout.NORTH);

            JPanel cards = new JPanel(new GridLayout(1, 2, 20, 0));
            cards.setOpaque(false);
            JButton pass = btn("Open ticket kiosk", GREEN, Color.WHITE, null);
            JButton staff = btn("Staff / admin login", DARK_GREEN, Color.WHITE, null);
            JButton exit = btn("Exit", GRAY, TEXT, null);
            pass.addActionListener(e -> { dispose(); new PassengerUI().setVisible(true); });
            staff.addActionListener(e -> { dispose(); new Main().setVisible(true); });
            exit.addActionListener(e -> dispose());
            cards.add(choice("Passenger", "Buy single-journey tickets, load a Beep card or request a refund.", pass));
            cards.add(choice("Staff", "Sign in to manage ticket sales, refunds, users and messages.", staff));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            JLabel q = label("Who is using the system?", Font.BOLD, 26, TEXT);
            q.setAlignmentX(Component.CENTER_ALIGNMENT);
            cards.setAlignmentX(Component.CENTER_ALIGNMENT);
            exit.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(q);
            center.add(javax.swing.Box.createVerticalStrut(22));
            center.add(cards);
            center.add(javax.swing.Box.createVerticalStrut(20));
            center.add(exit);

            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(36, 40, 40, 40));
            wrap.add(center);
            add(wrap, BorderLayout.CENTER);
            pack();
            setMinimumSize(new Dimension(760, getHeight()));
            setLocationRelativeTo(null);
        }

        private JPanel choice(String name, String blurb, JButton action) {
            Theme.Card c = new Theme.Card(new BorderLayout(0, 16));
            c.setBorder(new EmptyBorder(26, 28, 26, 28));
            JLabel t = label(name, Font.BOLD, 22, TEXT);
            t.setHorizontalAlignment(SwingConstants.LEFT);
            JLabel d = label("<html><body style='width:230px'>" + blurb + "</body></html>", Font.PLAIN, 14, MUTED);
            d.setHorizontalAlignment(SwingConstants.LEFT);
            d.setVerticalAlignment(SwingConstants.TOP);
            c.add(t, BorderLayout.NORTH);
            c.add(d, BorderLayout.CENTER);
            c.add(action, BorderLayout.SOUTH);
            return c;
        }
    }

    // =====================================================================
    //  The Login Window
    // =====================================================================
    private int attempts = 0;
    private boolean backToMenu = true;
    private final JTextField userField = new JTextField(22);
    private final JPasswordField passField = new JPasswordField(22);
    private final JLabel triesLabel = label("Tries left: 3", Font.PLAIN, 12, MUTED);

    public Main() {
        super("LRT-1 Ticketing System - Login");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND);
        add(makeHeader("Back office"), BorderLayout.NORTH);

        Theme.Card card = new Theme.Card(new GridBagLayout());
        card.setBorder(new EmptyBorder(32, 38, 26, 38));

        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false); show.setFocusPainted(false); show.setFont(font(Font.PLAIN, 13));
        show.addActionListener(e -> passField.setEchoChar(show.isSelected() ? (char) 0 : '\u2022'));
        triesLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JButton login = btn("Log in", GREEN, Color.WHITE, "Log in");
        login.setFont(font(Font.BOLD, 15));
        JButton forgot = link("Forgot password?", "Ask the admin to reset your password");

        login.addActionListener(e -> doLogin());
        forgot.addActionListener(e -> forgotPassword());

        JLabel head = label("Staff sign in", Font.BOLD, 26, TEXT);
        JLabel sub = label("Use your staff or admin account.", Font.PLAIN, 14, MUTED);
        JLabel uLbl = label("Username", Font.BOLD, 13, TEXT), pLbl = label("Password", Font.BOLD, 13, TEXT);
        for (JLabel l : new JLabel[]{head, sub, uLbl, pLbl}) l.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.add(show, BorderLayout.WEST);
        options.add(triesLabel, BorderLayout.EAST);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.setOpaque(false);
        links.add(forgot);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        Component[] rows = {head, sub, uLbl, userField, pLbl, passField, options, login, links};
        int[] gap = {0, 4, 22, 6, 14, 6, 10, 20, 12};
        for (int i = 0; i < rows.length; i++) {
            g.gridy = i; g.insets = new Insets(gap[i], 0, 0, 0);
            card.add(rows[i], g);
        }

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(BACKGROUND);
        wrap.setBorder(new EmptyBorder(32, 40, 40, 40));
        wrap.add(card);
        add(wrap, BorderLayout.CENTER);

        getRootPane().setDefaultButton(login);
        pack(); setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (backToMenu) showMenu();
    }

    private void doLogin() {
        String u = userField.getText().trim(), p = new String(passField.getPassword());
        if (u.isEmpty() || p.isEmpty()) { msg(this, "Fill in both username and password.", 2); return; }

        try {
            String[] user = Storage.login(u, p);
            backToMenu = false;
            dispose();
            new AdminUI(user).setVisible(true);
        } catch (IllegalArgumentException ex) {
            attempts++;
            if (attempts >= 3) {
                try { Storage.blockUser(u); } catch (Exception ignored) {}
                msg(this, "Too many wrong tries! Account blocked. System closing.", 0);
                System.exit(0);
            } else {
                triesLabel.setText("Tries left: " + (3 - attempts));
                triesLabel.setForeground(RED);
                msg(this, "Wrong credentials. Try again.", 0);
            }
        } catch (Exception ex) {
            msg(this, ex.getMessage(), 0);
        }
    }

    private void forgotPassword() {
        String u = JOptionPane.showInputDialog(this, "Enter username to request a reset from Admin:");
        if (u != null && !u.trim().isEmpty()) {
            try {
                Storage.requestPasswordReset(u.trim());
                msg(this, "Request sent to the Admin.", 1);
            } catch (Exception ex) { msg(this, ex.getMessage(), 0); }
        }
    }
}