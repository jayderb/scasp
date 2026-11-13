package com.scasp.concurrent;

/** Immutable snapshot of RequestProcessor statistics. */
public record ProcessorStats(long submitted, long succeeded, long failed, long rejected, long timedOut, long cancelled,
                             long totalProcessingNanos, long maxProcessingNanos) {
    public long completed() { return succeeded + failed; }
    public double averageProcessingMillis() { return completed() == 0 ? 0 : totalProcessingNanos / 1_000_000.0 / completed(); }
    @Override public String toString() {
        return String.format("submitted=%d succeeded=%d failed=%d rejected=%d timedOut=%d cancelled=%d avg=%.3fms max=%.3fms",
                submitted, succeeded, failed, rejected, timedOut, cancelled, averageProcessingMillis(), maxProcessingNanos / 1_000_000.0);
    }
}
