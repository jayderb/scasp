package com.scasp.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Stage 2 requirement 6: platform-thread pool vs virtual-thread-per-task for a blocking (I/O-like) workload.
 * Each task simulates a 20 ms blocking call (e.g. a database or network round trip). With a fixed pool of
 * N platform threads, throughput is limited to N concurrent waits; virtual threads park cheaply so many
 * more tasks wait concurrently. For CPU-bound work virtual threads give no advantage.
 */
public final class ThreadModelComparison {
    private ThreadModelComparison() {}

    public record Measurement(RequestProcessor.Mode mode, int tasks, int workersOrLimit, long elapsedMillis, ProcessorStats stats) {}

    public static Measurement measure(RequestProcessor.Mode mode, int tasks, int workers, long blockMillis) throws Exception {
        // queue capacity = tasks so nothing is rejected; for VIRTUAL the in-flight limit is tasks + workers
        try (RequestProcessor p = new RequestProcessor(tasks, workers, mode)) {
            long start = System.nanoTime();
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                final int n = i;
                futures.add(p.submit(() -> { Thread.sleep(blockMillis); return n; }, 5, TimeUnit.SECONDS));
            }
            for (Future<Integer> f : futures) f.get();
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            return new Measurement(mode, tasks, workers, elapsed, p.stats());
        }
    }

    public static void main(String[] args) throws Exception {
        int tasks = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
        int workers = 16; long block = 20;
        System.out.println("Workload: " + tasks + " tasks, each blocking " + block + " ms; JDK " + Runtime.version()
                + "; CPUs=" + Runtime.getRuntime().availableProcessors());
        measure(RequestProcessor.Mode.PLATFORM, 200, workers, block); // warm-up
        measure(RequestProcessor.Mode.VIRTUAL, 200, workers, block);  // warm-up
        for (RequestProcessor.Mode m : RequestProcessor.Mode.values()) {
            Measurement r = measure(m, tasks, workers, block);
            System.out.printf("%-8s workers=%s elapsed=%d ms  %s%n", m, m == RequestProcessor.Mode.PLATFORM ? workers : "per-task",
                    r.elapsedMillis(), r.stats());
        }
    }
}
