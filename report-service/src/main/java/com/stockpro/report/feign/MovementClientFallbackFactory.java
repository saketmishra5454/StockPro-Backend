package com.stockpro.report.feign;

import com.stockpro.report.dto.StockMovementDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MovementClientFallbackFactory implements FallbackFactory<MovementClient> {

    @Override
    public MovementClient create(Throwable cause) {
        return new MovementClient() {
            @Override
            public List<StockMovementDto> getAllMovements() {
                log.warn("movement-service unavailable while loading all movements: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<StockMovementDto> getMovementsByProduct(int productId) {
                log.warn("movement-service unavailable for product {} movements: {}", productId, cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<StockMovementDto> getMovementsByDateRange(String from, String to) {
                log.warn("movement-service unavailable for date range {} to {}: {}", from, to, cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> getStockOut(int productId) {
                log.warn("movement-service unavailable for product {} stock-out summary: {}", productId, cause.getMessage());
                return Collections.emptyMap();
            }
        };
    }
}
