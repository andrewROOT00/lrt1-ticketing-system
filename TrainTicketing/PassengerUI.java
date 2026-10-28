import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.FontMetrics;
import java.util.Random;
import java.util.function.IntConsumer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PassengerUI extends JFrame {

    private static final String[] STATIONS = Storage.STATIONS;
    private static final int[] MONEY = {1, 5, 10, 20, 50, 100, 200, 500, 1000};
    private static final int[] LOADS = {100, 200, 500};
    private static final int MAP_WIDTH = 960;

    // ---------------- Current transaction ----------------
    private int beepBalance = Storage.beepBalance();
    private int origin = 2, dest = -1;
    // Quantity tracking for [Regular, Senior, PWD]
    private int[] qtys = {1, 0, 0}; 
    private int dueCash, dueBeep, dueQr, cashInserted = 0;
    private int beepLoadTarget = 0, beepLoadInserted = 0;
    private String screen = "HOME", pendingReceipt = "";

    // ---------------- UI Components ----------------
    private final CardLayout cards = new CardLayout(), payCards = new CardLayout();
    private final JPanel screens = new JPanel(cards), payArea = new JPanel(payCards);
    private final JPanel destGrid = new JPanel(new GridLayout(5, 5, 12, 12));
    private final JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));

    private final JComboBox<String> stationBox = new JComboBox<>(STATIONS);
    private final JLabel clock = Main.label("", Font.PLAIN, 13, Main.ON_DARK);
    private final JLabel instruction = new JLabel(), map = new JLabel();
    private final JButton back = createBtn("Back", Main.GRAY, Main.TEXT, 17);
    private final JButton cancel = createBtn("Cancel", Main.RED, Color.WHITE, 17);

    // Passenger / Payment screen labels
    private final JLabel route = Main.label("", Font.BOLD, 24, Main.TEXT);
    private final JLabel fareEach = Main.label("", Font.PLAIN, 16, Main.MUTED);
    private final JLabel totalLabel = Main.label("", Font.BOLD, 28, Color.WHITE);
    
    // Counters for the passenger types
    private final JLabel[] qtyLabels = { 
        Main.label("1", Font.BOLD, 30, Main.TEXT), 
        Main.label("0", Font.BOLD, 30, Main.TEXT), 
        Main.label("0", Font.BOLD, 30, Main.TEXT) 
    };

    private final JButton cashBtn = createBtn("Cash", Color.WHITE, Main.TEXT, 20);
    private final JButton beepBtn = createBtn("Beep card", Color.WHITE, Main.TEXT, 20);
    private final JButton qrBtn = createBtn("GCash / Maya", Color.WHITE, Main.TEXT, 20);

    private final JLabel cashDue = Main.label("", Font.BOLD, 28, Main.GREEN), cashIn = Main.label("", Font.PLAIN, 20, Main.TEXT), cashLeft = Main.label("", Font.BOLD, 20, Main.TEXT);
    private final JLabel beepDue = Main.label("", Font.BOLD, 28, Main.GREEN), beepBal = Main.label("", Font.PLAIN, 20, Main.TEXT);
    private final JLabel qrDue = Main.label("", Font.BOLD, 28, Main.GREEN);
    
    // In-Purchase Load Labels
    private final JLabel loadDue = Main.label("", Font.BOLD, 28, Main.GREEN), loadIn = Main.label("", Font.PLAIN, 20, Main.TEXT), loadLeft = Main.label("", Font.BOLD, 20, Main.TEXT);

    // Standalone Load Labels
    private final JLabel saBalLabel = Main.label("", Font.BOLD, 28, Main.TEXT);
    private final JLabel saLoadDue = Main.label("", Font.BOLD, 28, Main.GREEN), saLoadIn = Main.label("", Font.PLAIN, 20, Main.TEXT), saLoadLeft = Main.label("", Font.BOLD, 20, Main.TEXT);

    private final JLabel printing = Main.label(" ", Font.BOLD, 24, Main.GREEN), doneInfo = Main.label(" ", Font.PLAIN, 18, Main.TEXT);
    private final JTextArea receiptArea = new JTextArea(12, 28);

    private final Timer clockTimer = new Timer(1000, e -> updateClock());
    private final Timer printTimer = new Timer(1500, e -> showPrintedTicket());

    public PassengerUI() {
        super("LRT-1 Ticket Vending Machine");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Main.BACKGROUND);

        JPanel header = Main.makeHeader("Ticket vending machine");
        stationBox.setSelectedIndex(origin);
        stationBox.addActionListener(e -> { origin = stationBox.getSelectedIndex(); buildDestinationButtons(); goHome(); });

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 2)); right.setOpaque(false);
        right.add(flow(FlowLayout.RIGHT, Main.label("You are at", Font.PLAIN, 13, Main.ON_DARK), stationBox));
        right.add(clock); clock.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(right, BorderLayout.EAST);

        map.setOpaque(true); map.setBackground(Color.WHITE); map.setHorizontalAlignment(SwingConstants.CENTER);
        map.setBorder(new MatteBorder(0, 0, 1, 0, Main.LINE));
        instruction.setOpaque(true); instruction.setBackground(Main.NOTICE); instruction.setForeground(Main.TEXT);
        instruction.setFont(Main.font(Font.BOLD, 17));
        instruction.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(0xEFD58A)), new EmptyBorder(12, 28, 12, 28)));

        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH); top.add(map, BorderLayout.CENTER); top.add(instruction, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        screens.setBackground(Main.BACKGROUND);
        screens.add(homeScreen(), "HOME"); 
        screens.add(destScreen(), "DEST");
        screens.add(qtyScreen(), "QTY"); 
        screens.add(payScreen(), "PAY"); 
        screens.add(standaloneLoadAmtScreen(), "SA_LOAD_AMT");
        screens.add(standaloneLoadCashScreen(), "SA_LOAD_CASH");
        screens.add(doneScreen(), "DONE");
        add(screens, BorderLayout.CENTER);

        footer.setBackground(Main.BACKGROUND); footer.setBorder(new MatteBorder(1, 0, 0, 0, Main.LINE)); footer.add(back); footer.add(cancel);
        back.addActionListener(e -> goBack()); cancel.addActionListener(e -> goHome());
        add(footer, BorderLayout.SOUTH);

        buildDestinationButtons(); goHome();
        printTimer.setRepeats(false); clockTimer.start(); updateClock();
        setSize(1000, 800); setLocationRelativeTo(null);
    }

    @Override
    public void dispose() { clockTimer.stop(); super.dispose(); Main.showMenu(); }

    // =====================================================================
    //  UI Builders
    // =====================================================================

    private JButton createBtn(String txt, Color bg, Color fg, int size) {
        JButton b = Main.btn(txt, bg, fg, ""); b.setFont(Main.font(Font.BOLD, size)); return b;
    }

    private JPanel flow(int align, Component... items) {
        JPanel p = new JPanel(new FlowLayout(align, 10, 5));
        p.setOpaque(false);
        for (Component c : items) p.add(c);
        return p;
    }

    /** Vertically and horizontally centres a column of components on the screen background. */
    private JPanel centerStack(JComponent... items) {
        JPanel box = new JPanel(); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS)); box.setOpaque(false);
        for (JComponent c : items) { c.setAlignmentX(Component.CENTER_ALIGNMENT); box.add(c); }
        JPanel p = new JPanel(new java.awt.GridBagLayout()); p.setBackground(Main.BACKGROUND);
        p.add(box);
        return p;
    }

    private Component gap(int h) { return Box.createVerticalStrut(h); }

    /** A white card with the live figures of a transaction laid out in one row. */
    private JPanel infoRow(JLabel... labels) {
        Theme.Card c = new Theme.Card(new GridLayout(1, labels.length, 12, 0));
        c.setBorder(new EmptyBorder(14, 20, 14, 20));
        for (JLabel l : labels) { l.setHorizontalAlignment(SwingConstants.CENTER); c.add(l); }
        return c;
    }

    private static Color billColor(int v) {
        switch (v) {
            case 20: return new Color(0xF2994A);    // orange
            case 50: return new Color(0xE5574F);    // red
            case 100: return new Color(0x8E5FBF);   // violet
            case 200: return new Color(0x3E9B6B);   // green
            case 500: return new Color(0xF2C94C);   // yellow
            case 1000: return new Color(0x3F7FC1);  // blue
            default: return Color.WHITE;            // coins
        }
    }

    private JPanel moneyGrid(String heading, IntConsumer onInsert) {
        JPanel money = new JPanel(new GridLayout(3, 3, 12, 12)); money.setOpaque(false);
        money.setBorder(Main.title(heading));
        for (int v : MONEY) {
            boolean bill = v >= 20;
            Color bg = billColor(v);
            Color fg = (!bill || v == 20 || v == 500) ? Main.TEXT : Color.WHITE;
            JButton b = createBtn(Main.PESO + v + (bill ? " bill" : " coin"), bg, fg, 18);
            b.addActionListener(e -> onInsert.accept(v));
            money.add(b);
        }
        return money;
    }

    private JPanel homeScreen() {
        JButton buy = createBtn("<html><center>Buy ticket<br><span style='font-size:14pt;font-weight:normal'>Pick a destination</span></center></html>", Main.GREEN, Color.WHITE, 30);
        buy.setPreferredSize(new Dimension(360, 120));
        JButton load = createBtn("<html><center>Load Beep card<br><span style='font-size:14pt;font-weight:normal'>Add stored value with cash</span></center></html>", Color.WHITE, Main.TEXT, 30);
        load.setPreferredSize(new Dimension(360, 120));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0)); btnRow.setOpaque(false);
        btnRow.add(buy); btnRow.add(load);

        JButton refund = createBtn("Refund a ticket", Main.GRAY, Main.TEXT, 16);

        buy.addActionListener(e -> showScreen("DEST", "Step 1 of 4: Touch your destination station"));
        load.addActionListener(e -> { updateSaLoadLabels(); showScreen("SA_LOAD_AMT", "Select the amount to load to your Beep card"); });
        refund.addActionListener(e -> refundAtKiosk());

        return centerStack(
            Main.label("Welcome aboard", Font.BOLD, 44, Main.TEXT),
            Main.label("Buy a ticket, load your Beep card, or pay with a QR code.", Font.PLAIN, 20, Main.MUTED),
            (JComponent) gap(40), btnRow, (JComponent) gap(22),
            Main.label("Fares: " + Main.PESO + "20 to " + Main.PESO + "55, depending on distance", Font.PLAIN, 16, Main.MUTED),
            (JComponent) gap(26), flow(FlowLayout.CENTER, refund));
    }

    private JPanel destScreen() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(Main.BACKGROUND); p.setBorder(new EmptyBorder(20, 24, 10, 24));
        destGrid.setOpaque(false); p.add(destGrid, BorderLayout.CENTER);
        return p;
    }

    private JPanel qtyScreen() {
        JButton next = createBtn("Continue to payment", Main.GREEN, Color.WHITE, 22);
        next.setPreferredSize(new Dimension(400, 58));
        next.addActionListener(e -> {
            if (totalQty() == 0) Main.msg(this, "Please select at least 1 passenger.", 2);
            else goToPayment();
        });

        String[] titles = {"Regular", "Senior citizen", "PWD"};
        String[] notes = {"Full fare", "20% off. Show a valid ID at the gate.", "20% off. Show a valid ID at the gate."};

        JPanel box = new JPanel(); box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS)); box.setOpaque(false);
        route.setAlignmentX(Component.CENTER_ALIGNMENT); fareEach.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(route); box.add(fareEach); box.add(gap(10));

        for (int i = 0; i < 3; i++) {
            int idx = i;
            JButton minus = createBtn("\u2212", Main.GRAY, Main.TEXT, 26); minus.setPreferredSize(new Dimension(54, 46)); minus.setBorder(new EmptyBorder(0, 0, 0, 0));
            JButton plus = createBtn("+", Main.GREEN, Color.WHITE, 26); plus.setPreferredSize(new Dimension(54, 46)); plus.setBorder(new EmptyBorder(0, 0, 0, 0));
            minus.addActionListener(e -> changeQuantity(idx, -1));
            plus.addActionListener(e -> changeQuantity(idx, 1));
            qtyLabels[idx].setPreferredSize(new Dimension(50, 46));

            JPanel text = new JPanel(new GridLayout(2, 1, 0, 2)); text.setOpaque(false);
            JLabel t = Main.label(titles[idx], Font.BOLD, 20, Main.TEXT), n = Main.label(notes[idx], Font.PLAIN, 13, Main.MUTED);
            t.setHorizontalAlignment(SwingConstants.LEFT); n.setHorizontalAlignment(SwingConstants.LEFT);
            text.add(t); text.add(n);

            Theme.Card row = new Theme.Card(new BorderLayout(16, 0));
            row.setBorder(new EmptyBorder(8, 22, 8, 18));
            row.setPreferredSize(new Dimension(600, 68)); row.setMaximumSize(new Dimension(600, 68));
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            row.add(text, BorderLayout.CENTER);
            JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); stepper.setOpaque(false);
            stepper.add(minus); stepper.add(qtyLabels[idx]); stepper.add(plus);
            row.add(stepper, BorderLayout.EAST);
            box.add(row); box.add(gap(8));
        }

        Theme.Card total = new Theme.Card(new BorderLayout(), Main.DARK_GREEN, null, 14);
        total.setBorder(new EmptyBorder(8, 24, 8, 24));
        total.setPreferredSize(new Dimension(600, 58)); total.setMaximumSize(new Dimension(600, 58));
        total.setAlignmentX(Component.CENTER_ALIGNMENT);
        total.add(totalLabel, BorderLayout.CENTER);
        box.add(gap(4)); box.add(total); box.add(gap(14));
        next.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(next);

        JPanel p = new JPanel(new java.awt.GridBagLayout()); p.setBackground(Main.BACKGROUND);
        p.add(box);
        return p;
    }

    private JPanel standaloneLoadAmtScreen() {
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0)); btns.setOpaque(false);
        for (int amt : LOADS) {
            JButton b = createBtn("+" + Main.PESO + amt, Main.GREEN, Color.WHITE, 30);
            b.setPreferredSize(new Dimension(190, 96));
            b.addActionListener(e -> {
                beepLoadTarget = amt; beepLoadInserted = 0; updateSaLoadLabels();
                showScreen("SA_LOAD_CASH", "Insert cash to load your Beep card");
            });
            btns.add(b);
        }
        return centerStack(saBalLabel, Main.label("Choose how much to add", Font.PLAIN, 18, Main.MUTED), (JComponent) gap(32), btns);
    }

    private JPanel standaloneLoadCashScreen() {
        JPanel p = new JPanel(new BorderLayout(20, 20)); p.setBackground(Main.BACKGROUND); p.setBorder(new EmptyBorder(25, 30, 20, 30));
        p.add(infoRow(saLoadDue, saLoadIn, saLoadLeft), BorderLayout.NORTH);
        p.add(moneyGrid("Insert coins or bills to load", this::insertSaLoadMoney), BorderLayout.CENTER);
        return p;
    }

    /** Shows which payment method is chosen: the selected tile turns green. */
    private String payHtml(String name, String sub, int amt, boolean on) {
        String c = on ? "#FFFFFF" : "#14231C", m = on ? "#CDEBDD" : "#5E6E66";
        return "<html><center><span style='color:" + c + ";font-size:17pt;font-weight:bold'>" + name + "</span><br>"
             + "<span style='color:" + m + ";font-size:12pt;font-weight:normal'>" + sub + "</span><br>"
             + "<span style='color:" + c + ";font-size:19pt;font-weight:bold'>" + Main.PESO + amt + "</span></center></html>";
    }

    private void selectPay(int idx) {
        JButton[] bs = {cashBtn, beepBtn, qrBtn};
        String[] names = {"Cash", "Beep card", "GCash / Maya"}, subs = {"Single journey", "Stored value", "QR code"};
        int[] amts = {dueCash, dueBeep, dueQr};
        for (int i = 0; i < 3; i++) {
            boolean on = i == idx;
            bs[i].setBackground(on ? Main.GREEN : Color.WHITE);
            bs[i].setForeground(on ? Color.WHITE : Main.TEXT);
            bs[i].setText(payHtml(names[i], subs[i], amts[i], on));
        }
    }

    private JPanel payScreen() {
        cashBtn.addActionListener(e -> { selectPay(0); updateCashLabels(); payCards.show(payArea, "CASH"); });
        beepBtn.addActionListener(e -> { selectPay(1); updateBeepLabels(); payCards.show(payArea, "BEEP"); });
        qrBtn.addActionListener(e -> { selectPay(2); updateQrLabels(); payCards.show(payArea, "QR"); });

        JPanel methods = new JPanel(new GridLayout(1, 3, 15, 0)); methods.setOpaque(false);
        methods.setPreferredSize(new Dimension(0, 120));
        methods.add(cashBtn); methods.add(beepBtn); methods.add(qrBtn);

        payArea.setOpaque(false);
        payArea.add(flow(FlowLayout.CENTER, Main.label("Choose a payment method above", Font.PLAIN, 20, Main.MUTED)), "CHOOSE");
        payArea.add(cashPanel(), "CASH");
        payArea.add(beepPanel(), "BEEP");
        payArea.add(beepLoadPanel(), "BEEP_LOAD");
        payArea.add(qrPanel(), "QR");

        JPanel p = new JPanel(new BorderLayout(15, 20)); p.setBackground(Main.BACKGROUND); p.setBorder(new EmptyBorder(25, 30, 20, 30));
        p.add(methods, BorderLayout.NORTH); p.add(payArea, BorderLayout.CENTER);
        return p;
    }

    private JPanel cashPanel() {
        JPanel p = new JPanel(new BorderLayout(20, 16)); p.setOpaque(false);
        p.add(infoRow(cashDue, cashIn, cashLeft), BorderLayout.NORTH);
        p.add(moneyGrid("Insert coins or bills", this::insertMoney), BorderLayout.CENTER);
        return p;
    }

    private JPanel beepPanel() {
        JButton tap = createBtn("Tap your Beep card here", Main.YELLOW, Main.TEXT, 24);
        tap.addActionListener(e -> {
            if (beepBalance < dueBeep) Main.msg(this, "Insufficient balance! Short by " + Main.PESO + (dueBeep - beepBalance), 2);
            else completePurchase(1);
        });

        JPanel load = new JPanel(new GridLayout(1, 3, 15, 15)); load.setOpaque(false); load.setPreferredSize(new Dimension(0, 110));
        load.setBorder(Main.title("Not enough value? Load your Beep card"));

        for (int amt : LOADS) {
            JButton b = createBtn("+" + Main.PESO + amt, Main.GREEN, Color.WHITE, 20);
            b.addActionListener(e -> {
                beepLoadTarget = amt;
                beepLoadInserted = 0;
                updateLoadLabels();
                payCards.show(payArea, "BEEP_LOAD");
            });
            load.add(b);
        }

        JPanel p = new JPanel(new BorderLayout(20, 16)); p.setOpaque(false);
        p.add(infoRow(beepDue, beepBal), BorderLayout.NORTH); p.add(tap, BorderLayout.CENTER); p.add(load, BorderLayout.SOUTH);
        return p;
    }

    private JPanel beepLoadPanel() {
        JButton cancelLoadBtn = createBtn("Cancel load", Main.RED_SOFT, Main.RED, 15);
        cancelLoadBtn.addActionListener(e -> {
            if (beepLoadInserted > 0) Main.msg(this, "Load cancelled. Take refund:\n" + changeText(beepLoadInserted), 1);
            beepLoadInserted = 0;
            payCards.show(payArea, "BEEP");
        });

        JPanel top = new JPanel(new BorderLayout(14, 0)); top.setOpaque(false);
        top.add(infoRow(loadDue, loadIn, loadLeft), BorderLayout.CENTER);
        top.add(cancelLoadBtn, BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout(20, 16)); p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(moneyGrid("Insert coins or bills to load", this::insertLoadMoney), BorderLayout.CENTER);
        return p;
    }

    private JPanel qrPanel() {
        Theme.Card qrBox = new Theme.Card(new BorderLayout(28, 0));
        qrBox.setBorder(new EmptyBorder(18, 28, 18, 28));
        JLabel qrDisplay = new JLabel(new QrIcon(125), SwingConstants.CENTER);
        JLabel hint = Main.label("Scan with the GCash or Maya app to pay", Font.PLAIN, 18, Main.MUTED);
        hint.setHorizontalAlignment(SwingConstants.LEFT);

        JButton scanDone = createBtn("Simulate payment confirmed", Main.GREEN, Color.WHITE, 20);
        scanDone.addActionListener(e -> completePurchase(2));

        JPanel side = new JPanel(new GridLayout(2, 1, 0, 14)); side.setOpaque(false);
        side.add(hint); side.add(scanDone);
        qrBox.add(qrDisplay, BorderLayout.WEST);
        qrBox.add(side, BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout(20, 16)); p.setOpaque(false);
        p.add(infoRow(qrDue), BorderLayout.NORTH); p.add(qrBox, BorderLayout.CENTER);
        return p;
    }

    private JPanel doneScreen() {
        receiptArea.setEditable(false); receiptArea.setFont(Main.mono(15)); receiptArea.setMargin(new Insets(15, 20, 15, 20));
        JScrollPane paper = new JScrollPane(receiptArea);
        paper.setBorder(new Theme.RoundedBorder(Main.LINE, 12, new Insets(1, 1, 1, 1)));

        JButton again = createBtn("Buy another ticket", Main.GREEN, Color.WHITE, 22); again.addActionListener(e -> goHome());

        JPanel south = new JPanel(new GridLayout(2, 1, 0, 15)); south.setOpaque(false); south.add(doneInfo); south.add(again);
        JPanel p = new JPanel(new BorderLayout(15, 15)); p.setBackground(Main.BACKGROUND); p.setBorder(new EmptyBorder(25, 45, 25, 45));
        p.add(printing, BorderLayout.NORTH); p.add(paper, BorderLayout.CENTER); p.add(south, BorderLayout.SOUTH);
        return p;
    }

    /** Placeholder QR code (decorative; the payment is simulated). */
    private static class QrIcon implements Icon {
        private static final int N = 25;
        private final int size;
        private final boolean[][] m = new boolean[N][N];

        QrIcon(int size) {
            this.size = size;
            Random r = new Random(7);
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) m[y][x] = r.nextInt(100) < 48;
            for (int[] f : new int[][]{{0, 0}, {N - 7, 0}, {0, N - 7}}) {
                for (int y = -1; y <= 7; y++) for (int x = -1; x <= 7; x++) {
                    int cx = f[0] + x, cy = f[1] + y;
                    if (cx < 0 || cy < 0 || cx >= N || cy >= N) continue;
                    boolean ring = x >= 0 && x <= 6 && y >= 0 && y <= 6 && (x == 0 || x == 6 || y == 0 || y == 6);
                    boolean core = x >= 2 && x <= 4 && y >= 2 && y <= 4;
                    m[cy][cx] = ring || core;
                }
            }
        }

        @Override public void paintIcon(Component c, java.awt.Graphics g, int x0, int y0) {
            int cell = size / N, off = (size - cell * N) / 2;
            g.setColor(Main.DARK_GREEN);
            for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
                if (m[y][x]) g.fillRect(x0 + off + x * cell, y0 + off + y * cell, cell, cell);
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // =====================================================================
    //  Map & State Logic
    // =====================================================================

    private void buildDestinationButtons() {
        destGrid.removeAll();
        for (int i = 0; i < STATIONS.length; i++) {
            JButton b;
            if (i == origin) {
                b = Main.btn("<html><center>" + STATIONS[i] + "<br><i>You are here</i></center></html>", Main.GRAY, Main.MUTED, "");
                b.setEnabled(false);
            } else {
                int fare = (int)Storage.fare(STATIONS[origin], STATIONS[i], "Cash", "Regular");
                b = Main.btn("<html><center>" + STATIONS[i] + "<br><span style='color:#0E7A4B'><b>" + Main.PESO + fare + "</b></span></center></html>", Color.WHITE, Main.TEXT, "");
                int destIdx = i; b.addActionListener(e -> chooseDestination(destIdx));
            }
            b.setFont(Main.font(Font.PLAIN, 14));
            b.setBorder(new EmptyBorder(6, 6, 6, 6));
            destGrid.add(b);
        }
        destGrid.revalidate(); destGrid.repaint();
    }

    private void drawMap() {
        BufferedImage img = new BufferedImage(MAP_WIDTH, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, MAP_WIDTH, 120);

        int count = STATIONS.length, left = 50, right = MAP_WIDTH - 50, y = 84;
        double gap = (right - left) / (double)(count - 1);
        int lo = Math.min(origin, dest < 0 ? origin : dest), hi = Math.max(origin, dest < 0 ? origin : dest);

        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xCFE3D8)); g.drawLine(left, y, right, y);
        if (dest >= 0) { g.setColor(Main.GREEN); g.drawLine((int)(left + lo * gap), y, (int)(left + hi * gap), y); }

        for (int i = 0; i < count; i++) {
            int x = (int)(left + i * gap);
            boolean end = i == origin || i == dest, inRange = dest >= 0 && i >= lo && i <= hi;
            int s = end ? 22 : 12;
            g.setColor(i == origin ? Main.YELLOW : i == dest ? Main.GREEN : Color.WHITE);
            g.fillOval(x - s / 2, y - s / 2, s, s);
            g.setStroke(new BasicStroke(end ? 3f : 2f));
            g.setColor(end ? Main.DARK_GREEN : inRange ? Main.GREEN : new Color(0x8DB5A0));
            g.drawOval(x - s / 2, y - s / 2, s, s);
        }

        g.setColor(Main.MUTED); g.setFont(Main.font(Font.PLAIN, 11));
        drawCenter(g, STATIONS[0], left, y + 30); drawCenter(g, STATIONS[count - 1], right, y + 30);

        g.setFont(Main.font(Font.BOLD, 13));
        int low = y - 20, high = y - 48, originY = low, destY = low;
        if (dest >= 0 && Math.abs(origin - dest) <= 3) { if (origin < dest) originY = high; else destY = high; }
        tag(g, STATIONS[origin], (int)(left + origin * gap), originY, Main.YELLOW, Main.TEXT);
        if (dest >= 0) tag(g, STATIONS[dest], (int)(left + dest * gap), destY, Main.GREEN, Color.WHITE);

        g.dispose(); map.setIcon(new ImageIcon(img));
    }

    private void tag(Graphics2D g, String txt, int x, int bottom, Color bg, Color fg) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(txt) + 20, h = 24;
        int tx = Math.max(4, Math.min(x - w / 2, MAP_WIDTH - w - 4));
        g.setColor(bg); g.fillRoundRect(tx, bottom - h, w, h, 12, 12);
        g.setColor(fg); g.drawString(txt, tx + 10, bottom - 7);
    }

    private void drawCenter(Graphics2D g, String txt, int x, int y) {
        int w = g.getFontMetrics().stringWidth(txt);
        g.drawString(txt, Math.max(4, Math.min(x - w / 2, MAP_WIDTH - w - 4)), y);
    }

    private void showScreen(String name, String text) {
        screen = name; instruction.setText(text);
        footer.setVisible(!name.equals("HOME") && !name.equals("DONE"));
        cards.show(screens, name);
    }

    private void goHome() { 
        refundCash(); dest = -1; 
        qtys[0] = 1; qtys[1] = 0; qtys[2] = 0; 
        drawMap(); showScreen("HOME", "Touch Buy ticket or Load Beep card to begin"); 
    }
    
    private void goBack() {
        refundCash();
        if (screen.equals("QTY")) showScreen("DEST", "Step 1 of 4: Touch your destination station");
        else if (screen.equals("PAY")) showScreen("QTY", "Step 2 of 4: How many passengers?");
        else if (screen.equals("SA_LOAD_AMT")) goHome();
        else if (screen.equals("SA_LOAD_CASH")) showScreen("SA_LOAD_AMT", "Select the amount to load to your Beep card");
        else goHome();
    }

    private void chooseDestination(int index) { 
        dest = index; 
        qtys[0] = 1; qtys[1] = 0; qtys[2] = 0; 
        drawMap(); updateQtyLabels(); 
        showScreen("QTY", "Step 2 of 4: How many passengers?"); 
    }
    
    private int totalQty() { return qtys[0] + qtys[1] + qtys[2]; }

    private void changeQuantity(int idx, int change) { 
        int newQty = qtys[idx] + change;
        if (newQty >= 0 && (totalQty() + change) <= 10) {
            qtys[idx] = newQty;
            updateQtyLabels();
        }
    }
    
    private void goToPayment() {
        dueCash = totalFare(0); 
        dueBeep = totalFare(1); 
        dueQr = totalFare(2);
        cashInserted = 0;
        beepLoadInserted = 0; // Reset load tracking
        
        selectPay(-1);
        
        payCards.show(payArea, "CHOOSE"); showScreen("PAY", "Step 3 of 4: Choose payment method");
    }

    private void insertMoney(int value) { cashInserted += value; updateCashLabels(); if (cashInserted >= dueCash) completePurchase(0); }

    private void insertLoadMoney(int value) {
        beepLoadInserted += value; 
        updateLoadLabels();
        if (beepLoadInserted >= beepLoadTarget) {
            int change = beepLoadInserted - beepLoadTarget;
            beepBalance += beepLoadTarget; 
            Storage.setBeepBalance(beepBalance);
            
            String msg = "Successfully loaded " + Main.PESO + beepLoadTarget + " to Beep Card.";
            if (change > 0) msg += "\nTake your change: " + changeText(change);
            Main.msg(this, msg, 1);
            
            beepLoadInserted = 0;
            updateBeepLabels();
            payCards.show(payArea, "BEEP"); // Return to standard beep panel
        }
    }

    private void insertSaLoadMoney(int value) {
        beepLoadInserted += value; 
        updateSaLoadLabels();
        if (beepLoadInserted >= beepLoadTarget) {
            int change = beepLoadInserted - beepLoadTarget;
            beepBalance += beepLoadTarget; 
            Storage.setBeepBalance(beepBalance);
            
            String msg = "Successfully loaded " + Main.PESO + beepLoadTarget + " to Beep Card.\nNew Balance: " + Main.PESO + beepBalance;
            if (change > 0) msg += "\nTake your change: " + changeText(change);
            Main.msg(this, msg, 1);
            
            beepLoadInserted = 0;
            goHome();
        }
    }

    private void refundCash() {
        if (cashInserted > 0) { Main.msg(this, "Transaction cancelled. Take refund:\n" + changeText(cashInserted), 1); cashInserted = 0; }
        if (beepLoadInserted > 0) { Main.msg(this, "Load cancelled. Take refund:\n" + changeText(beepLoadInserted), 1); beepLoadInserted = 0; }
    }

    private void completePurchase(int payIdx) {
        int due = payIdx == 0 ? dueCash : payIdx == 1 ? dueBeep : dueQr; 
        String text = "";
        
        for (int typeIdx = 0; typeIdx < 3; typeIdx++) {
            for (int i = 0; i < qtys[typeIdx]; i++) {
                text += Storage.receipt(Storage.issueTicket(STATIONS[origin], STATIONS[dest], Storage.PAYMENTS[payIdx], Storage.PASSENGERS[typeIdx])) + "\n\n";
            }
        }
        
        int totalTix = totalQty();
        String info;
        if (payIdx == 1) {
            beepBalance -= due; Storage.setBeepBalance(beepBalance);
            text += "Tickets   : " + totalTix + "\nTotal paid: " + Main.PESO + due + "\nCard bal. : " + Main.PESO + beepBalance;
            info = "Paid with Beep Card. Balance: " + Main.PESO + beepBalance;
        } else if (payIdx == 2) {
            text += "Tickets   : " + totalTix + "\nTotal paid: " + Main.PESO + due + "\nMethod    : GCash / Maya (QR)";
            info = "Paid via GCash / Maya QR Code. Payment Confirmed.";
        } else {
            int change = cashInserted - due;
            text += "Tickets   : " + totalTix + "\nTotal due : " + Main.PESO + due + "\nInserted  : " + Main.PESO + cashInserted + "\nChange    : " + Main.PESO + change;
            info = changeText(change); cashInserted = 0;
        }
        pendingReceipt = text; printing.setText("Printing your ticket(s)..."); receiptArea.setText(""); doneInfo.setText(info);
        showScreen("DONE", "Step 4 of 4: Collect your ticket"); printTimer.start();
    }

    private void showPrintedTicket() { receiptArea.setText(pendingReceipt); receiptArea.setCaretPosition(0); printing.setText("Take your ticket(s). Have a safe trip!"); }

    private String changeText(int amt) {
        if (amt == 0) return "No change. Exact amount!";
        String text = "Change: " + Main.PESO + amt + " (";
        for (int v : new int[]{1000, 500, 200, 100, 50, 20, 10, 5, 1}) {
            int c = amt / v;
            if (c > 0) { text += (text.endsWith("(") ? "" : ", ") + c + " x " + Main.PESO + v; amt -= c * v; }
        }
        return text + ")";
    }

    private int fareEach(int payIdx, int typeIdx) { return (int)Storage.fare(STATIONS[origin], STATIONS[dest], Storage.PAYMENTS[payIdx], Storage.PASSENGERS[typeIdx]); }
    
    private int totalFare(int payIdx) {
        return (qtys[0] * fareEach(payIdx, 0)) + (qtys[1] * fareEach(payIdx, 1)) + (qtys[2] * fareEach(payIdx, 2));
    }

    private void updateQtyLabels() {
        if (dest < 0) return;
        route.setText(STATIONS[origin] + "  \u2192  " + STATIONS[dest]);
        fareEach.setText("Base fare (Regular): " + Main.PESO + fareEach(0, 0)); 
        
        for (int i = 0; i < 3; i++) {
            qtyLabels[i].setText(String.valueOf(qtys[i]));
        }
        
        totalLabel.setText("Total due: " + Main.PESO + totalFare(0));
    }
    
    private void updateCashLabels() { cashDue.setText("Amount due: " + Main.PESO + dueCash); cashIn.setText("Inserted: " + Main.PESO + cashInserted); cashLeft.setText("Remaining: " + Main.PESO + Math.max(0, dueCash - cashInserted)); }
    private void updateBeepLabels() { beepDue.setText("Amount due: " + Main.PESO + dueBeep); beepBal.setText("Beep Card balance: " + Main.PESO + beepBalance); }
    private void updateLoadLabels() { loadDue.setText("Load amount: " + Main.PESO + beepLoadTarget); loadIn.setText("Inserted: " + Main.PESO + beepLoadInserted); loadLeft.setText("Remaining: " + Main.PESO + Math.max(0, beepLoadTarget - beepLoadInserted)); }
    private void updateSaLoadLabels() { saBalLabel.setText("Current Beep Balance: " + Main.PESO + beepBalance); saLoadDue.setText("Load amount: " + Main.PESO + beepLoadTarget); saLoadIn.setText("Inserted: " + Main.PESO + beepLoadInserted); saLoadLeft.setText("Remaining: " + Main.PESO + Math.max(0, beepLoadTarget - beepLoadInserted)); }
    private void updateQrLabels() { qrDue.setText("Amount due: " + Main.PESO + dueQr); }
    private void updateClock() { clock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM dd yyyy   hh:mm:ss a"))); }

    private void refundAtKiosk() {
        String id = JOptionPane.showInputDialog(this, "Enter Ticket ID (e.g. LRT-1001)\nRefundable within " + Storage.REFUND_MINUTES + " mins:");
        if (id != null && !id.trim().isEmpty()) {
            try {
                String[] t = Storage.find(Storage.tickets(), id.trim());
                if (t == null) { Main.msg(this, "Ticket not found.", 2); return; }
                
                int amt = (int)Storage.fare(t);
                if (JOptionPane.showConfirmDialog(this, "Refund ticket " + t[Storage.ID] + "?\nYou will get back " + Main.PESO + amt, "Confirm", 0) == 0) {
                    Storage.refund(t[Storage.ID], true);
                    beepBalance = Storage.beepBalance();
                    Main.msg(this, "Beep Card".equals(t[Storage.PAY]) ? "Returned to Beep Card. New balance: " + Main.PESO + beepBalance : "Take your cash refund:\n" + changeText(amt).replace("Change:", "Refund:"), 1);
                }
            } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
        }
    }
}