package com.maillite.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientHandler {
    public enum State {
        CONNECTED,
        AUTHENTICATED,
        SENDING_MAIL
    }

    private SocketChannel channel;
    private State state;
    private String username;
    private StringBuilder buffer;
    private int udpPort = -1;
    
    // For SEND command state
    private String sendFrom;
    private String sendTo;
    private String sendSubject;
    private int sendBodyLen;
    private ByteBuffer bodyBuffer;

    public ClientHandler(SocketChannel channel) {
        this.channel = channel;
        this.state = State.CONNECTED;
        this.buffer = new StringBuilder();
    }

    public SocketChannel getChannel() { return channel; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public StringBuilder getBuffer() { return buffer; }
    public int getUdpPort() { return udpPort; }
    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    
    public void prepareSend(String from, String to, String subject, int len) {
        this.sendFrom = from;
        this.sendTo = to;
        this.sendSubject = subject;
        this.sendBodyLen = len;
        this.bodyBuffer = ByteBuffer.allocate(len);
        this.state = State.SENDING_MAIL;
    }
    
    public ByteBuffer getBodyBuffer() { return bodyBuffer; }
    public String getSendFrom() { return sendFrom; }
    public String getSendTo() { return sendTo; }
    public String getSendSubject() { return sendSubject; }
    public int getSendBodyLen() { return sendBodyLen; }
    
    public void resetSend() {
        this.sendFrom = null;
        this.sendTo = null;
        this.sendSubject = null;
        this.sendBodyLen = 0;
        this.bodyBuffer = null;
        this.state = State.AUTHENTICATED;
    }
}
