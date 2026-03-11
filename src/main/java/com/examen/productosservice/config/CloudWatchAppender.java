package com.examen.productosservice.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Custom Logback appender that sends logs to LocalStack CloudWatch Logs.
 * Uses Java 21 built-in HttpClient — zero extra Maven dependencies.
 */
public class CloudWatchAppender extends AppenderBase<ILoggingEvent> {

    // --- Configurable via logback-spring.xml ---
    private String endpoint    = "http://localstack:4566";
    private String logGroupName;

    // Nombre fijo, configurable desde logback-spring.xml
    private String logStreamName = "productos-stream";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final BlockingQueue<ILoggingEvent> queue = new LinkedBlockingQueue<>(500);
    private ScheduledExecutorService scheduler;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void start() {
        createStream();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cw-appender");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flush, 5, 10, TimeUnit.SECONDS);
        super.start();
    }

    @Override
    public void stop() {
        flush();
        if (scheduler != null) scheduler.shutdownNow();
        super.stop();
    }

    // ── Appender ─────────────────────────────────────────────────────────────

    @Override
    protected void append(ILoggingEvent event) {
        // materialize message now (MDC etc. are cleared after the call returns)
        event.prepareForDeferredProcessing();
        queue.offer(event);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void flush() {
        List<ILoggingEvent> events = new ArrayList<>();
        queue.drainTo(events, 50);
        if (events.isEmpty()) return;

        StringBuilder sb = new StringBuilder()
                .append("{\"logGroupName\":\"").append(logGroupName).append("\"")
                .append(",\"logStreamName\":\"").append(logStreamName).append("\"")
                .append(",\"logEvents\":[");
        for (int i = 0; i < events.size(); i++) {
            ILoggingEvent e = events.get(i);
            String msg = sanitize("[" + e.getLevel() + "] " + e.getLoggerName() + " - " + e.getFormattedMessage());
            if (i > 0) sb.append(",");
            sb.append("{\"timestamp\":").append(e.getTimeStamp())
              .append(",\"message\":\"").append(msg).append("\"}");
        }
        sb.append("]}");

        call("Logs_20140328.PutLogEvents", sb.toString());
    }

    private void createStream() {
        String body = "{\"logGroupName\":\"" + logGroupName
                + "\",\"logStreamName\":\"" + logStreamName + "\"}";
        // Ignore ResourceAlreadyExistsException (409) — stream already exists from a previous run
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/"))
                    .header("X-Amz-Target", "Logs_20140328.CreateLogStream")
                    .header("Content-Type", "application/json")
                    .header("Authorization",
                            "AWS4-HMAC-SHA256 Credential=test/20240101/us-east-1/logs/aws4_request, " +
                            "SignedHeaders=host;x-amz-date, Signature=test")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            client.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            addWarn("CloudWatchAppender createStream warning: " + e.getMessage());
        }
    }

    private void call(String target, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/"))
                    .header("X-Amz-Target", target)
                    .header("Content-Type", "application/json")
                    .header("Authorization",
                            "AWS4-HMAC-SHA256 Credential=test/20240101/us-east-1/logs/aws4_request, " +
                            "SignedHeaders=host;x-amz-date, Signature=test")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            client.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            addWarn("CloudWatchAppender error (" + target + "): " + e.getMessage());
        }
    }

    /** Escape characters that break JSON strings. */
    private String sanitize(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // ── Setters (required by Logback XML binding) ─────────────────────────────

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }
}
