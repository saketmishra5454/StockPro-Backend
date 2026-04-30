package com.stockpro.report.scheduler;

import com.stockpro.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


 // SnapshotScheduler — takes daily inventory snapshots at midnight.


@RequiredArgsConstructor
@Slf4j
public class SnapshotScheduler {

    private final ReportService reportService;


//     * Runs at 00:00:00 every day.
//     * Takes a snapshot of all warehouses for trend analysis.

    @Scheduled(cron = "0 0 0 * * *")
    public void takeDailySnapshot() {
        log.info("=== Daily inventory snapshot started at midnight ===");
        try {
            reportService.takeSnapshotAllWarehouses();
            log.info("=== Daily inventory snapshot completed ===");
        } catch (Exception e) {
            log.error("Daily snapshot failed: {}", e.getMessage());
        }
    }
}