package com.trivocab.ielts.common;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens the default browser. The macOS {@code open} command is preferred because
 * Desktop.browse can fail silently for background (LSUIElement) apps.
 */
public final class BrowserOpener {
    private BrowserOpener() {
    }

    public static void open(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return;
            }
            if (os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
                return;
            }
            if (os.contains("win")) {
                openWindowsBrowser(url);
                return;
            }
        } catch (Exception ignored) {
            // Fall back to Desktop below.
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // Opening the browser is best-effort.
        }
    }

    private static void openWindowsBrowser(String url) {
        // App-mode windows allow the page to close itself with window.close(),
        // so quitting the app can also close the browser window.
        String[] edgeCandidates = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                "msedge.exe",
        };
        for (String edge : edgeCandidates) {
            try {
                new ProcessBuilder(edge, "--app=" + url).start();
                return;
            } catch (Exception ignored) {
                // Try the next candidate.
            }
        }
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (Exception ignored) {
            // Fall back to Desktop below.
        }
    }
}
