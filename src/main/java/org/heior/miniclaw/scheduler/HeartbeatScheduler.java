package org.heior.miniclaw.scheduler;


import org.heior.miniclaw.memory.MemoryStoreService;
import org.heior.miniclaw.observer.ObserverService;
import org.heior.miniclaw.state.MiniClawState;
import org.heior.miniclaw.state.StateStoreService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class HeartbeatScheduler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);
    private final MemoryStoreService memoryStoreService;
    private final StateStoreService stateStoreService;
    private final ObserverService observerService;

    public HeartbeatScheduler(MemoryStoreService memoryStoreService,
                              StateStoreService stateStoreService,
                              ObserverService observerService) {
        this.memoryStoreService = memoryStoreService;
        this.stateStoreService = stateStoreService;
        this.observerService = observerService;
    }

    @Scheduled(fixedRateString = "${miniclaw.heart-interval-ms:1800000}")
    public void heartbeat() {
        try {
            Path todayLog = memoryStoreService.getTodayLogPath();
            long bytes = Files.exists(todayLog) ? Files.size(todayLog) : 0L;
//            boolean needsDistill = shouldDistill(bytes);
            var report = observerService.analyzeRecentLogs();
            int entryCount = memoryStoreService.readTodayLogEntries().size();
            if (entryCount == 0) {
                entryCount = memoryStoreService.readLatestNonEmptyLogEntries().size();
            }
            boolean needsDistill = observerService.shouldSuggestDistill(report, bytes, entryCount);
            MiniClawState state = stateStoreService.updateHeartbeat(bytes, needsDistill);
            appendHeartbeatRecord(bytes, needsDistill);

            log.info("Heartbeat done. bytes={}, needsDistill={}, lastHeartbeat={}",
                    bytes, needsDistill, state.getLastHeartbeat());
        } catch (Exception e) {
            log.error("Heartbeat 执行失败", e);
        }
    }

    private boolean shouldDistill(long bytes) {
        return bytes > 8_000;
    }
    private void appendHeartbeatRecord(long bytes, boolean needsDistill) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = "- [" + time + "] heartbeat | dailyLogBytes=" + bytes
                + " | needsDistill=" + needsDistill + System.lineSeparator();
        Path heartbeatFile = memoryStoreService.getHome().resolve("HEARTBEAT.md");

        try {
            if(Files.notExists(heartbeatFile)) {
                Files.writeString(heartbeatFile, "# Heartbeat" + System.lineSeparator());
            }
            Files.writeString(
                    heartbeatFile,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException( "Failed to write heartbeat record: " + e.getMessage(), e);

        }

    }

}
