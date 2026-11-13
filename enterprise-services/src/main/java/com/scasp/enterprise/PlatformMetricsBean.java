package com.scasp.enterprise;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * Shared application metrics (Stage 4 singleton). Bean-managed concurrency is used because the counters are
 * LongAdders, which are already thread-safe; container WRITE locks would serialise every caller for nothing.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class PlatformMetricsBean {
    private static final Logger LOG = Logger.getLogger(PlatformMetricsBean.class.getName());
    private final LongAdder registrations = new LongAdder();
    private final LongAdder withdrawals = new LongAdder();
    private final LongAdder requests = new LongAdder();
    private final LongAdder heartbeats = new LongAdder();
    private volatile Instant startedAt;

    @PostConstruct public void start() { startedAt = Instant.now(); LOG.info("SCASP metrics singleton started at " + startedAt); }
    public void recordRegistration() { registrations.increment(); }
    public void recordWithdrawal() { withdrawals.increment(); }
    public void recordRequest() { requests.increment(); }
    public void recordHeartbeat() { heartbeats.increment(); }
    public long getRegistrationCount() { return registrations.sum(); }
    public long getWithdrawalCount() { return withdrawals.sum(); }
    public long getRequestCount() { return requests.sum(); }
    public long getHeartbeatCount() { return heartbeats.sum(); }
    public Instant getStartedAt() { return startedAt; }
}
