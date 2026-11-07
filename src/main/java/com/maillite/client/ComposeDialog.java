package com.maillite.client;

import com.maillite.common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class ComposeDialog extends JDialog {
    private NetworkClient network;
    private String fromUser;
    private JTextField toField;
    private JTextField subjectField;
    private JTextArea bodyArea;

    public ComposeDialog(Frame owner, NetworkClient network, String fromUser) {
        super(owner, "Compose Mail", true);
        this.network = network;
        this.fromUser = fromUser;
        setSize(400, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new GridLayout(2, 2));
        toField = new JTextField();
        subjectField = new JTextField();
        header.add(new JLabel("To (comma separated for multiple):"));
        header.add(toField);
        header.add(new JLabel("Subject:"));
        header.add(subjectField);
        add(header, BorderLayout.NORTH);

        bodyArea = new JTextArea();
        add(new JScrollPane(bodyArea), BorderLayout.CENTER);

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> send());
        add(sendBtn, BorderLayout.SOUTH);
    }

    private void send() {
        try {
            String to = toField.getText();
            String subj = subjectField.getText();
            String body = bodyArea.getText();
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

            network.send(Protocol.CMD_SEND);
            String resp = network.receive(); // 354 FROM? ...
            
            if (resp.startsWith("354")) {
                // FROM:<me> TO:<u1> SUBJ:<s> BODYLEN:<n>
                // Note: Server doesn't check FROM, but we should send it.
                // Actually protocol says: C: FROM:<me> ...
                // We don't have 'me' stored here easily unless passed. 
                // But server knows who we are. Let's send "me" or actual username if we had it.
                // Protocol: C: FROM:<me> TO:<u1,u2,...> SUBJ:<subject> BODYLEN:<bytes>
                
                String cmd = String.format("FROM:%s TO:%s SUBJ:%s BODYLEN:%d", fromUser, to, subj, bodyBytes.length);
                network.send(cmd);
                
                String resp2 = network.receive(); // 354 BODY
                if (resp2.startsWith("354")) {
                    network.sendRaw(bodyBytes);
                    String finalResp = network.receive(); // 250 MSGID ...
                    if (finalResp.startsWith("250")) {
                        JOptionPane.showMessageDialog(this, "Sent!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Error: " + finalResp);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
