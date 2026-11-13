package com.scasp.web;

import com.scasp.report.CapacityRow;
import jakarta.el.ELProcessor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordELResolverTest {
    @Test void resolvesRecordComponentsInExpressions() {
        org.junit.jupiter.api.Assumptions.assumeTrue(elImplementationPresent(), "no EL implementation on the test classpath");
        ELProcessor el = new ELProcessor();
        el.getELManager().addELResolver(new RecordELResolver());
        el.defineBean("row", new CapacityRow("CSC301", "Advanced Java", 2, 1, 0, true));
        assertEquals("CSC301", el.eval("row.courseCode"));
        assertEquals(1, ((Number) el.eval("row.enrolled")).intValue());
    }
    private static boolean elImplementationPresent() {
        try { new ELProcessor(); return true; } catch (Throwable t) { return false; }
    }
}
