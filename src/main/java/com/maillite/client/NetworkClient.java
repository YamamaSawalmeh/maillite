package com.maillite.client;

import com.maillite.common.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class NetworkClient {
    private String host = "localhost";
    private static final int PORT = 11223;
    
    private SocketChannel socketChannel;
    private DatagramChannel udpChannel;
    private int localUdpPort;
    
    private boolean running = true;
    private BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private Consumer<String> onUdpNotification;
    
    public NetworkClient(String host, Consumer<String> onUdpNotification) {
        this.host = host;
        this.onUdpNotification = onUdpNotification;
    }
    
    public void setUdpListener(Consumer<String> listener) {
        this.onUdpNotification = listener;
    }

    public void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, PORT));
        socketChannel.configureBlocking(false);
        
        // Setup UDP
        udpChannel = DatagramChannel.open();
        udpChannel.bind(null); // Bind to random port
        udpChannel.configureBlocking(false);
        localUdpPort = ((InetSocketAddress) udpChannel.getLocalAddress()).getPort();
        
        // Start reader threads
        new Thread(this::readTcpLoop).start();
        new Thread(this::readUdpLoop).start();
    }
    
    public int getLocalUdpPort() { return localUdpPort; }

    public void send(String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap((msg + "\n").getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
    }
    
    public void sendRaw(byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
    }

    public String receive() throws InterruptedException {
        return responseQueue.take();
    }

    private void readTcpLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        StringBuilder sb = new StringBuilder();
        
        while (running) {
            try {
                int read = socketChannel.read(buffer);
                if (read == -1) {
                    running = false;
                    break;
                }
                if (read > 0) {
                    buffer.flip();
                    String chunk = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
                    sb.append(chunk);
                    buffer.clear();
                    
                    int newlineIdx;
                    while ((newlineIdx = sb.indexOf("\n")) != -1) {
                        String line = sb.substring(0, newlineIdx).trim();
                        sb.delete(0, newlineIdx + 1);
                        if (!line.isEmpty()) {
                            responseQueue.offer(line);
                        }
                    }
                } else {
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        }
    }

    private void readUdpLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (running) {
            try {
                buffer.clear();
                InetSocketAddress sender = (InetSocketAddress) udpChannel.receive(buffer);
                if (sender != null) {
                    buffer.flip();
                    String msg = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
                    if (onUdpNotification != null) {
                        onUdpNotification.accept(msg);
                    }
                } else {
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void close() {
        running = false;
        try {
            if (socketChannel != null) socketChannel.close();
            if (udpChannel != null) udpChannel.close();
        } catch (IOException e) {}
    }
}
