package com.stockpro.report.feign;

import com.stockpro.report.dto.StockMovementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "movement-service")
public interface MovementClient {


    @GetMapping("/api/movements/all")
    List<StockMovementDto> getAllMovements();


    @GetMapping("/api/movements/product/{productId}")
    List<StockMovementDto> getMovementsByProduct(@PathVariable int productId);


    @GetMapping("/api/movements/date-range")
    List<StockMovementDto> getMovementsByDateRange(
            @RequestParam String from,
            @RequestParam String to);


    @GetMapping("/api/movements/stock-out/{productId}")
    java.util.Map<String, Object> getStockOut(@PathVariable int productId);
}
