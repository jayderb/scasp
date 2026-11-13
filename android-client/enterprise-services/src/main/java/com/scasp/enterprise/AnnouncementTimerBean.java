package com.scasp.enterprise;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;

import java.time.Instant;
import java.util.logging.Logger;

/**
 * Container-managed calendar timer (Stage 4 requirement 6). A non-persistent timer fires every 5 minutes as a
 * platform heartbeat and updates the metrics singleton; a daily 00:00 timer logs a maintenance marker.
 */
@Singleton
@Startup
public class AnnouncementTimerBean {
    private static final Logger LOG = Logger.getLogger(AnnouncementTimerBean.class.getName());
    @EJB private PlatformMetricsBean metrics;

    @Schedule(hour = "*", minute = "*/5", persistent = false, info = "scasp-heartbeat")
    public void heartbeat(Timer timer) {
        metrics.recordHeartbeat();
        LOG.info("SCASP heartbeat " + Instant.now() + " (" + timer.getInfo() + ")");
    }
    @Schedule(hour = "0", minute = "0", second = "0", persistent = false, info = "scasp-daily")
    public void daily(Timer timer) { LOG.info("SCASP daily maintenance marker " + Instant.now()); }
}
