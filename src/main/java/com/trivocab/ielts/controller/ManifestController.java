package com.trivocab.ielts.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the PWA manifest with the correct media type so iOS/Android home
 * screen installation is recognized.
 */
@RestController
public class ManifestController {

    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    public Resource manifest() {
        return new ClassPathResource("static/manifest.webmanifest");
    }
}
