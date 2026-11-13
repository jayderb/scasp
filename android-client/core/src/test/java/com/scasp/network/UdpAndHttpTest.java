package com.scasp.network;

import com.scasp.app.SampleData;
import com.scasp.model.Announcement;
import com.scasp.service.AcademicService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class UdpAndHttpTest {
    @Test void udpAnnouncementIsReceived() throws Exception {
        BlockingQueue<String> got = new LinkedBlockingQueue<>();
        try (AnnouncementUdpServer srv = new AnnouncementUdpServer(0, got::add)) {
            srv.startInBackground();
            AnnouncementUdpClient.send("127.0.0.1", srv.getPort(), "SCASP heartbeat");
            String msg = got.poll(5, TimeUnit.SECONDS);
            assertNotNull(msg); assertTrue(msg.endsWith("-> SCASP heartbeat"));
        }
        assertThrows(IllegalArgumentException.class, () -> AnnouncementUdpClient.send("127.0.0.1", 9, "x".repeat(600)));
    }
    @Test void registrationEventsAreNotifiedOverUdp() throws Exception {
        BlockingQueue<String> got = new LinkedBlockingQueue<>();
        try (AnnouncementUdpServer srv = new AnnouncementUdpServer(0, got::add)) {
            srv.startInBackground();
            AcademicService s = SampleData.load(new AcademicService());
            s.addAuditListener(new UdpNotificationPublisher("127.0.0.1", srv.getPort()));
            s.register("STU001", "CSC301");
            s.publishAnnouncement("STAFF01", "t", "b", Announcement.Audience.ALL);
            assertTrue(got.poll(5, TimeUnit.SECONDS).endsWith("EVENT|REGISTER|REGISTRATION|STU001:CSC301|STU001"));
            assertTrue(got.poll(5, TimeUnit.SECONDS).endsWith("EVENT|PUBLISH_ANNOUNCEMENT|ANNOUNCEMENT|ANN001|STAFF01"));
        }
    }
    @Test void httpFetchOfAuthorisedResourceAndSafeFailures() throws Exception {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/api/announcements", ex -> {
            boolean ok = "Bearer test-token".equals(ex.getRequestHeaders().getFirst("Authorization"));
            byte[] body = (ok ? "ANN001 Registration reminder" : "").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(ok ? 200 : 401, ok ? body.length : -1);
            if (ok) try (OutputStream o = ex.getResponseBody()) { o.write(body); }
            ex.close();
        });
        http.start();
        try {
            String url = "http://127.0.0.1:" + http.getAddress().getPort() + "/api/announcements";
            HttpResourceFetcher f = new HttpResourceFetcher(Duration.ofSeconds(3));
            HttpResourceFetcher.Result ok = f.fetch(url, "test-token");
            assertTrue(ok.ok()); assertEquals(200, ok.status()); assertEquals("ANN001 Registration reminder", ok.body());
            HttpResourceFetcher.Result denied = f.fetch(url, "wrong");
            assertFalse(denied.ok()); assertEquals(401, denied.status()); assertEquals("Not authorised to read this resource", denied.message());
            assertEquals("Only http/https URLs are allowed", f.fetch("file:///etc/passwd", null).message());
            assertEquals("Malformed URL", f.fetch("http://bad host/", null).message());
        } finally { http.stop(0); }
        int closedPort; try (ServerSocket ss = new ServerSocket(0)) { closedPort = ss.getLocalPort(); }
        HttpResourceFetcher.Result down = new HttpResourceFetcher(Duration.ofSeconds(2)).fetch("http://127.0.0.1:" + closedPort + "/x", null);
        assertFalse(down.ok()); assertEquals("Remote service unavailable", down.message());
    }
    @Test void inetAddressDemoDescribesHostsAndHandlesUnknown() {
        List<String> lines = InetAddressDemo.describe("localhost", "no-such-host.invalid");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Loopback:") && l.contains("isLoopback=true")));
        // some networks (like the build sandbox) resolve every name, so only require that the lookup is handled
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Resolve no-such-host.invalid")));
    }
}
