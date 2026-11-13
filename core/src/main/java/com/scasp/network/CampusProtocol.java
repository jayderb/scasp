package com.scasp.network;

import com.scasp.model.Announcement;
import com.scasp.model.Course;
import com.scasp.service.AcademicService;
import com.scasp.service.AuthorisationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SCASP gateway protocol (see docs/architecture/network-protocol.md).
 * Framing: one UTF-8 request line terminated by LF (CRLF accepted), max {@value #MAX_LINE} bytes.
 * Fields are separated by '|'. Every request gets exactly one response line:
 * {@code OK <payload>} or {@code ERR <code> <REASON> <message>}.
 * Pure function of (service, line) so it can be unit tested without sockets.
 */
public final class CampusProtocol {
    public static final int MAX_LINE = 1024;
    public static final String COMMANDS = "PING|HELP|COURSES|SEATS|REGISTER|WAITLIST|ANNOUNCEMENTS|QUIT";
    private static final String ID = "[A-Za-z0-9_-]{2,30}";

    private final AcademicService service;
    public CampusProtocol(AcademicService service) { this.service = service; }

    public String handle(String line) {
        if (line == null || line.isBlank()) return err(400, "MALFORMED", "Empty request");
        String[] p = line.strip().split("\\|", -1);
        String cmd = p[0].toUpperCase();
        try {
            return switch (cmd) {
                case "PING" -> args(p, 0) ? "OK PONG" : arity(cmd, 0);
                case "HELP" -> args(p, 0) ? "OK " + COMMANDS : arity(cmd, 0);
                case "COURSES" -> args(p, 0) ? "OK " + service.listCourses().stream()
                        .map(c -> c.getCourseCode() + "," + clean(c.getTitle()) + "," + c.getCapacity() + "," + c.getStatus())
                        .collect(Collectors.joining(";")) : arity(cmd, 0);
                case "SEATS" -> {
                    if (!args(p, 1) || !p[1].matches(ID)) yield arity(cmd, 1);
                    Course c = service.findCourse(p[1]).orElse(null);
                    if (c == null) yield err(404, "NOT_FOUND", "Unknown course");
                    yield "OK " + c.getCourseCode() + " enrolled=" + service.enrolledCount(c.getCourseCode()) + " capacity="
                            + c.getCapacity() + " waiting=" + service.waitingList(c.getCourseCode()).size();
                }
                case "REGISTER" -> {
                    if (!args(p, 2) || !p[1].matches(ID) || !p[2].matches(ID)) yield arity(cmd, 2);
                    yield "OK " + service.register(p[1], p[1], p[2]);
                }
                case "WAITLIST" -> {
                    if (!args(p, 1) || !p[1].matches(ID)) yield arity(cmd, 1);
                    yield "OK " + String.join(",", service.waitingList(p[1]));
                }
                case "ANNOUNCEMENTS" -> {
                    if (!args(p, 1)) yield arity(cmd, 1);
                    Announcement.Audience a;
                    try { a = Announcement.Audience.valueOf(p[1].toUpperCase()); }
                    catch (IllegalArgumentException e) { yield err(400, "MALFORMED", "Audience must be ALL, STUDENTS or STAFF"); }
                    List<Announcement> list = service.announcementsFor(a);
                    yield "OK " + list.stream().map(x -> x.announcementId() + "," + clean(x.title())).collect(Collectors.joining(";"));
                }
                case "QUIT" -> "OK BYE";
                default -> err(400, "UNKNOWN_COMMAND", "Use HELP");
            };
        } catch (AuthorisationException e) {
            return err(403, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            return err(404, "NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            return err(409, "CONFLICT", e.getMessage());
        } catch (RuntimeException e) {
            return err(500, "SERVER_ERROR", "Request could not be processed");
        }
    }
    private static boolean args(String[] p, int n) { return p.length == n + 1; }
    private static String arity(String cmd, int n) { return err(400, "MALFORMED", cmd + " expects " + n + " argument(s)"); }
    static String err(int code, String reason, String msg) { return "ERR " + code + " " + reason + " " + clean(msg); }
    private static String clean(String s) { return s == null ? "" : s.replaceAll("[|;,\\r\\n]", " "); }
}
