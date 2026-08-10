package com.trivocab.ielts.controller;

import com.trivocab.ielts.common.ApiResponse;
import com.trivocab.ielts.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Desktop-only controls for the packaged app. Shutdown is opt-in via
 * app.allow-shutdown=true (set by the jpackage build). Any logged-in user may
 * quit the personal desktop app; server deployments keep the flag off.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final boolean allowShutdown;

    public SystemController(
            @Value("${app.allow-shutdown:false}") boolean allowShutdown
    ) {
        this.allowShutdown = allowShutdown;
    }

    @PostMapping("/shutdown")
    public ApiResponse<Void> shutdown() {
        if (!allowShutdown) {
            throw new ForbiddenException("当前环境不支持退出应用");
        }
        Thread shutdown = new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            closeBrowserTabs();
            System.exit(0);
        }, "trivocab-shutdown");
        shutdown.start();
        return ApiResponse.ok(null, "应用即将退出");
    }

    /**
     * Best-effort: close browser tabs pointing at the app before exiting.
     * Only meaningful in the packaged macOS app.
     */
    private void closeBrowserTabs() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return;
        }
        String closeScript = """
                try
                  tell application "Google Chrome"
                    set targets to {}
                    repeat with w in windows
                      repeat with t in tabs of w
                        if URL of t contains "8090" then set end of targets to t
                      end repeat
                    end repeat
                    repeat with t in targets
                      close t
                    end repeat
                  end tell
                end try
                """;
        String safariScript = """
                try
                  tell application "Safari"
                    set targets to {}
                    repeat with w in windows
                      repeat with t in tabs of w
                        if URL of t contains "8090" then set end of targets to t
                      end repeat
                    end repeat
                    repeat with t in targets
                      close t
                    end repeat
                  end tell
                end try
                """;
        for (String script : List.of(closeScript, safariScript)) {
            try {
                new ProcessBuilder("osascript", "-e", script).start();
            } catch (Exception ignored) {
                // Closing the browser is best-effort.
            }
        }
    }
}
