package com.scasp.concurrent;

import com.scasp.app.SampleData;
import com.scasp.service.AcademicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic concurrency tests: latches/barriers control interleavings; no sleeps are used for timing. */
@Timeout(20)
class RequestProcessorTest {

    @ParameterizedTest @EnumSource(RequestProcessor.Mode.class)
    void boundedQueueAppliesBackPressureAndRejects(RequestProcessor.Mode mode) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        try (RequestProcessor p = new RequestProcessor(1, 1, mode)) {      // 1 running + 1 queued = 2 permits
            Future<String> a = p.submit(() -> { started.countDown(); release.await(); return "a"; }, 1, TimeUnit.SECONDS);
            started.await();
            Future<String> b = p.submit(() -> { release.await(); return "b"; }, 1, TimeUnit.SECONDS);
            assertEquals(0, p.availableCapacity());
            assertThrows(RejectedExecutionException.class, () -> p.submit(() -> "c", 10, TimeUnit.MILLISECONDS));
            assertFalse(p.submit(() -> {}, 10, TimeUnit.MILLISECONDS), "Runnable API reports rejection as false");
            release.countDown();
            assertEquals("a", a.get()); assertEquals("b", b.get());
            ProcessorStats st = p.stats();
            assertEquals(2, st.submitted()); assertEquals(2, st.succeeded()); assertEquals(2, st.rejected()); assertEquals(0, st.failed());
        }
    }

    @Test void failuresAreCountedAndPropagated() throws Exception {
        try (RequestProcessor p = new RequestProcessor(4, 2)) {
            Future<Object> f = p.submit(() -> { throw new IllegalStateException("boom"); }, 1, TimeUnit.SECONDS);
            ExecutionException e = assertThrows(ExecutionException.class, f::get);
            assertInstanceOf(IllegalStateException.class, e.getCause());
            assertEquals("ok", p.submit(() -> "ok", 1, TimeUnit.SECONDS).get());
            p.shutdown(5, TimeUnit.SECONDS);
            assertEquals(1, p.stats().failed()); assertEquals(1, p.stats().succeeded());
            assertTrue(p.stats().totalProcessingNanos() > 0);
        }
    }

    @Test void orderlyShutdownCompletesQueuedWorkInsteadOfDroppingIt() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger done = new AtomicInteger();
        RequestProcessor p = new RequestProcessor(10, 1);
        p.submit(() -> { release.await(); return done.incrementAndGet(); }, 1, TimeUnit.SECONDS);
        for (int i = 0; i < 5; i++) p.submit(done::incrementAndGet, 1, TimeUnit.SECONDS);
        Thread shutter = Thread.ofPlatform().start(() -> {
            try { assertTrue(p.shutdown(10, TimeUnit.SECONDS).isEmpty()); } catch (InterruptedException e) { throw new RuntimeException(e); }
        });
        // after shutdown starts, new work is refused
        while (p.isAccepting()) Thread.onSpinWait();
        assertThrows(RejectedExecutionException.class, () -> p.submit(() -> 1, 10, TimeUnit.MILLISECONDS));
        release.countDown();
        shutter.join();
        assertEquals(6, done.get(), "all queued tasks ran");
        assertTrue(p.isTerminated());
    }

    @Test void shutdownInterruptsStuckTasksAfterGracePeriod() throws Exception {
        CountDownLatch never = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        RequestProcessor p = new RequestProcessor(5, 1);
        Future<String> stuck = p.submit(() -> { started.countDown(); never.await(); return "x"; }, 1, TimeUnit.SECONDS);
        p.submit(() -> "never runs", 1, TimeUnit.SECONDS);
        started.await();
        List<Runnable> notStarted = p.shutdown(100, TimeUnit.MILLISECONDS);
        assertEquals(1, notStarted.size(), "queued task returned, not silently lost");
        assertThrows(ExecutionException.class, stuck::get);
        assertEquals(1, p.stats().cancelled(), "interrupt observed by the task");
        assertTrue(p.isTerminated());
    }

    @Test void invokeTimesOutAndCancels() throws Exception {
        CountDownLatch never = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        try (RequestProcessor p = new RequestProcessor(2, 1)) {
            assertThrows(TimeoutException.class, () -> p.invoke(() -> {
                try { never.await(); } catch (InterruptedException e) { interrupted.countDown(); throw e; }
                return 1;
            }, 50, TimeUnit.MILLISECONDS));
            assertTrue(interrupted.await(5, TimeUnit.SECONDS), "timed-out task was interrupted");
            assertEquals(1, p.stats().timedOut());
            assertEquals(42, p.invoke(() -> 42, 5, TimeUnit.SECONDS), "processor still usable");
        }
    }

    @ParameterizedTest @EnumSource(RequestProcessor.Mode.class)
    void concurrentRegistrationsNeverExceedCapacityOrDuplicate(RequestProcessor.Mode mode) throws Exception {
        AcademicService s = new AcademicService();
        s.addCourse(new com.scasp.model.Course("CON100", "Concurrency", 10));
        int n = 60;
        for (int i = 0; i < n; i++) s.addStudent(new com.scasp.model.Student("S" + i, "Student " + i, "CS", 1));
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Future<String>> results = new ArrayList<>();
        try (RequestProcessor p = new RequestProcessor(n, n, mode)) {
            for (int i = 0; i < n; i++) {
                String id = "S" + i;
                results.add(p.submit(() -> { barrier.await(); return s.register(id, id, "CON100"); }, 1, TimeUnit.SECONDS));
            }
            long registered = 0, waitlisted = 0;
            for (Future<String> f : results) { if (f.get().equals("REGISTERED")) registered++; else waitlisted++; }
            assertEquals(10, registered); assertEquals(50, waitlisted);
        }
        assertEquals(10, s.enrolledCount("CON100"));
        assertEquals(50, new HashSet<>(s.waitingList("CON100")).size());
    }

    @Test void businessTasksRunOnProcessor() throws Exception {
        AcademicService s = SampleData.load(new AcademicService());
        try (RequestProcessor p = new RequestProcessor(8, 2)) {
            assertEquals("REGISTERED", p.submit(ScaspTasks.register(s, "STU001", "STU001", "CSC301"), 1, TimeUnit.SECONDS).get());
            assertEquals(1, p.submit(ScaspTasks.capacityReport(s, "STAFF01"), 1, TimeUnit.SECONDS).get().get(0).enrolled());
            p.submit(ScaspTasks.submitRequest(s, new com.scasp.model.ServiceRequest("STU001", "TRANSCRIPT", "t", 3)), 1, TimeUnit.SECONDS).get();
            assertTrue(p.submit(ScaspTasks.assignNextRequest(s, "STAFF01"), 1, TimeUnit.SECONDS).get().isPresent());
            assertTrue(p.submit(ScaspTasks.withdraw(s, "STU001", "STU001", "CSC301"), 1, TimeUnit.SECONDS).get().isEmpty());
            ExecutionException denied = assertThrows(ExecutionException.class,
                    () -> p.submit(ScaspTasks.capacityReport(s, "STU001"), 1, TimeUnit.SECONDS).get());
            assertInstanceOf(com.scasp.service.AuthorisationException.class, denied.getCause());
        }
    }

    @Test void virtualModeUsesVirtualThreads() throws Exception {
        try (RequestProcessor p = new RequestProcessor(2, 1, RequestProcessor.Mode.VIRTUAL)) {
            assertTrue(p.submit(() -> Thread.currentThread().isVirtual(), 1, TimeUnit.SECONDS).get());
        }
        try (RequestProcessor p = new RequestProcessor(2, 1, RequestProcessor.Mode.PLATFORM)) {
            assertFalse(p.submit(() -> Thread.currentThread().isVirtual(), 1, TimeUnit.SECONDS).get());
            assertTrue(p.submit(() -> Thread.currentThread().getName(), 1, TimeUnit.SECONDS).get().startsWith("scasp-worker-"));
        }
    }

    @Test void invalidConfigurationRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RequestProcessor(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RequestProcessor(1, 0));
    }
}
