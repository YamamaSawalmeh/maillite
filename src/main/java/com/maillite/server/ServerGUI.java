package com.maillite.server;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
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
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ServerGUI extends JFrame {
    private java.util.function.Supplier<NioServer> serverFactory;
    private NioServer server;
    private UserManager userManager;
    private StorageManager storage;
    private Thread serverThread;
    
    private JTextArea logArea;
    private DefaultTableModel userModel;
    private JLabel portLabel;
    private JButton startBtn, stopBtn;
    private JTextField cleanupDaysField;

    public ServerGUI(java.util.function.Supplier<NioServer> serverFactory, UserManager userManager, StorageManager storage) {
        this.serverFactory = serverFactory;
        this.userManager = userManager;
        this.storage = storage;
        
        setTitle("MailLite Server Control");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        setupUI();
        
        // Listen for user updates
        userManager.setUpdateListener(this::refreshUserList);
        
        refreshUserList();
    }

    private void setupUI() {
        // Top: Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startBtn = new JButton("Start Server");
        stopBtn = new JButton("Stop Server");
        stopBtn.setEnabled(true); // Enabled initially to allow start
        portLabel = new JLabel("Port: 11223");
        portLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        startBtn.addActionListener(e -> startServer());
        stopBtn.addActionListener(e -> stopServer());
        stopBtn.setEnabled(false); // Disable stop initially
        
        controlPanel.add(startBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(portLabel);
        
        add(controlPanel, BorderLayout.NORTH);
        
        // Center: Tabs
        JTabbedPane tabs = new JTabbedPane();
        
        // Tab 1: Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        tabs.addTab("Logs", new JScrollPane(logArea));
        
        // Tab 2: Users
        JPanel userPanel = new JPanel(new BorderLayout());
        userModel = new DefaultTableModel(new String[]{"Username", "Status", "IP", "Port", "Last Seen"}, 0);
        JTable userTable = new JTable(userModel);
        
        JPanel userControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField userField = new JTextField(10);
        JPasswordField passField = new JPasswordField(10);
        JButton addUserBtn = new JButton("Add User");
        JButton removeUserBtn = new JButton("Remove User");
        
        addUserBtn.addActionListener(e -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if (!u.isEmpty() && !p.isEmpty()) {
                userManager.addUser(u, p);
                userField.setText("");
                passField.setText("");
            }
        });
        
        removeUserBtn.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row != -1) {
                String u = (String) userModel.getValueAt(row, 0);
                userManager.removeUser(u);
            }
        });
        
        userControlPanel.add(new JLabel("User:"));
        userControlPanel.add(userField);
        userControlPanel.add(new JLabel("Pass:"));
        userControlPanel.add(passField);
        userControlPanel.add(addUserBtn);
        userControlPanel.add(removeUserBtn);
        
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        userPanel.add(userControlPanel, BorderLayout.SOUTH);
        
        tabs.addTab("Users", userPanel);
        
        // Tab 3: Cleanup
        JPanel cleanupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cleanupDaysField = new JTextField("30", 5);
        JButton cleanupBtn = new JButton("Run Cleanup Now");
        
        cleanupBtn.addActionListener(e -> runCleanup());
        
        cleanupPanel.add(new JLabel("Delete archived mails older than (days):"));
        cleanupPanel.add(cleanupDaysField);
        cleanupPanel.add(cleanupBtn);
        
        tabs.addTab("Cleanup", cleanupPanel);
        
        add(tabs, BorderLayout.CENTER);
    }
    
    private void startServer() {
        if (serverThread != null && serverThread.isAlive()) {
            return;
        }
        
        server = serverFactory.get();
        server.setLogger(this::log);
        
        serverThread = new Thread(server);
        serverThread.start();
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
    }
    
    private void stopServer() {
        if (server != null) {
            server.stop();
        }
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        log("Server stopping...");
    }
    
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void refreshUserList() {
        SwingUtilities.invokeLater(() -> {
            userModel.setRowCount(0);
            for (User u : userManager.getAllUsers()) {
                userModel.addRow(new Object[]{
                    u.getUsername(),
                    u.getStatus(),
                    u.getIp(),
                    u.getUdpPort() > 0 ? u.getUdpPort() : "",
                    u.getLastSeen()
                });
            }
        });
    }
    
    private void runCleanup() {
        try {
            int days = Integer.parseInt(cleanupDaysField.getText());
            log("Starting manual cleanup for mails older than " + days + " days...");
            
            new Thread(() -> {
                storage.getAllUsers().forEach(user -> {
                    List<Mail> mails = storage.getMails(user.getUsername());
                    List<Mail> toKeep = new ArrayList<>();
                    boolean changed = false;
                    for (Mail m : mails) {
                        if (m.isArchived()) {
                            try {
                                java.time.LocalDateTime date = java.time.LocalDateTime.parse(m.getTimestamp(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                if (date.isBefore(java.time.LocalDateTime.now().minusDays(days))) {
                                    changed = true;
                                    continue;
                                }
                            } catch (Exception e) {}
                        }
                        toKeep.add(m);
                    }
                    if (changed) {
                        mails.clear();
                        mails.addAll(toKeep);
                        storage.saveMails(user.getUsername());
                        log("Cleaned up mails for " + user.getUsername());
                    }
                });
                log("Cleanup complete.");
            }).start();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid days number");
        }
    }
}