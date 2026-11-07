package com.maillite.client;

import com.maillite.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ClientMain {
    private NetworkClient network;
    private String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain().showLogin());
    }

    public void showLogin() {
        JFrame frame = new JFrame("MailLite Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField ipField = new JTextField("127.0.0.1");
        JTextField userField = new JTextField("alice");
        JPasswordField passField = new JPasswordField("123");
        JButton loginBtn = new JButton("Login");

        panel.add(new JLabel("Server IP:"));
        panel.add(ipField);
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel(""));
        panel.add(loginBtn);

        frame.add(panel);
        frame.setVisible(true);

        loginBtn.addActionListener(e -> {
            String ip = ipField.getText();
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            connectAndLogin(frame, ip, user, pass);
        });
    }

    private void connectAndLogin(JFrame loginFrame, String ip, String user, String pass) {
        try {
            network = new NetworkClient(ip, this::handleUdp);
            network.connect();
            
            // Handshake
            network.send(Protocol.CMD_HELO + " " + user + " UDP:" + network.getLocalUdpPort());
            String heloResp = network.receive();
            if (!heloResp.startsWith("250")) {
                JOptionPane.showMessageDialog(loginFrame, "Server Error: " + heloResp);
                return;
            }

            // Auth
            network.send(Protocol.CMD_AUTH + " " + user + " " + pass);
            String authResp = network.receive();
            if (authResp.startsWith("235")) {
                this.username = user;
                loginFrame.dispose();
                new MainFrame(network, username).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Login Failed: " + authResp);
                network.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(loginFrame, "Connection Error: " + ex.getMessage());
        }
    }
    
    private void handleUdp(String msg) {
        // Will be handled by MainFrame mostly, or we can pass a listener
        System.out.println("UDP: " + msg);
    }
}
