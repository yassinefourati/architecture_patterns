package com.application.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates a flaky external dependency.
 *  - When healthy: fails ~30% of the time, otherwise responds in ~50ms
 *  - When toggled unhealthy: every call fails fast
 *  - Random slow calls (>2s) trigger the time limiter
 *
 * Toggle health via REST: POST /admin/downstream/health/{up|down}
 */
@Component
public class FlakyDownstream {

    private static final Logger log = LoggerFactory.getLogger(FlakyDownstream.class);
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public String call(String input) throws InterruptedException {
        if (!healthy.get()) {
            throw new DownstreamException("Downstream is DOWN");
        }
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 10) {
            // 10% chance of being very slow â†’ triggers TimeLimiter
            log.info("[downstream] simulating slow call (3s)");
            Thread.sleep(3_000);
            return "slow response for " + input;
        }
        if (roll < 40) {
            throw new DownstreamException("Random transient failure");
        }
        Thread.sleep(50);
        return "ok: " + input;
    }

    public void setHealthy(boolean up) {
        healthy.set(up);
        log.info("[downstream] health toggled â†’ {}", up ? "UP" : "DOWN");
    }

    public boolean isHealthy() { return healthy.get(); }

    public static class DownstreamException extends RuntimeException {
        public DownstreamException(String msg) { super(msg); }
    }
}
