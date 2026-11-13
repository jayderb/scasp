package com.scasp.rmi;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Command-line administration client. Usage: RmiClient [host] [port] [staffId]
 * Every remote failure is turned into a meaningful message (exit code 2) instead of a stack trace.
 */
public class RmiClient {
    public static AdminRemote connect(String host, int port) throws RemoteException, NotBoundException {
        return (AdminRemote) LocateRegistry.getRegistry(host, port).lookup(RmiServer.NAME);
    }
    public static String explain(Exception e) {
        if (e instanceof ConnectException) return "Administration server unavailable (registry not reachable): " + e.getCause();
        if (e instanceof NotBoundException) return "Registry reachable but '" + RmiServer.NAME + "' is not bound - is the SCASP RMI server running?";
        if (e instanceof RemoteAdminException) return "Request refused: " + e.getMessage();
        if (e instanceof RemoteException) return "Remote call failed: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        return "Unexpected error: " + e;
    }
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 1099;
        String staff = args.length > 2 ? args[2] : "STAFF01";
        try {
            AdminRemote admin = connect(host, port);
            System.out.println("Courses: " + admin.listCourses());
            System.out.println("CSC301 registrations: " + admin.countRegistrations("CSC301"));
            System.out.println("CSC301 waiting list: " + admin.getWaitingList("CSC301"));
            System.out.println("Capacity report (DTOs):");
            admin.getCapacityReport(staff).forEach(d -> System.out.println("  " + d));
            System.out.println("Request workload: " + admin.getRequestWorkload(staff));
            System.out.println("Published announcement id: " + admin.publishAnnouncement(staff, "RMI notice", "Sent from the admin client", "STAFF"));
            System.out.println("Recent audit:");
            admin.getRecentAudit(staff, 3).forEach(d -> System.out.println("  " + d));
            try { admin.getCapacityReport("STU001"); }
            catch (RemoteAdminException e) { System.out.println("As STU001: " + explain(e)); }
        } catch (Exception e) {
            System.err.println(explain(e));
            System.exit(2);
        }
    }
}
