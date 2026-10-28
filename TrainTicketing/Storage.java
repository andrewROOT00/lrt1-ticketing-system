import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

public class Storage {

    // ---------------- Shared LRT-1 Data ----------------
    public static final String[] STATIONS = {
        "Fernando Poe Jr.", "Balintawak", "Yamaha Monumento", "5th Avenue", "R. Papa", "Abad Santos", "Blumentritt", 
        "Tayuman", "Bambang", "Doroteo Jose", "Carriedo", "Central Terminal", "United Nations", "Pedro Gil", 
        "Quirino", "Vito Cruz", "Gil Puyat", "Libertad", "EDSA", "Baclaran", "Redemptorist-Aseana", "MIA Road", 
        "PITX", "Ninoy Aquino Avenue", "Dr. Santos"
    };
    
    private static final double[] FARE_FROM_START = {
        16.25, 19, 22, 24, 25, 26, 28, 29, 30, 30, 31, 33, 34, 35, 37, 38, 39, 40, 42, 43, 45, 46, 48, 50, 52
    };

    public static final String[] PAYMENTS = {"Cash", "Beep Card", "GCash / Maya"};
    public static final String[] PASSENGERS = {"Regular", "Senior Citizen", "PWD"};
    public static final int REFUND_MINUTES = 30;

    // Array Positions
    public static final int ID = 0, TYPE = 1, FROM = 2, TO = 3, FARE = 4, PAY = 5, TIME = 6, PTYPE = 7, STATUS = 8; 
    public static final int NAME = 0, PASS = 1, ROLE = 2, BLOCKED = 3; 

    private static final Path USERS = Paths.get("users.txt"), TICKETS = Paths.get("tickets.txt"),
            BEEP = Paths.get("beep_balance.txt"), MESSAGES = Paths.get("messages.txt");
    private static final DateTimeFormatter SHOW = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    // =========================================================================
    // === REQUIRED DSA: LINEAR DATA STRUCTURE (STACK) ===
    // Requirement 1: Stack is used here to maintain a history of tickets 
    // issued during the current application session (Last-In-First-Out).
    // =========================================================================
    public static Stack<String[]> sessionTicketHistory = new Stack<>();
    // ==================== END OF SCREENSHOT FOR REQUIREMENT 1 (DECLARATION) ====================

    // ================== FILE HELPERS ==================

