package com.stockpro.report.feign;

import com.stockpro.report.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductDto getProductById(int id) {
                log.warn("product-service unavailable for product {}: {}", id, cause.getMessage());
                return null;
            }

            @Override
            public List<ProductDto> getAllProducts() {
                log.warn("product-service unavailable while loading products: {}", cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
