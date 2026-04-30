package com.stockpro.report.service.impl;

import com.stockpro.report.dto.ProductDto;
import com.stockpro.report.dto.ProductMovementSummary;
import com.stockpro.report.dto.StockLevelDto;
import com.stockpro.report.dto.StockMovementDto;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.feign.MovementClient;
import com.stockpro.report.feign.ProductClient;
import com.stockpro.report.feign.WarehouseClient;
import com.stockpro.report.repository.ReportRepository;
import com.stockpro.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


 // ReportServiceImpl — all analytics business logic.

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final WarehouseClient warehouseClient;
    private final ProductClient productClient;
    private final MovementClient movementClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ── Snapshot ────────────────────────────────────────────────────


     // Take a snapshot of all stock in one warehouse.

    @Override
    @Transactional
    public void takeSnapshot(int warehouseId) {
        log.info("Taking snapshot for warehouse {}", warehouseId);

        List<StockLevelDto> stockLevels;
        try {
            stockLevels = warehouseClient.getStockByWarehouse(warehouseId);
        } catch (Exception e) {
            log.error("Failed to fetch stock for warehouse {}: {}", warehouseId, e.getMessage());
            return;
        }

        LocalDate today = LocalDate.now();
        int saved = 0;
        int skipped = 0;

        for (StockLevelDto sl : stockLevels) {
            // Skip if already snapshotted today
            if (reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(
                    sl.getWarehouseId(), sl.getProductId(), today)) {
                skipped++;
                continue;
            }

            // Fetch costPrice from product-service
            double costPrice = 0.0;
            try {
                ProductDto product = productClient.getProductById(sl.getProductId());
                costPrice = product.getCostPrice();
            } catch (Exception e) {
                log.warn("Could not fetch product {} for snapshot: {}",
                        sl.getProductId(), e.getMessage());
                // Continue with costPrice = 0 so snapshot is still recorded
            }

            // Build and save snapshot
            InventorySnapshot snapshot = new InventorySnapshot();
            snapshot.setWarehouseId(sl.getWarehouseId());
            snapshot.setProductId(sl.getProductId());
            snapshot.setQuantity(sl.getQuantity());
            snapshot.setStockValue(sl.getQuantity() * costPrice);
            snapshot.setSnapshotDate(today);

            reportRepository.save(snapshot);
            saved++;
        }

        log.info("Snapshot complete for warehouse {}: {} saved, {} skipped (already done today)",
                warehouseId, saved, skipped);
    }


//      Take snapshots for ALL warehouses that have been previously snapshotted.
//      Called by SnapshotScheduler at midnight.

    @Override
    public void takeSnapshotAllWarehouses() {
        log.info("Taking daily snapshot for all warehouses...");

        List<Integer> warehouseIds = reportRepository.findDistinctWarehouseIds();

        // If no snapshots exist yet, we can't discover warehouses from our DB
        // In production, you'd call warehouse-service to get all warehouse IDs
        if (warehouseIds.isEmpty()) {
            log.warn("No warehouse IDs found in snapshot history. " +
                    "Trigger manual snapshot first via POST /api/reports/snapshot/{warehouseId}");
            return;
        }

        for (int warehouseId : warehouseIds) {
            try {
                takeSnapshot(warehouseId);
            } catch (Exception e) {
                log.error("Failed snapshot for warehouse {}: {}", warehouseId, e.getMessage());
            }
        }
    }

    @Override
    public List<InventorySnapshot> getLatestSnapshot(int warehouseId) {
        return reportRepository.findByWarehouseId(warehouseId).stream()
                .filter(s -> s.getSnapshotDate().equals(LocalDate.now()))
                .collect(Collectors.toList());
    }

    // ── Valuation ────────────────────────────────────────────────────


