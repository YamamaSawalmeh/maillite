package com.maillite.server;

public class User {
    private String username;
    private String password;
    private transient String status = "OFFLINE"; // Transient: don't save to disk
    private transient String ip;
    private transient int udpPort = -1;
    private String lastSeen;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getStatus() { 
        return status == null ? "OFFLINE" : status; 
    }
    public void setStatus(String status) { this.status = status; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public int getUdpPort() { 
        return udpPort == 0 ? -1 : udpPort; 
    }
    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
}
