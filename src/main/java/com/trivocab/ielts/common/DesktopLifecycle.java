package com.trivocab.ielts.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tracks whether a browser client is still alive for packaged desktop builds.
 * The frontend sends a lightweight heartbeat; when heartbeats stop, the idle
 * watchdog exits the app. In server/dev runs this is a no-op because
 * app.allow-shutdown defaults to false.
 */
@Component
public class DesktopLifecycle {
    private final boolean allowShutdown;
    private volatile long lastHeartbeatAt = 0L;

    public DesktopLifecycle(
            @Value("${app.allow-shutdown:false}") boolean allowShutdown
    ) {
        this.allowShutdown = allowShutdown;
    }

    public boolean isShutdownAllowed() {
        return allowShutdown;
    }

    public void heartbeat() {
        this.lastHeartbeatAt = System.currentTimeMillis();
    }

    public long lastHeartbeatAt() {
        return lastHeartbeatAt;
    }
}