//      Total stock value across all warehouses.
//      Reads from our snapshot DB — uses latest snapshot date.

    @Override
    public double getTotalStockValue() {
        Double total = reportRepository.sumTotalStockValue();
        return total != null ? total : 0.0;
    }

    /**
     * Stock value for one warehouse from latest snapshot.
     */
    @Override
    public double getStockValueByWarehouse(int warehouseId) {
        Double value = reportRepository.sumStockValueByWarehouse(warehouseId);
        return value != null ? value : 0.0;
    }

    // ── Turnover ──────────────────────────────────────────────────────


     // Inventory Turnover Rate = COGS / Average Inventory Value

    @Override
    public double getInventoryTurnover(int warehouseId, LocalDate from, LocalDate to) {
        // Step 1: Calculate COGS from movement-service
        double cogs = 0.0;
        try {
            String fromStr = from.atStartOfDay().format(DT_FORMATTER);
            String toStr = to.atTime(23, 59, 59).format(DT_FORMATTER);

            List<StockMovementDto> movements =
                    movementClient.getMovementsByDateRange(fromStr, toStr);

            // COGS = sum of STOCK_OUT value only (not transfers or adjustments)
            cogs = movements.stream()
                    .filter(m -> "STOCK_OUT".equals(m.getMovementType()))
                    .mapToDouble(m -> m.getQuantity() * m.getUnitCost())
                    .sum();

        } catch (Exception e) {
            log.warn("Could not fetch movements for turnover calculation: {}", e.getMessage());
        }

        // Step 2: Get average inventory value from snapshot DB
        Double avgInventory = reportRepository
                .avgStockValueByWarehouseAndDateRange(warehouseId, from, to);

        if (avgInventory == null || avgInventory == 0.0) {
            log.warn("No snapshot data for warehouse {} between {} and {}", warehouseId, from, to);
            return 0.0;
        }

        double turnover = cogs / avgInventory;
        log.info("Inventory turnover for warehouse {}: COGS={}, AvgInventory={}, Turnover={}",
                warehouseId, cogs, avgInventory, turnover);

        return Math.round(turnover * 100.0) / 100.0;
    }

    // ── Product Movement Reports ─────────────────────────────────────

//      Low stock report — products currently below reorder level.
//      Calls warehouse-service for current low stock items.

    @Override
    public List<Map<String, Object>> getLowStockReport() {
        List<Map<String, Object>> report = new ArrayList<>();

        try {
            List<StockLevelDto> lowStockItems = warehouseClient.getLowStockItems();

            for (StockLevelDto item : lowStockItems) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("warehouseId", item.getWarehouseId());
                row.put("productId", item.getProductId());
                row.put("currentQuantity", item.getQuantity());
                row.put("reservedQuantity", item.getReservedQuantity());
                row.put("availableQuantity", item.getQuantity() - item.getReservedQuantity());

                // Enrich with product name if product-service is available
                try {
                    ProductDto product = productClient.getProductById(item.getProductId());
                    row.put("productName", product.getName());
                    row.put("sku", product.getSku());
                    row.put("reorderLevel", product.getReorderLevel());
                    row.put("unitCost", product.getCostPrice());
                } catch (Exception e) {
                    row.put("productName", "Product-" + item.getProductId());
                    log.warn("Could not enrich product {}", item.getProductId());
                }

                report.add(row);
            }
        } catch (Exception e) {
            log.error("Failed to fetch low stock report: {}", e.getMessage());
        }

        return report;
    }


     // Top moving products — ranked by total units moved (in + out combined).

    @Override
    public List<ProductMovementSummary> getTopMovingProducts(int limit) {
        return buildMovementRanking(limit, false);
    }


