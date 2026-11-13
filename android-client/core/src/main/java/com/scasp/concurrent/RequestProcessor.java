package com.scasp.concurrent;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Stage 2 concurrent request processor.
 *
 * <ul>
 *   <li>PLATFORM mode: a fixed pool of N platform worker threads fed by a bounded {@link ArrayBlockingQueue}.</li>
 *   <li>VIRTUAL mode: one virtual thread per task (JDK 21), still bounded by the same in-flight limit.</li>
 * </ul>
 * Back-pressure: a {@link Semaphore} with (queueCapacity + workers) permits limits the work in flight. A caller
 * waits up to the given timeout for a permit; if none frees up the task is <b>rejected</b> (counted, and a
 * {@link RejectedExecutionException} is thrown) instead of growing memory without bound.
 * Shutdown is orderly: no new work is accepted, queued work is allowed to finish within a grace period,
 * and only then are remaining tasks interrupted with shutdownNow.
 * Statistics use {@link LongAdder}/{@link LongAccumulator} so they are thread-safe without a global lock.
 */
public class RequestProcessor implements AutoCloseable {
    public enum Mode { PLATFORM, VIRTUAL }

    private final ExecutorService executor;
    private final Semaphore inFlight;
    private final Mode mode;
    private final int workers;
    private final int queueCapacity;
    private volatile boolean accepting = true;

    private final LongAdder submitted = new LongAdder(), succeeded = new LongAdder(), failed = new LongAdder(),
            rejected = new LongAdder(), timedOut = new LongAdder(), cancelled = new LongAdder(), totalNanos = new LongAdder();
    private final LongAccumulator maxNanos = new LongAccumulator(Long::max, 0);

    /** Original constructor kept for compatibility: platform threads. */
    public RequestProcessor(int capacity, int workerCount) { this(capacity, workerCount, Mode.PLATFORM); }

    public RequestProcessor(int queueCapacity, int workerCount, Mode mode) {
        if (queueCapacity < 1 || workerCount < 1) throw new IllegalArgumentException("queueCapacity and workerCount must be >= 1");
        this.mode = Objects.requireNonNull(mode);
        this.workers = workerCount;
        this.queueCapacity = queueCapacity;
        this.inFlight = new Semaphore(queueCapacity + workerCount);
        if (mode == Mode.PLATFORM) {
            ThreadFactory tf = Thread.ofPlatform().name("scasp-worker-", 1).factory();
            // The semaphore is the real bound; the executor queue has a little head-room so a worker that has
            // finished (permit released) but not yet returned to the pool cannot cause a spurious abort.
            ThreadPoolExecutor pool = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCapacity + workerCount), tf, new ThreadPoolExecutor.AbortPolicy());
            executor = pool;
        } else {
            executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("scasp-vthread-", 1).factory());
        }
    }

    public Mode mode() { return mode; }
    public int workers() { return workers; }
    public int queueCapacity() { return queueCapacity; }

    /**
     * Submits a task, waiting at most {@code timeout} for capacity (back-pressure).
     * @throws RejectedExecutionException if the processor is full for the whole timeout or shutting down
     */
    public <T> Future<T> submit(Callable<T> task, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(task);
        if (!accepting) { rejected.increment(); throw new RejectedExecutionException("Processor is shutting down"); }
        if (!inFlight.tryAcquire(timeout, unit)) { rejected.increment(); throw new RejectedExecutionException("Processor is busy (queue full)"); }
        try {
            // done() runs exactly once whether the task completes, fails or is cancelled while queued,
            // so the in-flight permit can never leak.
            FutureTask<T> f = new FutureTask<>(() -> runMeasured(task)) {
                @Override protected void done() { inFlight.release(); }
            };
            executor.execute(f);
            submitted.increment();
            return f;
        } catch (RejectedExecutionException e) {
            inFlight.release(); rejected.increment(); throw e;
        }
    }

    /** Original Runnable API kept: returns false (and counts a rejection) instead of throwing when full. */
    public boolean submit(Runnable task, long timeout, TimeUnit unit) throws InterruptedException {
        try { submit(Executors.callable(task), timeout, unit); return true; }
        catch (RejectedExecutionException e) { return false; }
    }

    /** Submits and waits for the result with a timeout; on timeout the task is cancelled (interrupted). */
    public <T> T invoke(Callable<T> task, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        Future<T> f = submit(task, timeout, unit);
        try {
            return f.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            timedOut.increment();
            f.cancel(true);
            throw e;
        }
    }

    private <T> T runMeasured(Callable<T> task) throws Exception {
        long start = System.nanoTime();
        try {
            T result = task.call();
            succeeded.increment();
            return result;
        } catch (InterruptedException e) {
            cancelled.increment(); failed.increment();
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception | Error e) {
            failed.increment();
            throw e;
        } finally {
            long elapsed = System.nanoTime() - start;
            totalNanos.add(elapsed); maxNanos.accumulate(elapsed);
        }
    }

    public ProcessorStats stats() {
        return new ProcessorStats(submitted.sum(), succeeded.sum(), failed.sum(), rejected.sum(), timedOut.sum(), cancelled.sum(),
                totalNanos.sum(), maxNanos.get());
    }
    /** Kept for compatibility with the original API. */
    public long completedCount() { return succeeded.sum() + failed.sum(); }
    public int availableCapacity() { return inFlight.availablePermits(); }
    public boolean isAccepting() { return accepting; }
    public boolean isTerminated() { return executor.isTerminated(); }

    /**
     * Orderly shutdown: stop accepting, let queued/running work finish for up to {@code grace},
     * then interrupt whatever is left. Returns the tasks that never started (empty if all completed).
     */
    public List<Runnable> shutdown(long grace, TimeUnit unit) throws InterruptedException {
        accepting = false;
        executor.shutdown();
        if (executor.awaitTermination(grace, unit)) return List.of();
        List<Runnable> neverStarted = executor.shutdownNow();
        executor.awaitTermination(grace, unit);
        return neverStarted;
    }

    @Override public void close() {
        try { shutdown(10, TimeUnit.SECONDS); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
