package com.maillite.server;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.maillite.common.Protocol;
import com.maillite.common.Utils;

public class CommandProcessor {
    private UserManager userManager;
    private StorageManager storageManager;
    private UdpNotifier udpNotifier;

    public CommandProcessor(UserManager userManager, StorageManager storageManager, UdpNotifier udpNotifier) {
        this.userManager = userManager;
        this.storageManager = storageManager;
        this.udpNotifier = udpNotifier;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public String process(ClientHandler client, String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0].toUpperCase();

        if (client.getState() == ClientHandler.State.CONNECTED) {
            return processConnected(client, cmd, parts, line);
        } else if (client.getState() == ClientHandler.State.AUTHENTICATED) {
            return processAuthenticated(client, cmd, parts, line);
        }
        return Protocol.RESP_UNKNOWN;
    }

    private String processConnected(ClientHandler client, String cmd, String[] parts, String line) {
        switch (cmd) {
            case Protocol.CMD_HELO:
                if (parts.length < 2) return Protocol.RESP_BAD_ARGS;
                // HELO <user> [UDP:<port>]
                if (parts.length >= 3 && parts[2].startsWith("UDP:")) {
                     try {
                         int port = Integer.parseInt(parts[2].substring(4));
                         client.setUdpPort(port);
                     } catch (NumberFormatException e) {}
                }
                return Protocol.RESP_READY;
            case Protocol.CMD_AUTH:
                if (parts.length < 3) return Protocol.RESP_BAD_ARGS;
                String user = parts[1];
                String pass = parts[2];
                if (userManager.authenticate(user, pass)) {
                    client.setUsername(user);
                    client.setState(ClientHandler.State.AUTHENTICATED);
                    userManager.setStatus(user, Protocol.STAT_ACTIVE);
                    
                    // Register UDP port if we have it
                    if (client.getUdpPort() > 0) {
                        String ip = client.getChannel().socket().getInetAddress().getHostAddress();
                        userManager.setConnectionInfo(user, ip, client.getUdpPort());
                    }
                    
                    return Protocol.RESP_AUTH_OK;
                } else {
                    return Protocol.RESP_AUTH_FAIL;
                }
            case Protocol.CMD_QUIT:
                return Protocol.RESP_BYE;
            default:
                return Protocol.RESP_UNKNOWN;
        }
    }

    private String processAuthenticated(ClientHandler client, String cmd, String[] parts, String line) {
        switch (cmd) {
            case Protocol.CMD_HELO:
                 // Handle UDP port update if sent late or re-sent
                 if (parts.length >= 3 && parts[2].startsWith("UDP:")) {
                     try {
                         int port = Integer.parseInt(parts[2].substring(4));
                         String ip = client.getChannel().socket().getInetAddress().getHostAddress();
                         userManager.setConnectionInfo(client.getUsername(), ip, port);
                     } catch (NumberFormatException e) {}
                 }
                 return Protocol.RESP_READY;
            case Protocol.CMD_SETSTAT:
                if (parts.length < 2) return Protocol.RESP_BAD_ARGS;
                userManager.setStatus(client.getUsername(), parts[1]);
                return Protocol.RESP_OK;
            case Protocol.CMD_WHO:
                Collection<User> online = userManager.getOnlineUsers();
                StringBuilder sb = new StringBuilder();
                sb.append("212 ").append(online.size()).append("\n");
                for (User u : online) {
                    sb.append("212U ").append(u.getUsername()).append(" ")
                      .append(u.getStatus()).append(" ")
                      .append(u.getIp() == null ? "0.0.0.0" : u.getIp()).append(" ")
                      .append(u.getLastSeen()).append("\n");
                }
                sb.append("212 END");
                return sb.toString();
            case Protocol.CMD_LIST:
                // LIST [ALL|UNREAD|SENT|ARCHIVED]
                String filter = parts.length > 1 ? parts[1].toUpperCase() : "INBOX";
                List<Mail> mails = storageManager.getMails(client.getUsername());
                StringBuilder listSb = new StringBuilder();
                
                List<Mail> filtered = mails.stream()
                    .filter(m -> {
                        boolean isMe = m.getFrom().equals(client.getUsername());
                        switch (filter) {
                            case "UNREAD": return !m.isRead() && !m.isArchived() && !isMe;
                            case "SENT": return isMe && !m.isArchived();
                            case "ARCHIVED": return m.isArchived();
                            case "ALL": return true;
                            default: return !m.isArchived() && !isMe; // INBOX
                        }
                    })
                    .collect(Collectors.toList());
                    
                for (Mail m : filtered) {
                    listSb.append("213 ").append(m.getId()).append(" ")
                          .append(m.getFrom()).append(" ")
                          .append(m.getSize()).append(" ")
                          .append(m.getTimestamp()).append(" ")
                          .append(m.getSubject().replace(" ", "_")) // Escape spaces
                          .append("\n");
                }
                listSb.append("213 END");
                return listSb.toString();
            case Protocol.CMD_RETR:
                if (parts.length < 2) return Protocol.RESP_BAD_ARGS;
                String msgId = parts[1];
                Optional<Mail> mailOpt = storageManager.getMails(client.getUsername()).stream()
                        .filter(m -> m.getId().equals(msgId)).findFirst();
                if (mailOpt.isPresent()) {
                    Mail m = mailOpt.get();
                    m.setRead(true);
                    storageManager.saveMails(client.getUsername());
                    
                    byte[] body = storageManager.getBody(m.getBodyPath());
                    StringBuilder retrSb = new StringBuilder();
                    retrSb.append("214 FROM:").append(m.getFrom()).append("\n");
                    retrSb.append("214 TO:").append(m.getTo()).append("\n");
                    retrSb.append("214 SUBJECT:").append(m.getSubject()).append("\n");
                    retrSb.append("214 DATE:").append(m.getTimestamp()).append("\n");
                    retrSb.append("214 BODYLEN ").append(body.length).append("\n");
                    retrSb.append("214 BODY\n");
                    retrSb.append(new String(body, StandardCharsets.UTF_8)).append("\n"); // Assuming text body for simplicity in demo
                    retrSb.append("214 END");
                    return retrSb.toString();
                } else {
                    return "550 MSG NOT FOUND";
                }
            case Protocol.CMD_SEND:
                return Protocol.RESP_SEND_START;
            case Protocol.CMD_DELE: // Archive
                if (parts.length < 2) return Protocol.RESP_BAD_ARGS;
                String delId = parts[1];
                Optional<Mail> delMail = storageManager.getMails(client.getUsername()).stream()
                        .filter(m -> m.getId().equals(delId)).findFirst();
                if (delMail.isPresent()) {
                    delMail.get().setArchived(true);
                    storageManager.saveMails(client.getUsername());
                    return Protocol.RESP_OK;
                }
                return "550 MSG NOT FOUND";
            case Protocol.CMD_RESTORE:
                if (parts.length < 2) return Protocol.RESP_BAD_ARGS;
                String resId = parts[1];
                Optional<Mail> resMail = storageManager.getMails(client.getUsername()).stream()
                        .filter(m -> m.getId().equals(resId)).findFirst();
                if (resMail.isPresent()) {
                    resMail.get().setArchived(false);
                    storageManager.saveMails(client.getUsername());
                    return Protocol.RESP_OK;
                }
                return "550 MSG NOT FOUND";
            case Protocol.CMD_STAT:
                List<Mail> myMails = storageManager.getMails(client.getUsername());
                long unread = myMails.stream().filter(m -> !m.isRead() && !m.isArchived()).count();
                long bytes = myMails.stream().mapToLong(Mail::getSize).sum();
                long onlineCount = userManager.getOnlineUsers().size();
                return "211 M:" + unread + " S:" + bytes + " U:" + onlineCount;
            case Protocol.CMD_QUIT:
                userManager.setStatus(client.getUsername(), "OFFLINE");
                return Protocol.RESP_BYE;
            default:
                if (cmd.startsWith("FROM:")) {
                   try {
                        String rest = line;
                        String from = extract(rest, "FROM:");
                        String to = extract(rest, "TO:");
                        String subj = extract(rest, "SUBJ:");
                        String lenStr = extract(rest, "BODYLEN:");
                        System.out.println("DEBUG: Parsed SEND - From:" + from + " To:" + to + " Subj:" + subj + " Len:" + lenStr);
                        int len = Integer.parseInt(lenStr);
                        
                        client.prepareSend(from, to, subj, len);
                        return Protocol.RESP_SEND_BODY;
                    } catch (Exception e) {
                        System.out.println("DEBUG: Parse Error: " + e.getMessage());
                        return Protocol.RESP_BAD_ARGS;
                    }
                }
                return Protocol.RESP_UNKNOWN;
        }
    }

