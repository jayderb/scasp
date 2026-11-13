package com.scasp.network;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Receives short campus announcement / heartbeat / event-notification datagrams.
 * UDP is justified here: notifications are small, independent and loss-tolerant (the authoritative
 * state is in the service/database), so connection set-up and retransmission are unnecessary.
 */
public final class AnnouncementUdpServer implements AutoCloseable {
    public static final int MAX_DATAGRAM = 512;
    private final DatagramSocket socket;
    private final Consumer<String> handler;
    private volatile boolean running = true;

    public AnnouncementUdpServer(int port, Consumer<String> handler) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.handler = handler;
    }
    public int getPort() { return socket.getLocalPort(); }

    public Thread startInBackground() {
        return Thread.ofPlatform().name("scasp-udp-listener").daemon(true).start(this::receiveLoop);
    }
    private void receiveLoop() {
        byte[] buffer = new byte[MAX_DATAGRAM];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                handler.accept(packet.getAddress().getHostAddress() + ":" + packet.getPort() + " -> " + msg);
            } catch (IOException e) {
                if (running) handler.accept("UDP receive error: " + e.getMessage());
            }
        }
    }
    @Override public void close() { running = false; socket.close(); }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9091;
        try (AnnouncementUdpServer s = new AnnouncementUdpServer(port, System.out::println)) {
            System.out.println("SCASP UDP listener on " + s.getPort() + " (Ctrl+C to stop)");
            s.startInBackground().join();
        }
    }
}
