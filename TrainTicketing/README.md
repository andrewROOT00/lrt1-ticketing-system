# LRT-1 Ticketing System

A Java Swing desktop app that simulates an LRT-1 ticket kiosk (passengers) and a back office (staff / admin).

- **Passenger kiosk** – buy single-journey / QR tickets, pay with cash, Beep Card or GCash/Maya, load a Beep Card, request refunds
- **Staff / Admin back office** – manage tickets, users, messages, and export sales reports
- **Storage** – plain-text files (`users.txt`, `tickets.txt`, `beep_balance.txt`, `messages.txt`)
- **DSA used** – Stack (session ticket history), Linear Search (`Storage.find`), Selection Sort (`AdminUI.selectionSortRecords`)

## How to run

```bash
javac *.java
java Main
```

Default admin account (created on first run): `admin` / `admin123`

## UML Class Diagram

```mermaid
classDiagram
    direction TB

    class JFrame {
        <<javax.swing>>
    }

    class Main {
        +PESO$ String
        +GREEN$ Color
        +DARK_GREEN$ Color
        -attempts int
        -backToMenu boolean
        -userField JTextField
        -passField JPasswordField
        +main(String[] args)$ void
        +showMenu()$ void
        +font(int style, int size)$ Font
        +mono(int size)$ Font
        +btn(String txt, Color bg, Color fg, String tip)$ JButton
        +link(String txt, String tip)$ JButton
        +label(String txt, int style, int size, Color c)$ JLabel
        +title(String text)$ Border
        +makeHeader(String sub)$ JPanel
        +msg(Component c, String txt, int type)$ void
        +table(String... cols)$ JTable
        +fill(JTable t, List rows)$ void
        +dispose() void
        -doLogin() void
        -forgotPassword() void
    }

    class MenuWindow {
        -choice(String name, String blurb, JButton action) JPanel
    }

    class AdminUI {
        -me String[]
        -isAdmin boolean
        -tabs JTabbedPane
        -allTickets List
        -users List
        -messages List
        -ticketTable JTable
        -userTable JTable
        -msgTable JTable
        +AdminUI(String[] user)
        +dispose() void
        -selectionSortRecords(List list, int fieldIndex, boolean isAscending) void
        -ticketsTab() JPanel
        -usersTab() JPanel
        -messagesTab() JPanel
        -myInboxTab() JPanel
        -editTicket() void
        -refundTicket() void
        -voidTicket() void
        -clearTickets() void
        -addUser() void
        -changeRole() void
        -resetPw(String name) boolean
        -unblockUser() void
        -delUser() void
        -composeMessage(boolean isAdminCheck) void
        -replyToMsg() void
        -markMsgDone() void
        -confirmPw(String action) boolean
        -changePassword() void
    }

    class PassengerUI {
        -STATIONS$ String[]
        -MONEY$ int[]
        -LOADS$ int[]
        -beepBalance int
        -origin int
        -dest int
        -qtys int[]
        -dueCash int
        -dueBeep int
        -dueQr int
        -cashInserted int
        -screen String
        -pendingReceipt String
        -clockTimer Timer
        -printTimer Timer
        +PassengerUI()
        +dispose() void
        -homeScreen() JPanel
        -destScreen() JPanel
        -qtyScreen() JPanel
        -payScreen() JPanel
        -doneScreen() JPanel
        -drawMap() void
        -chooseDestination(int index) void
        -changeQuantity(int idx, int change) void
        -goToPayment() void
        -insertMoney(int value) void
        -insertLoadMoney(int value) void
        -completePurchase(int payIdx) void
        -refundCash() void
        -refundAtKiosk() void
        -fareEach(int payIdx, int typeIdx) int
        -totalFare(int payIdx) int
    }

    class QrIcon {
        -N$ int
        -size int
        -m boolean[][]
        +paintIcon(Component c, Graphics g, int x, int y) void
    }

    class Storage {
        +STATIONS$ String[]
        +PAYMENTS$ String[]
        +PASSENGERS$ String[]
        +REFUND_MINUTES$ int
        +sessionTicketHistory$ Stack
        -read(Path file, int... validLengths)$ List
        -write(Path file, List rows)$ void
        +find(List rows, String key)$ String[]
        +users()$ List
        +findUser(String name)$ String[]
        +login(String name, String pass)$ String[]
        +addUser(String name, String pass, String confirm, String role)$ void
        +changePassword(String name, String oldPw, String newPw, String confirm)$ void
        +changeRole(String name, String role)$ void
        +blockUser(String name)$ void
        +unblockUser(String name)$ void
        +deleteUser(String name)$ void
        +resetPassword(String name)$ String
        +isAdmin(String[] u)$ boolean
        +isBlocked(String[] u)$ boolean
        +fare(String from, String to, String pay, String type)$ double
        +ticket(String id, String from, String to, String pay, String type, LocalDateTime time, boolean ref)$ String[]
        +tickets()$ List
        +saveTickets(List tickets)$ void
        +issueTicket(String from, String to, String pay, String type)$ String[]
        +refund(String id, boolean fromKiosk)$ String[]
        +replaceTicket(String oldId, String[] newT)$ void
        +totals(List tickets)$ double[]
        +receipt(String[] t)$ String
        +exportReport(List tickets, String user)$ String
        +beepBalance()$ int
        +setBeepBalance(int bal)$ void
        +messages()$ List
        +requestPasswordReset(String name)$ void
        +addMessage(String type, String name, String text)$ void
        +setMessageStatus(String id, String status)$ void
        +deleteMessage(String id)$ void
    }

    class Theme {
        <<final utility>>
        -Theme()
        +install()$ void
        +badge(String text)$ JLabel
        ~mix(Color a, Color b, double t)$ Color
        ~alpha(Color c, int a)$ Color
        ~luma(Color c)$ double
        ~smooth(Graphics g)$ Graphics2D
    }

    %% Inheritance
    JFrame <|-- Main
    JFrame <|-- MenuWindow
    JFrame <|-- AdminUI
    JFrame <|-- PassengerUI

    %% Nested classes
    Main *-- MenuWindow : nested
    PassengerUI *-- QrIcon : nested

    %% Screen navigation
    Main ..> MenuWindow : showMenu() creates
    MenuWindow ..> PassengerUI : opens kiosk
    MenuWindow ..> Main : opens staff login
    Main ..> AdminUI : opens after login
    AdminUI ..> Main : showMenu() on logout
    PassengerUI ..> Main : showMenu() on close

    %% Data access
    Main ..> Storage : login, blockUser, requestPasswordReset
    AdminUI ..> Storage : tickets, users, messages
    PassengerUI ..> Storage : fares, tickets, beep balance

    %% Look and feel
    Main ..> Theme : install, Card, RoundedBorder
    AdminUI ..> Theme : Card
    PassengerUI ..> Theme : Card, RoundedBorder
    Theme ..> Main : colours and fonts
```