    private static List<String[]> read(Path file, int... validLengths) {
        List<String[]> rows = new ArrayList<>();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file)) {
                    String[] p = line.trim().split("\\|");
                    for (int n : validLengths) if (p.length == n) { rows.add(p); break; }
                }
            }
        } catch (Exception ignored) {} 
        return rows;
    }

    private static void write(Path file, List<String[]> rows) {
        try {
            Files.write(file, rows.stream().map(r -> String.join("|", r)).collect(Collectors.toList()));
        } catch (Exception e) { throw new RuntimeException("Could not save " + file, e); }
    }

    // =========================================================================
    // === REQUIRED DSA: SEARCHING ALGORITHM (LINEAR SEARCH) ===
    // Requirement 2: This manual linear search replaces built-in Java 
    // methods (like Streams) to find a specific record by its Primary Key.
    // =========================================================================
    public static String[] find(List<String[]> rows, String key) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)[0].equalsIgnoreCase(key.trim())) {
                return rows.get(i);
            }
        }
        return null; // Return null if not found
    }
    

    private static String[] need(List<String[]> rows, String key, String errorMsg) {
        String[] r = find(rows, key);
        if (r == null) throw new IllegalArgumentException(errorMsg);
        return r;
    }

    public static String when(String isoTime) { return LocalDateTime.parse(isoTime).format(SHOW); }

    // ================== USERS ==================

    public static List<String[]> users() {
        List<String[]> list = read(USERS, 4);
        for (String[] u : list) if (!"Admin".equals(u[ROLE])) u[ROLE] = "Staff";
        if (list.isEmpty()) { 
            list.add(new String[]{"admin", "admin123", "Admin", "false"}); 
            write(USERS, list); 
        }
        return list;
    }

    public static String[] findUser(String name) { return find(users(), name); }
    public static boolean isAdmin(String[] u) { return "Admin".equals(u[ROLE]); }
    public static boolean isBlocked(String[] u) { return Boolean.parseBoolean(u[BLOCKED]); }

    private static void checkUserPass(String n, String p) {
        if (n == null || !n.matches("\\w{4,15}")) throw new IllegalArgumentException("Username must be 4-15 letters, numbers, or _.");
        if (p == null || p.length() < 6 || !p.matches(".*[a-zA-Z].*") || !p.matches(".*\\d.*") || p.contains(" ") || p.contains("|")) 
            throw new IllegalArgumentException("Password must be 6+ chars, contain a letter and number, with no spaces.");
    }

    public static String[] login(String name, String pass) {
        String[] u = findUser(name);
        if (u == null || !u[PASS].equals(pass)) throw new IllegalArgumentException("Invalid username or password.");
        if (isBlocked(u)) throw new IllegalArgumentException("Account BLOCKED. Contact Admin.");
        return u;
    }

    public static void addUser(String name, String pass, String confirm, String role) {
        checkUserPass(name, pass);
        if (!pass.equals(confirm)) throw new IllegalArgumentException("Passwords do not match.");
        List<String[]> all = users();
        if (find(all, name) != null) throw new IllegalArgumentException("Username taken.");
        all.add(new String[]{name, pass, role, "false"});
        write(USERS, all);
    }

    public static void changePassword(String name, String oldPw, String newPw, String confirm) {
        List<String[]> all = users();
        String[] u = need(all, name, "User not found.");
        if (!u[PASS].equals(oldPw)) throw new IllegalArgumentException("Current password incorrect.");
        checkUserPass("dummy", newPw); 
        if (!newPw.equals(confirm)) throw new IllegalArgumentException("New passwords do not match.");
        u[PASS] = newPw;
        write(USERS, all);
    }

    public static void changeRole(String name, String role) {
        List<String[]> all = users(); String[] u = need(all, name, "User not found.");
        if (isAdmin(u) && !"Admin".equals(role) && all.stream().filter(Storage::isAdmin).count() == 1) 
            throw new IllegalArgumentException("Cannot demote the last Admin.");
        u[ROLE] = role; write(USERS, all);
    }

    public static void blockUser(String name) {
        List<String[]> all = users(); String[] u = find(all, name);
        if (u != null && !isAdmin(u)) { u[BLOCKED] = "true"; write(USERS, all); }
    }

    public static void unblockUser(String name) {
        List<String[]> all = users(); need(all, name, "User not found.")[BLOCKED] = "false"; write(USERS, all);
    }

    public static void deleteUser(String name) {
        List<String[]> all = users(); String[] u = need(all, name, "User not found.");
        if (isAdmin(u) && all.stream().filter(Storage::isAdmin).count() == 1) throw new IllegalArgumentException("Cannot delete last Admin.");
        all.remove(u); write(USERS, all);
    }

    public static String resetPassword(String name) {
        List<String[]> all = users(); String[] u = need(all, name, "User not found.");
        u[PASS] = "Temp" + (1000 + new Random().nextInt(9000));
        u[BLOCKED] = "false"; write(USERS, all);
        return u[PASS];
    }

    // ================== FARES and TICKETS ==================

    public static double fare(String from, String to, String pay, String type) {
        int a = Arrays.asList(STATIONS).indexOf(from), b = Arrays.asList(STATIONS).indexOf(to);
        double raw = (a >= 0 && b >= 0) ? 16.25 + Math.abs(FARE_FROM_START[a] - FARE_FROM_START[b]) : 0;
        double base = ("Beep Card".equals(pay) || "GCash / Maya".equals(pay)) ? Math.round(raw) : Math.max(20, Math.min(55, Math.ceil(raw / 5.0) * 5.0));
        return Math.round(base * ("Regular".equals(type) ? 1.0 : 0.80)); 
    }

    public static double fare(String[] t) { return Double.parseDouble(t[FARE]); }
    public static boolean isRefunded(String[] t) { return "Refunded".equals(t[STATUS]); }

    public static String[] ticket(String id, String from, String to, String pay, String type, LocalDateTime time, boolean ref) {
        String ticketType = "Beep Card".equals(pay) ? "Beep Card (Stored Value)" : 
                            "GCash / Maya".equals(pay) ? "QR Code Ticket" : "Single Journey Ticket";
        return new String[]{id, ticketType, from, to, String.valueOf(fare(from, to, pay, type)), pay, time.toString(), type, ref ? "Refunded" : "Active"};
    }

    public static List<String[]> tickets() {
        List<String[]> list = new ArrayList<>();
        for (String[] p : read(TICKETS, 7, 9)) {
            try { list.add(ticket(p[0], p[2], p[3], p[5], p.length == 9 ? p[7] : "Regular", LocalDateTime.parse(p[6]), p.length == 9 && "Refunded".equals(p[8]))); } 
            catch (Exception ignored) {}
        }
        return list;
    }

    public static void saveTickets(List<String[]> tickets) { write(TICKETS, tickets); }
    public static String ticketsFilePath() { return TICKETS.toAbsolutePath().toString(); }

    public static String[] issueTicket(String from, String to, String pay, String type) {
        List<String[]> all = tickets();
        int maxId = all.stream().filter(t -> t[ID].startsWith("LRT-"))
                                .mapToInt(t -> Integer.parseInt(t[ID].substring(4)))
                                .max().orElse(1000);
        String[] t = ticket("LRT-" + (maxId + 1), from, to, pay, type, LocalDateTime.now(), false);
        all.add(t); 
        saveTickets(all);
        
        // Push the new ticket to the required DSA Stack
        sessionTicketHistory.push(t);
        
        
        return t;
    }

    public static String[] refund(String id, boolean fromKiosk) {
        List<String[]> all = tickets(); String[] t = need(all, id, "Ticket not found.");
        if (isRefunded(t)) throw new IllegalArgumentException("Already refunded.");
        if (fromKiosk && LocalDateTime.parse(t[TIME]).plusMinutes(REFUND_MINUTES).isBefore(LocalDateTime.now())) 
            throw new IllegalArgumentException("Kiosk refunds only valid within " + REFUND_MINUTES + " mins of purchase. Contact Admin.");
        
        t[STATUS] = "Refunded"; saveTickets(all);
        if ("Beep Card".equals(t[PAY])) setBeepBalance(beepBalance() + (int)fare(t));
        return t;
    }

    public static void replaceTicket(String oldId, String[] newT) {
        List<String[]> all = tickets();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i)[ID].equals(oldId)) { all.set(i, newT); saveTickets(all); return; }
        }
        throw new IllegalArgumentException("Ticket not found.");
    }

    public static double[] totals(List<String[]> tickets) {
        double[] s = new double[7];
        for (String[] t : tickets) {
            if (isRefunded(t)) { s[5]++; s[6] += fare(t); } 
            else { 
                s[0]++; 
                s[1] += fare(t); 
                if ("Beep Card".equals(t[PAY])) s[3] += fare(t);
                else if ("GCash / Maya".equals(t[PAY])) s[4] += fare(t);
                else s[2] += fare(t);
            }
        }
        return s;
    }

    public static String receipt(String[] t) {
        double rate = "Regular".equals(t[PTYPE]) ? 0.0 : 0.20;
        String text = String.format("====== LRT-1 TICKET ======\nTicket ID : %s\nType      : %s\nFrom      : %s\nTo        : %s\nPassenger : %s\n", t[ID], t[TYPE], t[FROM], t[TO], t[PTYPE]);
        if (rate > 0) text += String.format("Base fare : \u20B1%.2f\nDiscount  : 20%%\n", fare(t[FROM], t[TO], t[PAY], "Regular"));
        text += String.format("FARE      : \u20B1%.2f\nPayment   : %s\nIssued    : %s\n", fare(t), t[PAY], when(t[TIME]));
        return text + (isRefunded(t) ? "*** REFUNDED ***\n" : "") + "==========================";
    }

    public static String exportReport(List<String[]> tickets, String user) {
        Path file = Paths.get("sales_report.txt");
        List<String> lines = new ArrayList<>(Arrays.asList("LRT-1 SALES REPORT", "Generated by: " + user, "Date: " + LocalDateTime.now(), "-----------------------------------------------"));
        for (String[] t : tickets) lines.add(String.format("%s | %s -> %s | %s | PHP %.2f | %s | %s", t[ID], t[FROM], t[TO], t[PTYPE], fare(t), t[PAY], t[STATUS]));
        double[] s = totals(tickets);
        lines.addAll(Arrays.asList("-----------------------------------------------", "Tickets sold   : " + (int)s[0], 
            String.format("Cash sales     : PHP %.2f", s[2]), String.format("Beep sales     : PHP %.2f", s[3]), 
            String.format("GCash/Maya     : PHP %.2f", s[4]), 
            String.format("TOTAL SALES    : PHP %.2f", s[1]), String.format("Refunded       : %d ticket(s), PHP %.2f", (int)s[5], s[6])));
        
        try { Files.write(file, lines); return file.toAbsolutePath().toString(); } 
        catch (Exception e) { throw new RuntimeException("Could not save report.", e); }
    }

    // ================== BEEP CARD ==================

    public static int beepBalance() {
        try { return Integer.parseInt(new String(Files.readAllBytes(BEEP)).trim()); } 
        catch (Exception e) { return 100; }
    }

    public static void setBeepBalance(int bal) { write(BEEP, Collections.singletonList(new String[]{String.valueOf(bal)})); }

    // ================== MESSAGES ==================

    public static List<String[]> messages() {
        List<String[]> list = read(MESSAGES, 6); list.removeIf(m -> !m[0].matches("\\d+")); return list;
    }

    public static String typeName(String[] m) { return "RESET".equals(m[2]) ? "Password reset" : "REPLY".equals(m[2]) ? "Reply" : "Contact"; }

    public static void requestPasswordReset(String name) {
        if (findUser(name != null ? name.trim() : "") == null) throw new IllegalArgumentException("Account not found.");
        addMessage("RESET", name.trim(), "Please reset my password.");
    }

    public static void addMessage(String type, String name, String text) {
        String clean = text == null ? "" : text.replace("|", "/").replace("\n", " ").trim();
        if (clean.isEmpty() || clean.length() > 200) throw new IllegalArgumentException("Message must be 1-200 characters.");
        
        List<String[]> all = messages();
        int nextId = all.stream().mapToInt(m -> Integer.parseInt(m[0])).max().orElse(0) + 1;
        
        all.add(new String[]{String.valueOf(nextId), LocalDateTime.now().toString(), type, name == null || name.isEmpty() ? "(not given)" : name.replace("|", "/").trim(), clean, "Open"});
        write(MESSAGES, all);
    }

    public static void setMessageStatus(String id, String status) {
        List<String[]> all = messages(); String[] m = find(all, id);
        if (m != null) { m[5] = status; write(MESSAGES, all); }
    }

    public static void deleteMessage(String id) {
        List<String[]> all = messages(); all.remove(find(all, id)); write(MESSAGES, all);
    }
}