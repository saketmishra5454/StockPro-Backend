package com.stockpro.alert.feign;

import com.stockpro.alert.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        url = "${stockpro.clients.product-service.url:}",
        fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable int id);
}