## Theme component classes

```mermaid
classDiagram
    direction LR

    class Theme {
        <<final utility>>
        +install()$ void
        +badge(String text)$ JLabel
    }

    class Card {
        -fill Color
        -line Color
        -arc int
        #paintComponent(Graphics g) void
        #paintChildren(Graphics g) void
        +paint(Graphics g) void
    }

    class RoundedBorder {
        -color Color
        -arc int
        -pad Insets
        +paintBorder(Component c, Graphics g, int x, int y, int w, int h) void
        +getBorderInsets(Component c) Insets
    }

    class GlyphIcon {
        -fill Color
        -ink Color
        -glyph String
        +paintIcon(Component c, Graphics g, int x, int y) void
    }

    class CheckIcon {
        +paintIcon(Component c, Graphics g, int x, int y) void
    }

    class ModernButtonUI {
        +createUI(JComponent c)$ ComponentUI
        +paint(Graphics g, JComponent c) void
    }

    class ModernComboBoxUI {
        +createUI(JComponent c)$ ComponentUI
        #createArrowButton() JButton
        #createPopup() ComboPopup
    }

    class ModernScrollBarUI {
        +createUI(JComponent c)$ ComponentUI
        #paintThumb(Graphics g, JComponent c, Rectangle r) void
    }

    class ModernTabbedPaneUI {
        +createUI(JComponent c)$ ComponentUI
        #paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) void
    }

    class JPanel
    class AbstractBorder
    class Icon {
        <<interface>>
    }
    class BasicButtonUI
    class BasicComboBoxUI
    class BasicScrollBarUI
    class BasicTabbedPaneUI

    JPanel <|-- Card
    AbstractBorder <|-- RoundedBorder
    Icon <|.. GlyphIcon
    Icon <|.. CheckIcon
    BasicButtonUI <|-- ModernButtonUI
    BasicComboBoxUI <|-- ModernComboBoxUI
    BasicScrollBarUI <|-- ModernScrollBarUI
    BasicTabbedPaneUI <|-- ModernTabbedPaneUI

    Theme *-- Card
    Theme *-- RoundedBorder
    Theme *-- GlyphIcon
    Theme *-- CheckIcon
    Theme *-- ModernButtonUI
    Theme *-- ModernComboBoxUI
    Theme *-- ModernScrollBarUI
    Theme *-- ModernTabbedPaneUI
```
