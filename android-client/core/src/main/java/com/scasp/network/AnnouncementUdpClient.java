package com.scasp.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/** Sends one campus announcement/heartbeat datagram to the UDP listener. */
public final class AnnouncementUdpClient {
    private AnnouncementUdpClient() {}
    public static void send(String host, int port, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        if (data.length > AnnouncementUdpServer.MAX_DATAGRAM) throw new IllegalArgumentException("Datagram too large");
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(host), port));
        }
    }
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        String message = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "SCASP heartbeat";
        send(host, port, message);
        System.out.println("Datagram sent to " + host + ":" + port);
    }
}
