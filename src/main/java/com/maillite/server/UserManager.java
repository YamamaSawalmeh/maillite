package com.maillite.server;

import com.maillite.common.Protocol;
import com.maillite.common.Utils;

import java.util.Collection;

public class UserManager {
    private StorageManager storage;

    public UserManager(StorageManager storage) {
        this.storage = storage;
    }

    private Runnable updateListener;

    public void setUpdateListener(Runnable listener) {
        this.updateListener = listener;
    }

    private void notifyUpdate() {
        if (updateListener != null) {
            updateListener.run();
        }
    }

    public void addUser(String username, String password) {
        storage.createUser(username, password);
        notifyUpdate();
    }

    public void removeUser(String username) {
        storage.removeUser(username);
        notifyUpdate();
    }

    public boolean authenticate(String username, String password) {
        User user = storage.getUser(username);
        return user != null && user.getPassword().equals(password);
    }

    public void setStatus(String username, String status) {
        User user = storage.getUser(username);
        if (user != null) {
            user.setStatus(status);
            if (status.equals(Protocol.STAT_ACTIVE)) {
                user.setLastSeen(Utils.now());
            }
            notifyUpdate();
        }
    }

    public void setConnectionInfo(String username, String ip, int udpPort) {
        User user = storage.getUser(username);
        if (user != null) {
            user.setIp(ip);
            user.setUdpPort(udpPort);
            user.setLastSeen(Utils.now());
            notifyUpdate();
        }
    }
    
    public User getUser(String username) {
        return storage.getUser(username);
    }

    public Collection<User> getOnlineUsers() {
        return storage.getAllUsers().stream()
                .filter(u -> !u.getStatus().equals("OFFLINE"))
                .toList();
    }
    
    public Collection<User> getAllUsers() {
        return storage.getAllUsers();
    }
}
