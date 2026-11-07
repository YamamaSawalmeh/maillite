package com.maillite.server;

import java.util.ArrayList;
import java.util.List;

public class ServerMain {
    public static void main(String[] args) {
        StorageManager storage = new StorageManager();
        UserManager userManager = new UserManager(storage);
        UdpNotifier udpNotifier = new UdpNotifier();
        CommandProcessor processor = new CommandProcessor(userManager, storage, udpNotifier);
        
        // Cleanup Job (Runs every hour)
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000); // 1 hour
                    System.out.println("Running cleanup job...");
                    storage.getAllUsers().forEach(user -> {
                        List<Mail> mails = storage.getMails(user.getUsername());
                        List<Mail> toKeep = new ArrayList<>();
                        boolean changed = false;
                        for (Mail m : mails) {
                            // Delete archived > 30 days
                            if (m.isArchived()) {
                                try {
                                    java.time.LocalDateTime date = java.time.LocalDateTime.parse(m.getTimestamp(), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    if (date.isBefore(java.time.LocalDateTime.now().minusDays(30))) {
                                        changed = true;
                                        continue; // Drop it
                                    }
                                } catch (Exception e) {}
                            }
                            toKeep.add(m);
                        }
                        if (changed) {
                            mails.clear();
                            mails.addAll(toKeep);
                            storage.saveMails(user.getUsername());
                            System.out.println("Cleaned up mails for " + user.getUsername());
                        }
                    });
                } catch (InterruptedException e) { break; }
            }
        }).start();
        
        // Launch GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI(() -> new NioServer(processor), userManager, storage);
            gui.setVisible(true);
        });
    }
}
