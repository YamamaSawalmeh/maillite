package com.maillite.server;

public class Mail {
    private String id;
    private String from;
    private String to; // Comma separated
    private String subject;
    private String bodyPath; // Path to raw body file
    private long size;
    private String timestamp;
    private boolean isRead;
    private boolean isArchived;

    public Mail(String id, String from, String to, String subject, String bodyPath, long size, String timestamp) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.bodyPath = bodyPath;
        this.size = size;
        this.timestamp = timestamp;
        this.isRead = false;
        this.isArchived = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBodyPath() { return bodyPath; }
    public long getSize() { return size; }
    public String getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }
}