//      Slow moving products — minimal movement in last 30 days.
//      Same calculation as top movers but sorted ascending (least movement first).

    @Override
    public List<ProductMovementSummary> getSlowMovingProducts(int limit) {
        return buildMovementRanking(limit, true);
    }


     // Dead stock — products with no quantity change in snapshot history for 90+ days.

    @Override
    public List<InventorySnapshot> getDeadStock() {
        LocalDate cutoff = LocalDate.now().minusDays(90);
        return reportRepository.findSlowMovingStock(cutoff);
    }


     // PO spend summary for a date range.

    @Override
    public Map<String, Object> getPOSummary(LocalDate from, LocalDate to) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("periodFrom", from.toString());
        summary.put("periodTo", to.toString());

        try {
            String fromStr = from.atStartOfDay().format(DT_FORMATTER);
            String toStr = to.atTime(23, 59, 59).format(DT_FORMATTER);

            List<StockMovementDto> movements =
                    movementClient.getMovementsByDateRange(fromStr, toStr);

            // Filter only STOCK_IN (these are goods received from POs)
            List<StockMovementDto> stockInMovements = movements.stream()
                    .filter(m -> "STOCK_IN".equals(m.getMovementType()))
                    .collect(Collectors.toList());

            // Total spend = sum of (quantity × unitCost)
            double totalSpend = stockInMovements.stream()
                    .mapToDouble(m -> m.getQuantity() * m.getUnitCost())
                    .sum();

            // Group by warehouse
            Map<Integer, Double> spendByWarehouse = stockInMovements.stream()
                    .collect(Collectors.groupingBy(
                            StockMovementDto::getWarehouseId,
                            Collectors.summingDouble(m -> m.getQuantity() * m.getUnitCost())));

            // Group by product
            Map<Integer, Integer> unitsByProduct = stockInMovements.stream()
                    .collect(Collectors.groupingBy(
                            StockMovementDto::getProductId,
                            Collectors.summingInt(StockMovementDto::getQuantity)));

            summary.put("totalStockInMovements", stockInMovements.size());
            summary.put("totalSpend", Math.round(totalSpend * 100.0) / 100.0);
            summary.put("spendByWarehouse", spendByWarehouse);
            summary.put("unitsByProduct", unitsByProduct);

        } catch (Exception e) {
            log.error("Failed to generate PO summary: {}", e.getMessage());
            summary.put("error", "Could not fetch movement data: " + e.getMessage());
        }

        return summary;
    }

    // ── Private helpers ───────────────────────────────────────────────


     // Builds movement ranking for top or slow movers.

    private List<ProductMovementSummary> buildMovementRanking(int limit, boolean ascending) {
        List<ProductMovementSummary> summaries = new ArrayList<>();

        try {
            List<ProductDto> products = productClient.getAllProducts();

            // Calculate movement velocity for each active product
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            String fromStr = thirtyDaysAgo.atStartOfDay().format(DT_FORMATTER);
            String toStr = LocalDateTime.now().format(DT_FORMATTER);

            List<StockMovementDto> recentMovements;
            try {
                recentMovements = movementClient.getMovementsByDateRange(fromStr, toStr);
            } catch (Exception e) {
                log.warn("Could not fetch movements for ranking: {}", e.getMessage());
                return summaries;
            }

            for (ProductDto product : products) {
                if (!product.isActive()) continue;

                // Get movements for this product in last 30 days
                List<StockMovementDto> productMovements = recentMovements.stream()
                        .filter(m -> m.getProductId() == product.getProductId())
                        .collect(Collectors.toList());

                int totalIn = productMovements.stream()
                        .filter(m -> "STOCK_IN".equals(m.getMovementType()))
                        .mapToInt(StockMovementDto::getQuantity)
                        .sum();

                int totalOut = productMovements.stream()
                        .filter(m -> "STOCK_OUT".equals(m.getMovementType()))
                        .mapToInt(StockMovementDto::getQuantity)
                        .sum();

                double totalValue = productMovements.stream()
                        .mapToDouble(m -> m.getQuantity() * m.getUnitCost())
                        .sum();

                summaries.add(new ProductMovementSummary(
                        product.getProductId(),
                        product.getName(),
                        product.getSku(),
                        totalIn,
                        totalOut,
                        totalIn + totalOut,
                        totalValue));
            }

            // Sort and limit
            Comparator<ProductMovementSummary> comparator =
                    Comparator.comparingInt(ProductMovementSummary::getTotalUnitsMoved);
            if (!ascending) comparator = comparator.reversed();

            return summaries.stream()
                    .sorted(comparator)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to build movement ranking: {}", e.getMessage());
            return summaries;
        }
    }
}