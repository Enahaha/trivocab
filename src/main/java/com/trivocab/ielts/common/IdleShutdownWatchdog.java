package com.trivocab.ielts.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Exits the desktop app shortly after the browser client stops heartbeating
 * (for example, when the user closes the page). Only active when shutdown is
 * explicitly allowed, so server and dev runs are unaffected.
 */
@Component
public class IdleShutdownWatchdog {
    private static final Logger log = LoggerFactory.getLogger(IdleShutdownWatchdog.class);
    private static final long IDLE_TIMEOUT_MS = 30_000L;

    private final DesktopLifecycle lifecycle;

    public IdleShutdownWatchdog(DesktopLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelay = 5000)
    public void shutdownWhenBrowserClosed() {
        if (!lifecycle.isShutdownAllowed()) {
            return;
        }
        long last = lifecycle.lastHeartbeatAt();
        if (last == 0L) {
            // 浏览器还没连上，先不判断超时。
            return;
        }
        if (System.currentTimeMillis() - last > IDLE_TIMEOUT_MS) {
            log.info("浏览器已关闭超过 {}ms，应用自动退出。", IDLE_TIMEOUT_MS);
            System.exit(0);
        }
    }
}
