package com.scasp.web;

import com.scasp.model.CampusService;
import com.scasp.model.Role;
import com.scasp.model.ServiceRequest;
import com.scasp.model.ServiceRequestSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void writesNestedStructuresInOrder() {
        String json = Json.write(Json.obj("a", 1, "b", List.of(true, false), "c", null, "d", Json.obj("x", 2.5), "e", Role.STAFF));
        assertEquals("{\"a\":1,\"b\":[true,false],\"c\":null,\"d\":{\"x\":2.5},\"e\":\"STAFF\"}", json);
        assertEquals("[]", Json.write(List.of()));
        assertEquals("{}", Json.write(Map.of()));
    }
    @Test void escapesStringsSoTheyCannotBreakOutOrBecomeHtml() {
        assertEquals("\"q\\\"\\\\\\n\\t\\u003cscript\\u003e\\u0026\\u0001\"", Json.write("q\"\\\n\t<script>&\u0001"));
        assertEquals("\"Zambia \u2013 Lusaka\"", Json.write("Zambia \u2013 Lusaka"), "non-ASCII stays UTF-8");
        assertEquals("null", Json.write(Double.NaN));
        assertEquals("\"2026-11-13T10:15\"", Json.write(LocalDateTime.of(2026, 11, 13, 10, 15)));
    }
    @Test void oddNumberOfArgumentsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Json.obj("only-key"));
    }
    @Test void apiShapesExposeNoSecrets() {
        String user = Json.write(MobileApiServlet.userJson(new CurrentUser("STU001", "Alex Banda", Role.STUDENT), null));
        assertEquals("{\"userId\":\"STU001\",\"name\":\"Alex Banda\",\"role\":\"STUDENT\",\"staff\":false,\"csrfToken\":null}", user);
        assertFalse(user.contains("password") || user.contains("hash"));
        String svc = Json.write(MobileApiServlet.serviceJson(new CampusService("LIB", "Main Library", "Books", -15.4167, 28.2833)));
        assertTrue(svc.contains("\"latitude\":-15.4167") && svc.contains("\"illustrative\":true"), svc);
        var req = new ServiceRequestSummary("7", "STU001", "TRANSCRIPT", "Need it", 4, ServiceRequest.Status.OPEN, null,
                LocalDateTime.of(2026, 11, 13, 9, 0), LocalDateTime.of(2026, 11, 13, 9, 0));
        assertEquals("{\"requestId\":\"7\",\"studentId\":\"STU001\",\"category\":\"TRANSCRIPT\",\"description\":\"Need it\",\"priority\":4,"
                + "\"status\":\"OPEN\",\"assignedTo\":null,\"createdAt\":\"2026-11-13T09:00\"}", Json.write(MobileApiServlet.requestJson(req)));
    }
}