    private String extract(String line, String key) {
        int start = line.indexOf(key);
        if (start == -1) return "";
        start += key.length();
        int end = line.indexOf(" ", start);
        if (end == -1) end = line.length();
        return line.substring(start, end);
    }

    public String processBody(ClientHandler client, byte[] data) {
        System.out.println("DEBUG: Received body chunk size: " + data.length + ". Total buffered: " + (client.getBodyBuffer().position() + data.length) + "/" + client.getBodyBuffer().capacity());
        client.getBodyBuffer().put(data);
        if (!client.getBodyBuffer().hasRemaining()) {
            System.out.println("DEBUG: Body complete. Processing...");
            // Body complete
            byte[] body = client.getBodyBuffer().array();
            String bodyPath = storageManager.saveBody(client.getUsername(), body); // Save to sender's folder? Or global? Ideally global or per recipient.
            // For simplicity, we save once and reference.
            
            String msgId = UUID.randomUUID().toString().substring(0, 8);
            String timestamp = Utils.now();
            
            // Deliver to recipients
            String[] recipients = client.getSendTo().split(",");
            for (String rcpt : recipients) {
                if (storageManager.getUser(rcpt) != null) {
                    // Create mail entry for recipient
                    // We need to save body for recipient too or share path. Sharing path is easier.
                    Mail mail = new Mail(msgId, client.getSendFrom(), client.getSendTo(), client.getSendSubject(), bodyPath, body.length, timestamp);
                    storageManager.addMail(rcpt, mail);
                    
                    // Notify
                    User u = storageManager.getUser(rcpt);
                    if (u != null) {
                        long count = storageManager.getMails(rcpt).stream().filter(m -> !m.isRead() && !m.isArchived()).count();
                        udpNotifier.notifyNewMail(u, (int)count);
                    }
                }
            }
            
            // Save to Sent for sender
            Mail sentMail = new Mail(msgId, client.getSendFrom(), client.getSendTo(), client.getSendSubject(), bodyPath, body.length, timestamp);
            sentMail.setRead(true); // Sent mail is read
            storageManager.addMail(client.getUsername(), sentMail); // We might want a separate "Sent" box logic, but for now just adding to list. 
            // Actually, spec says "Inbox / Sent / Archive". We can distinguish by "From". 
            // If From == Me, it's Sent.
            
            client.resetSend();
            return "250 MSGID " + msgId;
        }
        return null; // Need more data
    }

    
}
