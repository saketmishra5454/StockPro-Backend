package com.stockpro.report.feign;

import com.stockpro.report.dto.StockLevelDto;
import com.stockpro.report.dto.WarehouseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class WarehouseClientFallbackFactory implements FallbackFactory<WarehouseClient> {

    @Override
    public WarehouseClient create(Throwable cause) {
        return new WarehouseClient() {
            @Override
            public List<WarehouseDto> getAllWarehouses() {
                log.warn("warehouse-service unavailable while loading warehouses: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<StockLevelDto> getAllStockLevels() {
                log.warn("warehouse-service unavailable while loading stock levels: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<StockLevelDto> getStockByWarehouse(int warehouseId) {
                log.warn("warehouse-service unavailable for warehouse {} stock: {}", warehouseId, cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<StockLevelDto> getLowStockItems() {
                log.warn("warehouse-service unavailable while loading low-stock items: {}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
