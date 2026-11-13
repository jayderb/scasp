package com.scasp.network;

import com.scasp.app.SampleData;
import com.scasp.model.Announcement;
import com.scasp.service.AcademicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CampusProtocolTest {
    AcademicService s; CampusProtocol p;
    @BeforeEach void setUp() { s = SampleData.load(new AcademicService()); p = new CampusProtocol(s); }

    @Test void validCommands() {
        assertEquals("OK PONG", p.handle("PING"));
        assertEquals("OK PONG", p.handle("ping\r"));
        assertTrue(p.handle("HELP").contains("REGISTER"));
        assertTrue(p.handle("COURSES").startsWith("OK CSC301,Advanced Java,2,ACTIVE;"));
        assertEquals("OK REGISTERED", p.handle("REGISTER|STU001|CSC301"));
        assertEquals("OK REGISTERED", p.handle("REGISTER|STU002|CSC301"));
        assertEquals("OK WAITLISTED", p.handle("REGISTER|STU003|CSC301"));
        assertEquals("OK CSC301 enrolled=2 capacity=2 waiting=1", p.handle("SEATS|csc301"));
        assertEquals("OK STU003", p.handle("WAITLIST|CSC301"));
        s.publishAnnouncement("STAFF01", "Exams", "Timetable", Announcement.Audience.STUDENTS);
        assertEquals("OK ANN001,Exams", p.handle("ANNOUNCEMENTS|students"));
        assertEquals("OK BYE", p.handle("QUIT"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "PING|extra", "SEATS", "SEATS|a|b", "REGISTER|STU001", "REGISTER|STU001|bad code!", "ANNOUNCEMENTS|EVERYONE"})
    void malformedMessagesGet400(String line) {
        assertTrue(p.handle(line).startsWith("ERR 400 MALFORMED"), () -> line + " -> " + p.handle(line));
    }
    @Test void errorsAreMappedToCodes() {
        assertTrue(p.handle("FLY").startsWith("ERR 400 UNKNOWN_COMMAND"));
        assertTrue(p.handle("SEATS|NOPE99").startsWith("ERR 404"));
        assertTrue(p.handle("REGISTER|NOBODY|CSC301").startsWith("ERR 403 FORBIDDEN"), "unknown actor");
        p.handle("REGISTER|STU001|CSC301");
        assertEquals("ERR 409 CONFLICT Duplicate registration", p.handle("REGISTER|STU001|CSC301"));
    }
}
