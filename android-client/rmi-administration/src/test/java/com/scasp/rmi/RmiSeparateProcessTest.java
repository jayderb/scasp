package com.scasp.rmi;

import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Starts RmiServer in a SEPARATE JVM process and calls it from this test JVM over RMI/TCP. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Timeout(60)
class RmiSeparateProcessTest {
    Process server; int port;

    static int freePort() throws Exception { try (ServerSocket s = new ServerSocket(0)) { return s.getLocalPort(); } }

    @BeforeAll void startServerProcess() throws Exception {
        port = freePort();
        String cp = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        server = new ProcessBuilder(java, "-Djava.rmi.server.hostname=127.0.0.1", "-cp", cp, "com.scasp.rmi.RmiServer", String.valueOf(port))
                .redirectErrorStream(true).start();
        BufferedReader out = new BufferedReader(new InputStreamReader(server.getInputStream()));
        String line = out.readLine();
        assertNotNull(line, "server printed nothing");
        assertTrue(line.startsWith("SCASP RMI server ready"), line);
        assertNotEquals(ProcessHandle.current().pid(), server.pid(), "server runs in another process");
    }
    @AfterAll void stop() throws Exception { if (server != null) { server.destroy(); server.waitFor(10, TimeUnit.SECONDS); } }

    @Test @Order(1) void remoteOperationsReturnSerializableDtos() throws Exception {
        AdminRemote admin = RmiClient.connect("127.0.0.1", port);
        assertEquals(List.of("CSC301 - Advanced Java", "DB302 - Advanced Database", "HCI308 - Human Computer Interface"), admin.listCourses());
        assertEquals(2, admin.countRegistrations("CSC301"));
        assertEquals(List.of("STU003"), admin.getWaitingList("CSC301"));
        CourseSummaryDTO dto = admin.getCapacityReport("STAFF01").get(0);
        assertEquals(new CourseSummaryDTO("CSC301", "Advanced Java", 2, 2, 1, true), dto);
        assertEquals(1L, admin.getRequestWorkload("STAFF01").get("OPEN"));
        assertEquals("ANN001", admin.publishAnnouncement("STAFF01", "t", "b", "ALL"));
        assertEquals("PUBLISH_ANNOUNCEMENT", admin.getRecentAudit("STAFF01", 1).get(0).action());
        assertFalse(admin.getAuditReport().isEmpty());
    }
    @Test @Order(2) void passByValueCopiesDoNotChangeServerState() throws Exception {
        AdminRemote admin = RmiClient.connect("127.0.0.1", port);
        List<String> wl = admin.getWaitingList("CSC301");
        wl.add("HACKER");                       // modifies the client's copy only
        assertEquals(List.of("STU003"), admin.getWaitingList("CSC301"));
    }
    @Test @Order(3) void businessAndAuthorisationErrorsComeBackAsMeaningfulRemoteExceptions() throws Exception {
        AdminRemote admin = RmiClient.connect("127.0.0.1", port);
        RemoteAdminException denied = assertThrows(RemoteAdminException.class, () -> admin.getCapacityReport("STU001"));
        assertTrue(RmiClient.explain(denied).contains("Not authorised"));
        assertThrows(RemoteAdminException.class, () -> admin.countRegistrations("NOPE99"));
        assertThrows(RemoteAdminException.class, () -> admin.setCourseActive("STAFF01", "NOPE99", false));
    }
    @Test @Order(4) void registryUnavailableIsReportedMeaningfully() throws Exception {
        int nothingThere = freePort();
        RemoteException e = assertThrows(RemoteException.class, () -> RmiClient.connect("127.0.0.1", nothingThere));
        assertInstanceOf(ConnectException.class, e);
        assertTrue(RmiClient.explain(e).startsWith("Administration server unavailable"));
    }
    @Test @Order(5) void serverDyingMidSessionRaisesRemoteException() throws Exception {
        AdminRemote admin = RmiClient.connect("127.0.0.1", port);
        assertEquals(2, admin.countRegistrations("CSC301"));
        server.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
        assertThrows(RemoteException.class, admin::listCourses, "stale stub fails with RemoteException");
    }
}
