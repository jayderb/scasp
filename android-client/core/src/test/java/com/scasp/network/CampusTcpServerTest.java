package com.scasp.network;

import com.scasp.app.SampleData;
import com.scasp.concurrent.RequestProcessor;
import com.scasp.service.AcademicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class CampusTcpServerTest {
    AcademicService service; CampusTcpServer server; int port;
    final List<String> trace = new CopyOnWriteArrayList<>();

    @BeforeEach void start() throws IOException {
        service = SampleData.load(new AcademicService());
        server = new CampusTcpServer(service, 0, new RequestProcessor(8, 4), 500); // 500 ms idle timeout
        server.setTrace(trace::add);
        server.startInBackground();
        port = server.getPort();
    }
    @AfterEach void stop() throws IOException { server.close(); }

    @Test void sessionWithSeveralCommands() throws IOException {
        try (CampusTcpClient c = new CampusTcpClient("localhost", port, 2000)) {
            assertEquals("OK PONG", c.send("PING"));
            assertEquals("OK REGISTERED", c.send("REGISTER|STU001|CSC301"));
            assertEquals("OK CSC301 enrolled=1 capacity=2 waiting=0", c.send("SEATS|CSC301"));
            assertEquals("OK BYE", c.send("QUIT"));
            assertNull(c.readLine(), "server closed after QUIT");
        }
        assertEquals("OK PONG", CampusTcpClient.send("localhost", port, "PING"), "original one-shot API");
    }
    @Test void multipleConcurrentClientsAreServed() throws Exception {
        int clients = 4;   // = worker count, all connected at the same time
        CyclicBarrier together = new CyclicBarrier(clients);
        ExecutorService ex = Executors.newFixedThreadPool(clients);
        List<Future<String>> fs = new ArrayList<>();
        String[] students = {"STU001", "STU002", "STU003", "STU004"};
        for (String sid : students) fs.add(ex.submit(() -> {
            try (CampusTcpClient c = new CampusTcpClient("localhost", port, 3000)) {
                assertEquals("OK PONG", c.send("PING"));
                together.await(5, TimeUnit.SECONDS);       // proves 4 sessions are open simultaneously
                return c.send("REGISTER|" + sid + "|DB302");
            }
        }));
        long registered = 0, waitlisted = 0;
        for (Future<String> f : fs) { String r = f.get(); if (r.equals("OK REGISTERED")) registered++; else if (r.equals("OK WAITLISTED")) waitlisted++; }
        ex.shutdown();
        assertEquals(3, registered); assertEquals(1, waitlisted);
    }
    @Test void malformedMessageGetsErrorAndConnectionStaysUsable() throws IOException {
        try (CampusTcpClient c = new CampusTcpClient("localhost", port, 2000)) {
            assertTrue(c.send("REGISTER|only-one-arg").startsWith("ERR 400"));
            assertTrue(c.send("\u0000garbage").startsWith("ERR 400 UNKNOWN_COMMAND"));
            assertEquals("OK PONG", c.send("PING"));
        }
    }
    @Test void oversizedLineIsRejectedAndConnectionClosed() throws IOException {
        try (CampusTcpClient c = new CampusTcpClient("localhost", port, 2000)) {
            assertTrue(c.send("PING" + "x".repeat(5000)).startsWith("ERR 413 LINE_TOO_LONG"));
            assertNull(c.readLine());
        }
    }
    @Test void abruptClientDisconnectDoesNotAffectOtherClients() throws Exception {
        Socket rude = new Socket("localhost", port);
        rude.getOutputStream().write("PIN".getBytes(StandardCharsets.UTF_8)); // half a frame then vanish
        rude.close();
        assertEquals("OK PONG", CampusTcpClient.send("localhost", port, "PING"));
    }
    @Test void idleClientTimesOut() throws IOException {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port), 2000);
            s.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            assertEquals("ERR 408 IDLE_TIMEOUT Closing idle connection", in.readLine()); // sent nothing
            assertNull(in.readLine(), "server closed the socket");
        }
    }
    @Test void serverBusyWhenProcessorIsFull() throws Exception {
        server.close();
        server = new CampusTcpServer(service, 0, new RequestProcessor(1, 1), 5000); // 2 connections max
        server.startInBackground();
        try (CampusTcpClient a = new CampusTcpClient("localhost", server.getPort(), 3000);
             CampusTcpClient b = new CampusTcpClient("localhost", server.getPort(), 3000);
             CampusTcpClient c = new CampusTcpClient("localhost", server.getPort(), 3000)) {
            assertEquals("OK PONG", a.send("PING"));
            assertTrue(c.readLine().startsWith("ERR 503 BUSY"));
            assertNotNull(b);
        }
    }
    @Test void shutdownEndsSessionsAndRefusesNewConnections() throws Exception {
        try (CampusTcpClient c = new CampusTcpClient("localhost", port, 3000)) {
            assertEquals("OK PONG", c.send("PING"));
            server.close();
            assertNull(c.readLine(), "open session ended by shutdown");
        }
        assertThrows(ConnectException.class, () -> new CampusTcpClient("localhost", port, 1000));
        assertTrue(trace.stream().anyMatch(t -> t.contains("C: PING")));
    }
}
