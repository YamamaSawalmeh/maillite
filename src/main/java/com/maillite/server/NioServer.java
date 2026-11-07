package com.maillite.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.maillite.common.Protocol;

public class NioServer implements Runnable {
    private static final int PORT = 11223;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private boolean running = true;
    
    private CommandProcessor processor;
    private Map<SocketChannel, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final String STAT_OFFLINE = "OFFLINE";

    public NioServer(CommandProcessor processor) {
        this.processor = processor;
    }

    private java.util.function.Consumer<String> logger;

    public void setLogger(java.util.function.Consumer<String> logger) {
        this.logger = logger;
    }

    private void log(String msg) {
        if (logger != null) logger.accept(msg);
        System.out.println(msg);
    }

    public void stop() {
        running = false;
        if (selector != null) selector.wakeup();
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            log("MailLite Server started on port " + PORT);

            while (running) {
                selector.select();
                if (!running) break;
                
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    
                    if (!key.isValid()) continue;
                    
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }
            }
            
            // Cleanup
            if (serverChannel != null) serverChannel.close();
            if (selector != null) selector.close();
            log("Server stopped.");
            
        } catch (IOException e) {
            e.printStackTrace();
            log("Server Error: " + e.getMessage());
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        
        ClientHandler handler = new ClientHandler(clientChannel);
        clients.put(clientChannel, handler);
        
        log("New connection from " + clientChannel.getRemoteAddress());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientHandler handler = clients.get(channel);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int read = -1;
        try {
            read = channel.read(buffer);
        } catch (IOException e) {
            // Connection closed forcibly
            close(channel);
            return;
        }

        if (read == -1) {
            close(channel);
            return;
        }

        buffer.flip();
        
        if (handler.getState() == ClientHandler.State.SENDING_MAIL) {
            // Raw body mode
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String response = processor.processBody(handler, data);
            if (response != null) {
                write(channel, response + "\n");
            }
        } else {
            // Text command mode
            String chunk = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
            handler.getBuffer().append(chunk);
            
            String content = handler.getBuffer().toString();
            int newlineIndex;
            while ((newlineIndex = content.indexOf('\n')) != -1) {
                String line = content.substring(0, newlineIndex).trim();
                handler.getBuffer().delete(0, newlineIndex + 1);
                content = handler.getBuffer().toString(); // Update content for next loop
                
                if (!line.isEmpty()) {
                    log("CMD from " + (handler.getUsername() != null ? handler.getUsername() : "UNKNOWN") + ": " + line);
                    String response = processor.process(handler, line);
                    write(channel, response + "\n");
                    
                    if (response.equals(Protocol.RESP_BYE)) {
                        close(channel);
                        return;
                    }
                }
            }
        }
    }

    private void write(SocketChannel channel, String msg) throws IOException {
        log("SERVER SEND to " + channel.getRemoteAddress() + ": " + msg.trim());
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

   // في NioServer.java
    private void close(SocketChannel channel) throws IOException {
        ClientHandler handler = clients.get(channel);

        if (handler != null && handler.getUsername() != null) {
            // تحديث الحالة إلى OFFLINE عند إغلاق الاتصال
            processor.getUserManager().setStatus(handler.getUsername(), STAT_OFFLINE);
            log("User logged out/disconnected: " + handler.getUsername());
        }
        
        log("Connection closed: " + channel.getRemoteAddress());
        clients.remove(channel);
        channel.close();
    }
    }

    

