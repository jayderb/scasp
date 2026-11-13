package com.scasp.concurrent;

import com.scasp.app.SampleData;
import com.scasp.service.AcademicService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Starts a RequestProcessor whose workers are parked on a latch so a thread dump shows the named
 * worker threads (scasp-worker-N) WAITING inside a task while further tasks sit in the bounded queue.
 * Capture with: jcmd &lt;pid&gt; Thread.print  (or jstack &lt;pid&gt;). Stops after 60 s or when stdin closes.
 */
public final class ThreadDumpDemo {
    private ThreadDumpDemo() {}
    public static void main(String[] args) throws Exception {
        AcademicService service = SampleData.load(new AcademicService());
        CountDownLatch gate = new CountDownLatch(1);
        try (RequestProcessor p = new RequestProcessor(4, 3)) {
            for (int i = 0; i < 3; i++) p.submit(() -> { gate.await(); return "held"; }, 1, TimeUnit.SECONDS);
            p.submit(ScaspTasks.register(service, "STU001", "STU001", "CSC301"), 1, TimeUnit.SECONDS);
            System.out.println("READY pid=" + ProcessHandle.current().pid() + " stats=" + p.stats());
            System.out.flush();
            gate.await(args.length > 0 ? Long.parseLong(args[0]) : 60, TimeUnit.SECONDS);
            gate.countDown();
        }
        System.out.println("Stopped. enrolled CSC301=" + service.enrolledStudents("CSC301"));
    }
}
