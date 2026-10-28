import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminUI extends JFrame {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int[] SORT_FIELDS = {Storage.ID, Storage.TIME, Storage.FROM, Storage.TO, Storage.PAY, Storage.FARE, Storage.TYPE, Storage.PTYPE, Storage.STATUS};

    private String[] me;
    private final boolean isAdmin;
    private final JTabbedPane tabs = new JTabbedPane();
    
    private List<String[]> allTickets = new ArrayList<>(), shown = new ArrayList<>();
    private List<String[]> users = new ArrayList<>(), messages = new ArrayList<>();

    // UI Components
    private final JTable ticketTable = Main.table("No.", "Ticket ID", "Type", "From", "To", "Passenger", "Fare", "Payment", "Date / Time", "Status");
    private final JTextField search = new JTextField(15);
    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{"Ticket ID", "Date / Time", "From Station", "To Station", "Payment Method", "Fare", "Ticket Type", "Passenger Type", "Status"});
    private final JComboBox<String> orderBox = new JComboBox<>(new String[]{"Ascending", "Descending"});
    private final JLabel[] sums = {new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel(), new JLabel()};
    private final JLabel[] caps = new JLabel[6];

    private final JTable userTable = Main.table("No.", "Username", "Access Level", "Status");
    private final JComboBox<String> userSortBox = new JComboBox<>(new String[]{"Username", "Access Level", "Status"});
    private final JComboBox<String> userOrderBox = new JComboBox<>(new String[]{"Ascending", "Descending"});

    private final JTable msgTable = Main.table("No.", "Date / Time", "Type", "Username", "Message", "Status");
    private final JComboBox<String> msgSortBox = new JComboBox<>(new String[]{"Date / Time", "Type", "Username", "Status"});
    private final JComboBox<String> msgOrderBox = new JComboBox<>(new String[]{"Descending", "Ascending"});

    private final JTable myMsgTable = Main.table("No.", "Date / Time", "From", "Message");

    public AdminUI(String[] user) {
        super("LRT-1 Back Office");
        me = user;
        isAdmin = Storage.isAdmin(user);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Main.BACKGROUND);
        add(buildHeader(), BorderLayout.NORTH);

        tabs.setFont(Main.font(Font.BOLD, 14));
        tabs.addTab("Tickets & Sales", ticketsTab());
        if (isAdmin) {
            tabs.addTab("Users", usersTab());
            tabs.addTab("Messages", messagesTab());
        } else {
            tabs.addTab("My Inbox", myInboxTab());
        }
        add(tabs, BorderLayout.CENTER);

        try {
            loadTickets();
            if (isAdmin) { loadUsers(); loadMessages(); }
            else { loadMyMessages(); }
        } catch (Exception ex) { Main.msg(this, "Error loading data: " + ex.getMessage(), 0); }

        pack(); setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        Main.showMenu();
    }

    // =========================================================================
    // === REQUIRED DSA: SORTING ALGORITHM (SELECTION SORT) ===
    // Requirement 3: Replaces built-in Java sorting (Collections.sort) with a 
    // custom-implemented Selection Sort to handle the Admin Dashboard data grids.
    // =========================================================================
    private void selectionSortRecords(List<String[]> list, int fieldIndex, boolean isAscending) {
        for (int i = 0; i < list.size() - 1; i++) {
            int minMaxIndex = i;
            for (int j = i + 1; j < list.size(); j++) {
                String val1 = list.get(j)[fieldIndex];
                String val2 = list.get(minMaxIndex)[fieldIndex];
                
                int comparison;
                try {
                    // Try numeric comparison first (for Fare, IDs, etc)
                    comparison = Double.compare(Double.parseDouble(val1), Double.parseDouble(val2));
                } catch (NumberFormatException e) {
                    // Fallback to alphabetical String comparison
                    comparison = val1.compareToIgnoreCase(val2);
                }
                
                if (isAscending ? (comparison < 0) : (comparison > 0)) {
                    minMaxIndex = j;
                }
            }
            // Swap logic
            String[] temp = list.get(minMaxIndex);
            list.set(minMaxIndex, list.get(i));
            list.set(i, temp);
        }
    }
    // ==================== END OF SCREENSHOT FOR REQUIREMENT 3 ====================

    // =====================================================================
    //  UI Builders & Helpers
    // =====================================================================
    
    private JPanel flow(int align, Component... items) {
        JPanel p = new JPanel(new FlowLayout(align, 10, 5));
        p.setOpaque(false);
        for (Component c : items) p.add(c);
        return p;
    }

    private JPanel buildHeader() {
        JPanel header = Main.makeHeader("Back office");
        JLabel who = Main.label(me[Storage.NAME] + "  (" + me[Storage.ROLE] + ")", Font.BOLD, 14, Color.WHITE);
        JButton pwBtn = Main.btn("Change password", Main.GHOST, Color.WHITE, "Change password");
        JButton logoutBtn = Main.btn("Log out", Main.YELLOW, Main.TEXT, "Return to menu");

        pwBtn.addActionListener(e -> changePassword());
        logoutBtn.addActionListener(e -> dispose());

        header.add(flow(FlowLayout.RIGHT, who, pwBtn, logoutBtn), BorderLayout.EAST);
        return header;
    }

    private JPanel tab(String title, JComponent extra, JTable table, JComponent bottom, int... colWidths) {
        for (int i = 0; i < colWidths.length; i += 2) {
            table.getColumnModel().getColumn(colWidths[i]).setPreferredWidth(colWidths[i + 1]);
        }
        JPanel top = new JPanel(new BorderLayout(5, 14)); top.setOpaque(false);
        JLabel head = Main.label(title, Font.BOLD, 24, Main.TEXT);
        head.setHorizontalAlignment(SwingConstants.LEFT);

        top.add(head, BorderLayout.NORTH);
        top.add(extra, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        Theme.Card tableCard = new Theme.Card(new BorderLayout());
        tableCard.setPreferredSize(new Dimension(1200, 320));
        tableCard.add(scroll, BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Main.BACKGROUND); p.setBorder(new EmptyBorder(24, 28, 18, 28));
        p.add(top, BorderLayout.NORTH); p.add(tableCard, BorderLayout.CENTER); p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JLabel muted(String text) { return Main.label(text, Font.PLAIN, 13, Main.MUTED); }

    /** Action bar: routine actions on the left, record-changing actions on the right. */
    private JPanel bar(JComponent left, JComponent right) {
        JPanel b = new JPanel(new BorderLayout()); b.setOpaque(false);
        b.add(left, BorderLayout.WEST); b.add(right, BorderLayout.EAST);
        return b;
    }

    private JPanel tile(int i, String caption, boolean hero) {
        Theme.Card c = hero ? new Theme.Card(new GridLayout(2, 1, 0, 2), Main.DARK_GREEN, null, 14) : new Theme.Card(new GridLayout(2, 1, 0, 2));
        c.setBorder(new EmptyBorder(12, 16, 12, 16));
        caps[i] = Main.label(caption, Font.PLAIN, 12, hero ? Main.ON_DARK : Main.MUTED);
        caps[i].setHorizontalAlignment(SwingConstants.LEFT);
        sums[i].setFont(Main.font(Font.BOLD, 19));
        sums[i].setForeground(hero ? Color.WHITE : Main.TEXT);
        c.add(caps[i]); c.add(sums[i]);
        return c;
    }

    private JPanel sortRow(JComboBox<String> sort, JComboBox<String> order, Runnable refresh) {
        sort.addActionListener(e -> refresh.run()); order.addActionListener(e -> refresh.run());
        return flow(FlowLayout.LEFT, muted("Sort by"), sort, order);
    }

    private String[] selected(JTable t, List<String[]> list, String name) {
        int r = t.getSelectedRow();
        if (r < 0) { Main.msg(this, "Please select a " + name + " first.", 2); return null; }
        return list.get(r);
    }

    // =====================================================================
    //  Tickets Tab
    // =====================================================================
    private JPanel ticketsTab() {
        JPanel summary = new JPanel(new GridLayout(1, 6, 12, 0));
        summary.setOpaque(false);
        String[] names = {"Tickets sold", "Total sales", "Cash", "Beep card", "GCash / Maya", "Refunded"};
        for (int i = 0; i < 6; i++) summary.add(tile(i, names[i], i == 1));

        JButton searchBtn = Main.btn("Search", Main.GREEN, Color.WHITE, "Search tickets");
        JButton showAll = Main.btn("Show all", Main.GRAY, Main.TEXT, "Clear search");
        search.setColumns(18);
        search.setToolTipText("Search by ticket ID, station, passenger type, payment or status");
        search.addActionListener(e -> showTickets()); searchBtn.addActionListener(e -> showTickets());
        showAll.addActionListener(e -> { search.setText(""); showTickets(); });

        JPanel controls = new JPanel(new BorderLayout()); controls.setOpaque(false);
        controls.add(flow(FlowLayout.LEFT, muted("Search"), search, searchBtn, showAll), BorderLayout.WEST);
        controls.add(sortRow(sortBox, orderBox, this::showTickets), BorderLayout.EAST);

        JPanel extra = new JPanel(new BorderLayout(0, 14)); extra.setOpaque(false);
        extra.add(summary, BorderLayout.NORTH); extra.add(controls, BorderLayout.CENTER);

        JButton refresh = Main.btn("Refresh", Main.GRAY, Main.TEXT, "Reload"); refresh.setMnemonic('R');
        JButton export = Main.btn("Export report", Main.GRAY, Main.TEXT, "Save to file"); export.setMnemonic('E');
        JButton edit = Main.btn("Edit ticket", Main.GRAY, Main.TEXT, "Admin only");
        JButton refund = Main.btn("Refund ticket", Main.GREEN, Color.WHITE, "Refund");
        JButton voidBtn = Main.btn("Void ticket", Main.RED_SOFT, Main.RED, "Delete single");
        JButton clear = Main.btn("Clear all", Main.RED_SOFT, Main.RED, "Delete ALL");

        refresh.addActionListener(e -> loadTickets());
        export.addActionListener(e -> Main.msg(this, "Report saved to:\n" + Storage.exportReport(allTickets, me[Storage.NAME]), 1));
        edit.addActionListener(e -> editTicket());
        refund.addActionListener(e -> refundTicket());
        voidBtn.addActionListener(e -> voidTicket());
        clear.addActionListener(e -> clearTickets());

        JButton popStack = Main.btn("Last sold ticket", Main.GRAY, Main.TEXT, "Check Stack");
        popStack.addActionListener(e -> {
            if (Storage.sessionTicketHistory.isEmpty()) {
                Main.msg(this, "No tickets issued yet during this session.", 1);
            } else {
                String[] last = Storage.sessionTicketHistory.peek();
                Main.msg(this, "Last Ticket Sold (Stack Peek):\n\n" + Storage.receipt(last), 1);
            }
        });

        if (!isAdmin) { edit.setEnabled(false); voidBtn.setEnabled(false); clear.setEnabled(false); }

        JPanel bottom = new JPanel(new BorderLayout(0, 6)); bottom.setOpaque(false);
        bottom.add(bar(flow(FlowLayout.LEFT, refresh, export, popStack), flow(FlowLayout.RIGHT, edit, refund, voidBtn, clear)), BorderLayout.CENTER);
        JLabel file = Main.label("Data file: " + Storage.ticketsFilePath(), Font.PLAIN, 11, Main.MUTED);
        file.setHorizontalAlignment(SwingConstants.LEFT);
        file.setBorder(new EmptyBorder(0, 10, 0, 0));
        bottom.add(file, BorderLayout.SOUTH);

        return tab("Tickets & Sales", extra, ticketTable, bottom, 0, 45, 1, 95, 2, 195, 3, 150, 4, 150, 5, 120, 6, 65, 7, 135, 8, 180, 9, 125);
    }

    private void loadTickets() { allTickets = Storage.tickets(); showTickets(); }

    private void showTickets() {
        String key = search.getText().trim().toLowerCase();
        shown.clear();
        for (String[] t : allTickets) {
            String rowText = String.join(" ", t).toLowerCase();
            if (rowText.contains(key)) shown.add(t);
        }
        
        // Use custom Selection Sort (DSA Requirement)
        int f = SORT_FIELDS[sortBox.getSelectedIndex()];
        selectionSortRecords(shown, f, orderBox.getSelectedIndex() == 0);

        List<Object[]> rows = new ArrayList<>();
        for (String[] t : shown) {
            rows.add(new Object[]{rows.size() + 1, t[Storage.ID], t[Storage.TYPE], t[Storage.FROM], t[Storage.TO],
                t[Storage.PTYPE], Main.PESO + (int)Storage.fare(t), t[Storage.PAY], Storage.when(t[Storage.TIME]), t[Storage.STATUS]});
        }
        Main.fill(ticketTable, rows);

        double[] s = Storage.totals(shown);
        sums[0].setText(String.valueOf((int)s[0]));
        sums[1].setText(String.format("%s%,.2f", Main.PESO, s[1]));
        sums[2].setText(String.format("%s%,.2f", Main.PESO, s[2]));
        sums[3].setText(String.format("%s%,.2f", Main.PESO, s[3]));
        sums[4].setText(String.format("%s%,.2f", Main.PESO, s[4]));
        sums[5].setText(String.valueOf((int)s[5]));
        caps[5].setText(String.format("Refunded (%s%,.2f)", Main.PESO, s[6]));
    }

    private void editTicket() {
        String[] old = selected(ticketTable, shown, "ticket");
        if (old == null) return;
        if (Storage.isRefunded(old)) { Main.msg(this, "Cannot edit refunded tickets.", 2); return; }

        JTextField name = new JTextField(old[Storage.ID], 15), time = new JTextField(LocalDateTime.parse(old[Storage.TIME]).format(TIME_FMT), 15);
        JComboBox<String> from = new JComboBox<>(Storage.STATIONS), to = new JComboBox<>(Storage.STATIONS);
        JComboBox<String> pay = new JComboBox<>(Storage.PAYMENTS), type = new JComboBox<>(Storage.PASSENGERS);
        
        from.setSelectedItem(old[Storage.FROM]); to.setSelectedItem(old[Storage.TO]);
        pay.setSelectedItem(old[Storage.PAY]); type.setSelectedItem(old[Storage.PTYPE]);

        Object[] fields = {"ID:", name, "From:", from, "To:", to, "Time (yyyy-MM-dd HH:mm):", time, "Payment:", pay, "Type:", type};
        
        if (JOptionPane.showConfirmDialog(this, fields, "Edit Ticket", JOptionPane.OK_CANCEL_OPTION) == 0) {
            try {
                if (from.getSelectedItem().equals(to.getSelectedItem())) throw new Exception("Stations must be different.");
                LocalDateTime dt = LocalDateTime.parse(time.getText().trim(), TIME_FMT);
                
                String[] updated = Storage.ticket(name.getText().trim(), (String)from.getSelectedItem(), (String)to.getSelectedItem(), 
                                        (String)pay.getSelectedItem(), (String)type.getSelectedItem(), dt, false);
                Storage.replaceTicket(old[Storage.ID], updated);
                loadTickets();
                Main.msg(this, "Ticket updated successfully.", 1);
            } catch (Exception ex) { Main.msg(this, "Error editing: " + ex.getMessage(), 0); }
        }
    }

    private void refundTicket() {
        String[] t = selected(ticketTable, shown, "ticket");
        if (t == null) return;
        if (Storage.isRefunded(t)) { Main.msg(this, "Already refunded.", 1); return; }

        int amt = (int)Storage.fare(t);
        if (JOptionPane.showConfirmDialog(this, "Refund ticket " + t[Storage.ID] + " for " + Main.PESO + amt + "?", "Confirm", 0) == 0) {
            Storage.refund(t[Storage.ID], false);
            loadTickets();
            Main.msg(this, "Refund successful. Return " + Main.PESO + amt + " via " + t[Storage.PAY], 1);
        }
    }

    private void voidTicket() {
        String[] t = selected(ticketTable, shown, "ticket");
        if (t != null && confirmPw("void ticket") && JOptionPane.showConfirmDialog(this, "Void ticket?", "Confirm", 0) == 0) {
            allTickets.remove(t); Storage.saveTickets(allTickets); loadTickets();
        }
    }

    private void clearTickets() {
        if (confirmPw("clear all") && JOptionPane.showConfirmDialog(this, "Delete ALL tickets?", "Confirm", 0) == 0) {
            allTickets.clear(); Storage.saveTickets(allTickets); loadTickets();
        }
    }

    // =====================================================================
    //  Users Tab
    // =====================================================================
    private JPanel usersTab() {
        JButton add = Main.btn("Add user", Main.GREEN, Color.WHITE, ""); add.addActionListener(e -> addUser());
        JButton role = Main.btn("Change role", Main.GRAY, Main.TEXT, ""); role.addActionListener(e -> changeRole());
        JButton reset = Main.btn("Reset password", Main.GRAY, Main.TEXT, "");
        reset.addActionListener(e -> { String[] u = selected(userTable, users, "user"); if (u != null) resetPw(u[Storage.NAME]); });

        JButton unblock = Main.btn("Unblock", Main.GRAY, Main.TEXT, ""); unblock.addActionListener(e -> unblockUser());
        JButton del = Main.btn("Delete user", Main.RED_SOFT, Main.RED, ""); del.addActionListener(e -> delUser());
        JButton ref = Main.btn("Refresh", Main.GRAY, Main.TEXT, ""); ref.addActionListener(e -> loadUsers());

        return tab("User Accounts", sortRow(userSortBox, userOrderBox, this::showUsers), userTable,
                   bar(flow(FlowLayout.LEFT, add, ref), flow(FlowLayout.RIGHT, role, reset, unblock, del)));
    }

    private void loadUsers() { users = Storage.users(); showUsers(); }

    private void showUsers() {
        int f = new int[]{Storage.NAME, Storage.ROLE, Storage.BLOCKED}[userSortBox.getSelectedIndex()];
        
        // Use custom Selection Sort (DSA Requirement)
        selectionSortRecords(users, f, userOrderBox.getSelectedIndex() == 0);
        
        List<Object[]> rows = new ArrayList<>();
        for (String[] u : users) rows.add(new Object[]{rows.size() + 1, u[Storage.NAME], u[Storage.ROLE], Storage.isBlocked(u) ? "BLOCKED" : "Active"});
        Main.fill(userTable, rows);
    }

    private void addUser() {
        JTextField n = new JTextField(15); JPasswordField p = new JPasswordField(15), c = new JPasswordField(15);
        JComboBox<String> r = new JComboBox<>(new String[]{"Staff", "Admin"});
        
        JLabel criteria = Main.label("Requires 6+ chars, 1 letter, 1 number, no spaces.", Font.ITALIC, 11, Main.MUTED);
        Object[] fields = {"Username:", n, "Password:", p, criteria, "Confirm:", c, "Role:", r};
        
        if (JOptionPane.showConfirmDialog(this, fields, "Add User", 2) == 0) {
            try {
                Storage.addUser(n.getText().trim(), new String(p.getPassword()), new String(c.getPassword()), (String)r.getSelectedItem());
                loadUsers(); Main.msg(this, "User added.", 1);
            } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
        }
    }

    private void changeRole() {
        String[] u = selected(userTable, users, "user");
        if (u != null && !u[Storage.NAME].equalsIgnoreCase(me[Storage.NAME])) {
            Storage.changeRole(u[Storage.NAME], Storage.isAdmin(u) ? "Staff" : "Admin"); loadUsers();
        }
    }

    private boolean resetPw(String name) {
        if (!confirmPw("reset password")) return false;
        String temp = Storage.resetPassword(name);
        if (name.equalsIgnoreCase(me[Storage.NAME])) me = Storage.findUser(name);
        loadUsers();
        Main.msg(this, "Temporary password for " + name + ":\n" + temp, 1);
        return true;
    }

    private void unblockUser() {
        String[] u = selected(userTable, users, "user");
        if (u != null && Storage.isBlocked(u)) { Storage.unblockUser(u[Storage.NAME]); loadUsers(); }
    }

    private void delUser() {
        String[] u = selected(userTable, users, "user");
        if (u != null && !u[Storage.NAME].equalsIgnoreCase(me[Storage.NAME]) && confirmPw("delete user")) {
            Storage.deleteUser(u[Storage.NAME]); loadUsers();
        }
    }

    // =====================================================================
    //  Messages Tab (Admin) & My Inbox (Staff)
    // =====================================================================
    private JPanel messagesTab() {
        JButton compose = Main.btn("Compose", Main.GREEN, Color.WHITE, "Send a new message");
        compose.addActionListener(e -> composeMessage(true));

        JButton reply = Main.btn("Reply", Main.GRAY, Main.TEXT, "Reply to user");
        reply.addActionListener(e -> replyToMsg());
        JButton done = Main.btn("Mark done", Main.GRAY, Main.TEXT, ""); done.addActionListener(e -> markMsgDone());
        JButton reset = Main.btn("Reset sender's password", Main.GRAY, Main.TEXT, "");
        reset.addActionListener(e -> { String[] m = selected(msgTable, messages, "msg"); if (m != null && resetPw(m[3])) markMsgDone(); });

        JButton del = Main.btn("Delete", Main.RED_SOFT, Main.RED, "");
        del.addActionListener(e -> { String[] m = selected(msgTable, messages, "msg"); if (m != null && confirmPw("delete")) { Storage.deleteMessage(m[0]); loadMessages(); }});

        JButton ref = Main.btn("Refresh", Main.GRAY, Main.TEXT, ""); ref.addActionListener(e -> loadMessages());

        return tab("Admin Messages", sortRow(msgSortBox, msgOrderBox, this::showMessages), msgTable,
                   bar(flow(FlowLayout.LEFT, compose, ref), flow(FlowLayout.RIGHT, reply, done, reset, del)), 1, 140, 4, 320);
    }

    private void loadMessages() { messages = Storage.messages(); showMessages(); }

    private void showMessages() {
        int f = new int[]{1, 2, 3, 5}[msgSortBox.getSelectedIndex()];
        
        // Use custom Selection Sort (DSA Requirement)
        selectionSortRecords(messages, f, msgOrderBox.getSelectedIndex() == 1);
        
        List<Object[]> rows = new ArrayList<>(); int open = 0;
        for (String[] m : messages) {
            rows.add(new Object[]{rows.size() + 1, Storage.when(m[1]), Storage.typeName(m), m[3], m[4], m[5]});
            if ("Open".equals(m[5])) open++;
        }
        Main.fill(msgTable, rows);
        if (isAdmin) tabs.setTitleAt(2, open > 0 ? "Messages (" + open + " new)" : "Messages");
    }

    private void composeMessage(boolean isAdminCheck) {
        JTextArea msgArea = new JTextArea(4, 20);
        
        if (isAdminCheck) {
            List<String> names = new ArrayList<>();
            for (String[] u : users) {
                if (!u[Storage.NAME].equalsIgnoreCase(me[Storage.NAME])) {
                    names.add(u[Storage.NAME]);
                }
            }
            JComboBox<String> userBox = new JComboBox<>(names.toArray(new String[0]));
            Object[] fields = {"Send to:", userBox, "Message:", new JScrollPane(msgArea)};
            
            if (JOptionPane.showConfirmDialog(this, fields, "Compose Message", JOptionPane.OK_CANCEL_OPTION) == 0) {
                try {
                    String target = (String) userBox.getSelectedItem();
                    if (target == null) throw new Exception("No user selected.");
                    Storage.addMessage("REPLY", target, msgArea.getText()); // Routed to the Staff member's inbox
                    loadMessages();
                    Main.msg(this, "Message sent to " + target, 1);
                } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
            }
        } else {
            Object[] fields = {"Message to Admin:", new JScrollPane(msgArea)};
            if (JOptionPane.showConfirmDialog(this, fields, "Contact Admin", JOptionPane.OK_CANCEL_OPTION) == 0) {
                try {
                    Storage.addMessage("CONTACT", me[Storage.NAME], msgArea.getText());
                    loadMyMessages();
                    Main.msg(this, "Message sent to Admin.", 1);
                } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
            }
        }
    }

    private void replyToMsg() {
        String[] m = selected(msgTable, messages, "msg");
        if (m != null) {
            JTextArea msgArea = new JTextArea(4, 20);
            Object[] fields = {"Reply to " + m[3] + ":", new JScrollPane(msgArea)};
            if (JOptionPane.showConfirmDialog(this, fields, "Reply", JOptionPane.OK_CANCEL_OPTION) == 0) {
                try {
                    Storage.addMessage("REPLY", m[3], msgArea.getText());
                    Storage.setMessageStatus(m[0], "Done");
                    loadMessages();
                    Main.msg(this, "Reply sent to " + m[3], 1);
                } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
            }
        }
    }

    private JPanel myInboxTab() {
        JButton compose = Main.btn("Compose", Main.GREEN, Color.WHITE, "Send message to Admin");
        compose.addActionListener(e -> composeMessage(false));

        JButton ref = Main.btn("Refresh", Main.GRAY, Main.TEXT, ""); ref.addActionListener(e -> loadMyMessages());
        JPanel none = new JPanel(); none.setOpaque(false);
        return tab("My Inbox", none, myMsgTable, bar(flow(FlowLayout.LEFT, compose, ref), new JPanel() {{ setOpaque(false); }}), 1, 150, 2, 450);
    }

    private void loadMyMessages() {
        List<String[]> myMsgs = new ArrayList<>();
        for (String[] m : Storage.messages()) {
            if ("REPLY".equals(m[2]) && me[Storage.NAME].equalsIgnoreCase(m[3])) {
                myMsgs.add(m);
            }
        }
        List<Object[]> rows = new ArrayList<>();
        for (String[] m : myMsgs) {
            rows.add(new Object[]{rows.size() + 1, Storage.when(m[1]), "Admin", m[4]});
        }
        Main.fill(myMsgTable, rows);
    }

    private void markMsgDone() {
        String[] m = selected(msgTable, messages, "msg");
        if (m != null) { Storage.setMessageStatus(m[0], "Done"); loadMessages(); }
    }

    private boolean confirmPw(String action) {
        JPasswordField pw = new JPasswordField(12);
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Admin password required to " + action + ":", pw}, "Security", 2) == 0) {
            if (me[Storage.PASS].equals(new String(pw.getPassword()))) return true;
            Main.msg(this, "Wrong password.", 0);
        }
        return false;
    }

    private void changePassword() {
        JPasswordField old = new JPasswordField(15), newP = new JPasswordField(15), c = new JPasswordField(15);
        
        JLabel criteria = Main.label("Requires 6+ chars, 1 letter, 1 number, no spaces.", Font.ITALIC, 11, Main.MUTED);
        Object[] fields = {"Current:", old, "New:", newP, criteria, "Confirm:", c};
        
        if (JOptionPane.showConfirmDialog(this, fields, "Change Password", 2) == 0) {
            try {
                Storage.changePassword(me[Storage.NAME], new String(old.getPassword()), new String(newP.getPassword()), new String(c.getPassword()));
                me = Storage.findUser(me[Storage.NAME]);
                Main.msg(this, "Password changed.", 1);
            } catch (Exception ex) { Main.msg(this, ex.getMessage(), 0); }
        }
    }
}