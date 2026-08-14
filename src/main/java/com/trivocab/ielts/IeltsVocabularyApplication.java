package com.trivocab.ielts;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.trivocab.ielts.common.BrowserOpener;
import java.net.InetSocketAddress;
import java.net.Socket;

@MapperScan("com.trivocab.ielts.mapper")
@SpringBootApplication
@EnableScheduling
public class IeltsVocabularyApplication {

    public static void main(String[] args) {
        int port = Integer.getInteger("server.port", 8081);
        String url = System.getProperty("app.public-url", "http://localhost:" + port);
        if (isPortListening(port)) {
            // Another instance is already running: just bring the page up.
            openBrowser(url);
            return;
        }
        SpringApplication.run(IeltsVocabularyApplication.class, args);
    }

    private static boolean isPortListening(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 600);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void openBrowser(String url) {
        BrowserOpener.open(url);
    }
}
