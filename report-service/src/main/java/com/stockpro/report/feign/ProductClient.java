package com.stockpro.report.feign;

import com.stockpro.report.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {


    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable int id);


    @GetMapping("/api/products/all")
    List<ProductDto> getAllProducts();
}
