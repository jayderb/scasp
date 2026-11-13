package com.scasp.concurrent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ThreadModelComparisonTest {
    /** Functional check only (timings are recorded as evidence, never asserted). */
    @ParameterizedTest @EnumSource(RequestProcessor.Mode.class)
    void bothModesCompleteTheWorkload(RequestProcessor.Mode mode) throws Exception {
        ThreadModelComparison.Measurement m = ThreadModelComparison.measure(mode, 100, 4, 1);
        assertEquals(100, m.stats().succeeded()); assertEquals(0, m.stats().rejected());
    }
}
