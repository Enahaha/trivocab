package com.trivocab.ielts.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Opens the study page in the default browser after startup. Only enabled in
 * packaged desktop builds (jpackage passes app.open-browser-on-start=true).
 */
@Component
public class BrowserLauncher implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    private final boolean enabled;
    private final String url;

    public BrowserLauncher(
            @Value("${app.open-browser-on-start:false}") boolean enabled,
            @Value("${app.public-url:http://localhost:8081}") String url
    ) {
        this.enabled = enabled;
        this.url = url;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        BrowserOpener.open(url);
    }
}
