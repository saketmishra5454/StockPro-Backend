package com.stockpro.product.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(int productId) {
        super("Product not found with ID: " + productId);
    }
}
