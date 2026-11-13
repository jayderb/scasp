package com.scasp.concurrent;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RaceConditionTest {
    /** The corrected counter must hold the invariant under maximum contention, every time. */
    @RepeatedTest(5) void safeCounterNeverOverbooks() throws Exception {
        RaceConditionDemo.Result r = RaceConditionDemo.run(new RaceConditionDemo.SafeSeatCounter(500), 8, 1000);
        assertEquals(500, r.successfulTakes()); assertEquals(500, r.finalCount()); assertTrue(r.invariantHeld());
    }
    /** Single-threaded the unsafe counter is "correct" - the bug only appears under concurrency (see evidence file). */
    @Test void unsafeCounterIsCorrectWithoutConcurrency() throws Exception {
        RaceConditionDemo.Result r = RaceConditionDemo.run(new RaceConditionDemo.UnsafeSeatCounter(500), 1, 1000);
        assertEquals(500, r.successfulTakes()); assertTrue(r.invariantHeld());
    }
}
