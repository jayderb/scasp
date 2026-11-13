package com.scasp.rmi;

import com.scasp.app.SampleData;
import com.scasp.model.ServiceRequest;
import com.scasp.service.AcademicService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;

/**
 * Starts an RMI registry and binds the administration service as "SCASPAdmin".
 * Usage: java -cp ... com.scasp.rmi.RmiServer [port]   (default 1099)
 * For a two-computer demo start with -Djava.rmi.server.hostname=&lt;server LAN IP&gt;.
 */
public class RmiServer {
    public static final String NAME = "SCASPAdmin";
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        AcademicService service = SampleData.load(new AcademicService());
        service.register("STU001", "CSC301"); service.register("STU002", "CSC301"); service.register("STU003", "CSC301");
        service.submitRequest(new ServiceRequest("STU002", "TRANSCRIPT", "Transcript for bursary", 3));
        Registry registry = LocateRegistry.createRegistry(port);
        AdminRemoteImpl impl = new AdminRemoteImpl(service);
        registry.rebind(NAME, impl);
        CountDownLatch stop = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { registry.unbind(NAME); UnicastRemoteObject.unexportObject(impl, true); UnicastRemoteObject.unexportObject(registry, true); }
            catch (Exception ignored) { }
            stop.countDown();
        }));
        System.out.println("SCASP RMI server ready on port " + port + " as " + NAME);
        System.out.flush();
        stop.await();
    }
}
