package com.maillite.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.maillite.common.Protocol;

public class MainFrame extends JFrame {
    private NetworkClient network;
    private String username;
    
    private JTable inboxTable;
    private DefaultTableModel inboxModel;
    private JTextArea logArea;
    private DefaultListModel<String> userListModel;
    
    private Timer autoAwayTimer;
    private long lastActivityTime;
    
    private JTabbedPane tabbedPane;
    private JTable sentTable, archiveTable;
    private DefaultTableModel sentModel, archiveModel;
    private JTextField searchField;

    private JLabel statusLabel, hostLabel, portLabel, elapsedLabel, lastLoginLabel;
    private long loginTime;
    private String currentStatus = Protocol.STAT_ACTIVE;
    private boolean isAway = false;

    public MainFrame(NetworkClient network, String username) {
        this.network = network;
        this.username = username;
        
        setTitle("MailLite - " + username);
        setSize(800, 600);
setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            try {
                // إرسال أمر QUIT قبل الإغلاق
                network.send(Protocol.CMD_QUIT);
            } catch (Exception ex) {
                // تجاهل الأخطاء
            } finally {
                network.close(); // إغلاق اتصال الشبكة
                dispose();
                System.exit(0); // إنهاء التطبيق
            }
        }
    });        setLocationRelativeTo(null);
        
        setupUI();
        setupAutoAway();
        
        network.setUdpListener(this::handleUdp);
        
        // Initial fetch
        refreshAll();
        
        log("Logged in as " + username);
    }

    private void setupUI() {
        // Top Panel: Status & Toolbar
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] statuses = {Protocol.STAT_ACTIVE, Protocol.STAT_BUSY, Protocol.STAT_AWAY};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        statusBox.addActionListener(e -> setStatus((String) statusBox.getSelectedItem()));
        statusPanel.add(new JLabel("Status:"));
        statusPanel.add(statusBox);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton composeBtn = new JButton("Compose");
        composeBtn.addActionListener(e -> new ComposeDialog(this, network, username).setVisible(true));
        actionPanel.add(composeBtn);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        actionPanel.add(refreshBtn);
        
        JButton exportBtn = new JButton("Export History");
        exportBtn.addActionListener(e -> exportHistory());
        actionPanel.add(exportBtn);
        
        searchField = new JTextField(15);
        searchField.setToolTipText("Search Sender or Subject...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        actionPanel.add(new JLabel("Search:"));
        actionPanel.add(searchField);
        actionPanel.add(searchBtn);
        
        topPanel.add(statusPanel, BorderLayout.WEST);
        topPanel.add(actionPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        // Center: Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        tabbedPane = new JTabbedPane();
        inboxModel = createMailModel();
        inboxTable = createMailTable(inboxModel, false);
        tabbedPane.addTab("Inbox", new JScrollPane(inboxTable));
        
        sentModel = createMailModel();
        sentTable = createMailTable(sentModel, false);
        tabbedPane.addTab("Sent", new JScrollPane(sentTable));
        
        archiveModel = createMailModel();
        archiveTable = createMailTable(archiveModel, true);
        tabbedPane.addTab("Archive", new JScrollPane(archiveTable));
        
        splitPane.setLeftComponent(tabbedPane);

        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(200, 0));
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);

        // Bottom: Logs + Status Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(5, 50);
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        
        hostLabel = new JLabel("Host: 127.0.0.1");
        portLabel = new JLabel("Port: 11223");
        loginTime = System.currentTimeMillis();
        elapsedLabel = new JLabel("Elapsed: 00:00:00");
        lastLoginLabel = new JLabel("Last Login: Now");
        statusLabel = new JLabel("Status: ACTIVE");
        
        statusBar.add(hostLabel);
        statusBar.add(portLabel);
        statusBar.add(lastLoginLabel);
        statusBar.add(elapsedLabel);
        statusBar.add(statusLabel);
        
        bottomPanel.add(statusBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Timer for elapsed time
        new Timer(1000, e -> {
            long elapsed = System.currentTimeMillis() - loginTime;
            long s = (elapsed / 1000) % 60;
            long m = (elapsed / (1000 * 60)) % 60;
            long h = (elapsed / (1000 * 60 * 60));
            elapsedLabel.setText(String.format("Elapsed: %02d:%02d:%02d", h, m, s));
        }).start();
    }
    
    private DefaultTableModel createMailModel() {
        return new DefaultTableModel(new String[]{"ID", "From", "Subject", "Size", "Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }
    
    private JTable createMailTable(DefaultTableModel model, boolean isArchive) {
        JTable table = new JTable(model);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String from = (String) table.getValueAt(row, 1);
                
                // Distinct Color Generation using HSB
                int hash = Math.abs(from.hashCode());
                float hue = (hash % 360) / 360f; // Full spectrum
                float saturation = 0.4f; // Soft pastel
                float brightness = 0.95f; // Very bright/light
                
                Color color = Color.getHSBColor(hue, saturation, brightness);
                
                if (!isSelected) c.setBackground(color);
                else c.setBackground(table.getSelectionBackground());
                return c;
            }
        });
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openMail(table, isArchive);
                }
            }
        });
        return table;
    }
    
    private void performSearch() {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term (Sender or Subject)");
            return;
        }
        new Thread(() -> {
            refreshMailboxSync(inboxModel, "INBOX", query);
            refreshMailboxSync(sentModel, "SENT", query);
            refreshMailboxSync(archiveModel, "ARCHIVED", query);
        }).start();
    }
    
    private void refresh() {
        searchField.setText("");
        refreshAll();
    }

    private void refreshAll() {
        String query = ""; // Always refresh all with empty query
        new Thread(() -> {
            refreshUserListSync();
            refreshMailboxSync(inboxModel, "INBOX", query);
            refreshMailboxSync(sentModel, "SENT", query);
            refreshMailboxSync(archiveModel, "ARCHIVED", query);
        }).start();
    }

    private void setStatus(String status) {
        try {
            network.send(Protocol.CMD_SETSTAT + " " + status);
            String resp = network.receive();
            log("Set Status " + status + ": " + resp);
            if (resp.startsWith("250")) {
                currentStatus = status;
                statusLabel.setText("Status: " + status);
                if (status.equals(Protocol.STAT_AWAY)) isAway = true;
                else isAway = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshUserListSync() {
        try {
            network.send(Protocol.CMD_WHO);
            String line;
            SwingUtilities.invokeLater(() -> userListModel.clear());
            while ((line = network.receive()) != null) {
                if (line.startsWith("212 END")) break;
                if (line.startsWith("212U")) {
                    String finalLine = line;
                    SwingUtilities.invokeLater(() -> userListModel.addElement(finalLine.substring(5)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshMailboxSync(DefaultTableModel model, String type, String query) {
        try {
            network.send(Protocol.CMD_LIST + " " + type);
            String line;
            SwingUtilities.invokeLater(() -> model.setRowCount(0));
            
            while ((line = network.receive()) != null) {
                if (line.startsWith("213 END")) break;
                if (line.startsWith("213 ")) {
                    String[] parts = line.split(" ");
                    // 213 <id> <from> <size> <date> <subject>
                    if (parts.length >= 6) {
                        String id = parts[1];
                        String from = parts[2];
                        String size = parts[3];
                        String date = parts[4] + " " + parts[5];
                        
                        String subject = "";
                        if (parts.length > 6) {
                            subject = parts[6].replace("_", " ");
                        }
                        
                        // Robust Filter
                        boolean match = true;
                        if (!query.isEmpty()) {
                            boolean inFrom = from.toLowerCase().contains(query);
                            boolean inSubj = subject.toLowerCase().contains(query);
                            if (!inFrom && !inSubj) match = false;
                        }
                        
                        if (match) {
                            String finalSubject = subject;
                            String finalDate = date;
                            SwingUtilities.invokeLater(() -> model.addRow(new Object[]{id, from, finalSubject, size, finalDate}));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void openMail(JTable table, boolean isArchive) {
        int row = table.getSelectedRow();
        if (row == -1) return;
        String id = (String) table.getValueAt(row, 0);
        String from = (String) table.getValueAt(row, 1);
        String subject = (String) table.getValueAt(row, 2);
        String date = (String) table.getValueAt(row, 4);
        
        new Thread(() -> {
            try {
                network.send(Protocol.CMD_RETR + " " + id);
                StringBuilder body = new StringBuilder();
                String line;
                boolean readingBody = false;
                String msgFrom = from; // Default to table value
                String msgTo = username; // Default to me
                
                while ((line = network.receive()) != null) {
                    if (line.startsWith("214 END")) break;
                    if (line.startsWith("214 FROM:")) {
                        msgFrom = line.substring(9).trim();
                    } else if (line.startsWith("214 TO:")) {
                        msgTo = line.substring(7).trim();
                    } else if (line.equals("214 BODY")) {
                        readingBody = true;
                        continue;
                    }
                    if (readingBody) {
                        body.append(line).append("\n");
                    }
                }
                
                String content = body.toString();
                String finalFrom = msgFrom;
                String finalTo = msgTo;
                
                SwingUtilities.invokeLater(() -> {
                    JPanel panel = new JPanel(new BorderLayout(5, 5));
                    
                    JPanel header = new JPanel(new GridLayout(4, 1));
                    header.add(new JLabel("From: " + finalFrom));
                    header.add(new JLabel("To: " + finalTo));
                    header.add(new JLabel("Date: " + date));
                    header.add(new JLabel("Subject: " + subject));
                    header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    
                    JTextArea area = new JTextArea(content);
                    area.setEditable(false);
                    area.setLineWrap(true);
                    
                    panel.add(header, BorderLayout.NORTH);
                    panel.add(new JScrollPane(area), BorderLayout.CENTER);
                    panel.setPreferredSize(new Dimension(400, 300));
                    
                    Object[] options;
                    if (isArchive) {
                        options = new Object[]{"Restore", "Close"};
                    } else {
                        options = new Object[]{"Archive", "Close"};
                    }
                    
                    int opt = JOptionPane.showOptionDialog(this, panel, "Message Details",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
                            
                    if (opt == 0) { // First button
                        if (isArchive) restoreMail(id);
                        else archiveMail(id);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void archiveMail(String id) {
        try {
            network.send(Protocol.CMD_DELE + " " + id);
            network.receive(); // 250 OK
            refreshAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void restoreMail(String id) {
        try {
            network.send(Protocol.CMD_RESTORE + " " + id);
            network.receive(); // 250 OK
            refreshAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUdp(String msg) {
        SwingUtilities.invokeLater(() -> {
            log("UDP Notification: " + msg);
            if (msg.contains("NEWMAIL")) {
                JOptionPane.showMessageDialog(this, "You have new mail!");
                refreshAll();
            }
        });
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void setupAutoAway() {
        lastActivityTime = System.currentTimeMillis();
        
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            lastActivityTime = System.currentTimeMillis();
            if (isAway) {
                // Revert to ACTIVE
                setStatus(Protocol.STAT_ACTIVE);
                isAway = false;
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        autoAwayTimer = new Timer(1000, e -> {
            if (!isAway && System.currentTimeMillis() - lastActivityTime > 30000) { // 30 sec
                setStatus(Protocol.STAT_AWAY);
                isAway = true;
            }
        });
        autoAwayTimer.start();
    }
    
    private void exportHistory() {
        try {
            File file = new File("chat_history_" + username + ".txt");
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("Exported History for " + username + "\n");
                fw.write("--------------------------------\n");
                for (int i = 0; i < inboxModel.getRowCount(); i++) {
                    fw.write("From: " + inboxModel.getValueAt(i, 1) + 
                             " | Subject: " + inboxModel.getValueAt(i, 2) +
                             " | Date: " + inboxModel.getValueAt(i, 4) + "\n");
                }
            }
            JOptionPane.showMessageDialog(this, "Exported to " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
