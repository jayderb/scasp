package com.scasp.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stage 2 requirement 7: a deliberate race condition and its correction.
 *
 * <p>{@link UnsafeSeatCounter} performs check-then-act ("if seats remain, take one") without synchronisation.
 * Two threads can both see the last free seat and both take it, so enrolment exceeds capacity and
 * the counter also loses updates ({@code count++} is read-modify-write). {@link SafeSeatCounter} makes the
 * check and the update one atomic step with a CAS loop on an AtomicInteger.
 * Run {@code main} to reproduce; evidence is saved in docs/evidence/race-condition-evidence.txt.
 */
public final class RaceConditionDemo {
    private RaceConditionDemo() {}

    public interface SeatCounter { boolean tryTakeSeat(); int taken(); int capacity(); }

    /** BROKEN on purpose: check-then-act race. Do not use in the service. */
    public static final class UnsafeSeatCounter implements SeatCounter {
        private final int capacity; private int taken;   // not volatile, not synchronized
        public UnsafeSeatCounter(int capacity) { this.capacity = capacity; }
        public boolean tryTakeSeat() {
            if (taken < capacity) {          // check
                Thread.yield();              // widen the window so the race is easy to observe
                taken++;                     // act (non-atomic read-modify-write)
                return true;
            }
            return false;
        }
        public int taken() { return taken; }
        public int capacity() { return capacity; }
    }

    /** Corrected: check and update happen atomically via compare-and-set. */
    public static final class SafeSeatCounter implements SeatCounter {
        private final int capacity; private final AtomicInteger taken = new AtomicInteger();
        public SafeSeatCounter(int capacity) { this.capacity = capacity; }
        public boolean tryTakeSeat() {
            while (true) {
                int current = taken.get();
                if (current >= capacity) return false;
                if (taken.compareAndSet(current, current + 1)) return true;
            }
        }
        public int taken() { return taken.get(); }
        public int capacity() { return capacity; }
    }

    /** Result of one contention run. */
    public record Result(String counter, int threads, int attemptsPerThread, int capacity, int successfulTakes, int finalCount) {
        public boolean invariantHeld() { return successfulTakes <= capacity && finalCount == successfulTakes; }
    }

    /** All threads are released together by a barrier to maximise contention; a latch waits for completion. */
    public static Result run(SeatCounter counter, int threads, int attemptsPerThread) throws Exception {
        CyclicBarrier start = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = Thread.ofPlatform().name("race-" + i).start(() -> {
                try {
                    start.await();
                    for (int a = 0; a < attemptsPerThread; a++) if (counter.tryTakeSeat()) successes.incrementAndGet();
                } catch (Exception e) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
            ts.add(t);
        }
        done.await();
        for (Thread t : ts) t.join();
        return new Result(counter.getClass().getSimpleName(), threads, attemptsPerThread, counter.capacity(), successes.get(), counter.taken());
    }

    public static void main(String[] args) throws Exception {
        int threads = 16, attempts = 2000, capacity = 1000;
        System.out.println("Race-condition demonstration: " + threads + " threads x " + attempts + " attempts, capacity " + capacity);
        for (int run = 1; run <= 5; run++) {
            Result u = run(new UnsafeSeatCounter(capacity), threads, attempts);
            System.out.printf("run %d UNSAFE: successfulTakes=%d finalCount=%d capacity=%d invariantHeld=%s%n",
                    run, u.successfulTakes(), u.finalCount(), u.capacity(), u.invariantHeld());
        }
        for (int run = 1; run <= 5; run++) {
            Result s = run(new SafeSeatCounter(capacity), threads, attempts);
            System.out.printf("run %d SAFE:   successfulTakes=%d finalCount=%d capacity=%d invariantHeld=%s%n",
                    run, s.successfulTakes(), s.finalCount(), s.capacity(), s.invariantHeld());
        }
    }
}
