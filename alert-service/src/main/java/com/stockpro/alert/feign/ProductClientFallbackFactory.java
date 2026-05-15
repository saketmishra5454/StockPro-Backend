package com.stockpro.alert.feign;

import com.stockpro.alert.dto.ProductDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductDto getProductById(int id) {
                if (cause instanceof FeignException.NotFound) {
                    log.warn("Product {} does not exist in product-service. Stock-health check will skip it.", id);
                } else if (cause instanceof FeignException feignException) {
                    log.warn("Product-service returned HTTP {} for product {}. Body: {}",
                            feignException.status(), id, feignException.contentUTF8());
                } else {
                    log.warn("Product-service lookup failed for product {}: {}", id, cause.getMessage());
                }
                return null;
            }
        };
    }
}
