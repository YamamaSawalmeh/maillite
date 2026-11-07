package com.maillite.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import com.maillite.common.Protocol;

public class UdpNotifier {
    private DatagramChannel channel;

    public UdpNotifier() {
        try {
            channel = DatagramChannel.open();
            channel.bind(null); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notifyNewMail(User user, int count) {
        if (user.getIp() == null || user.getUdpPort() <= 0) {
            System.out.println("DEBUG: UDP Skip for " + user.getUsername() + " IP=" + user.getIp() + " Port=" + user.getUdpPort());
            return;
        }

        String msg = Protocol.UDP_NOTIFY + " " + user.getUsername() + " " + count;
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        try {
            channel.send(buffer, new InetSocketAddress(user.getIp(), user.getUdpPort()));
            System.out.println("UDP Sent to " + user.getUsername() + ": " + msg);
        } catch (IOException e) {
            System.err.println("Failed to send UDP to " + user.getUsername());
        }
    }
}
