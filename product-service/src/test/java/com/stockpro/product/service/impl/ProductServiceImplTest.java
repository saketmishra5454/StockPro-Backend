package com.stockpro.product.service.impl;

import com.stockpro.product.dto.StockLevelDto;
import com.stockpro.product.entity.Product;
import com.stockpro.product.feign.WarehouseClient;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setProductId(1);
        sampleProduct.setSku("BOLT-M6-SS");
        sampleProduct.setName("M6 Stainless Steel Bolt");
        sampleProduct.setCategory("Fasteners");
        sampleProduct.setBrand("FastenTech");
        sampleProduct.setCostPrice(2.50);
        sampleProduct.setSellingPrice(4.00);
        sampleProduct.setReorderLevel(100);
        sampleProduct.setMaxStockLevel(1000);
        sampleProduct.setActive(true);
        sampleProduct.setBarcode("5901234123457");
    }

    @Test
    @DisplayName("createProduct — should save and return product")
    void createProduct_newSku_savesProduct() {
        when(productRepository.findBySku("BOLT-M6-SS")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        Product result = productService.createProduct(sampleProduct);

        assertThat(result.getSku()).isEqualTo("BOLT-M6-SS");
        assertThat(result.getCostPrice()).isEqualTo(2.50);
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("createProduct — should throw exception for duplicate SKU")
    void createProduct_duplicateSku_throwsException() {
        when(productRepository.findBySku("BOLT-M6-SS"))
                .thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.createProduct(sampleProduct))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById — should return product for valid ID")
    void getById_validId_returnsProduct() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        Product result = productService.getById(1);

        assertThat(result.getName()).isEqualTo("M6 Stainless Steel Bolt");
    }

    @Test
    @DisplayName("getById — should throw exception for invalid ID")
    void getById_invalidId_throwsException() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getBySku — should return product for valid SKU")
    void getBySku_validSku_returnsProduct() {
        when(productRepository.findBySku("BOLT-M6-SS"))
                .thenReturn(Optional.of(sampleProduct));

        Optional<Product> result = productService.getBySku("BOLT-M6-SS");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("M6 Stainless Steel Bolt");
    }

    @Test
    @DisplayName("searchProducts — should return matching products")
    void searchProducts_nameMatch_returnsResults() {
        when(productRepository.searchCatalog("bolt"))
                .thenReturn(List.of(sampleProduct));

        List<Product> results = productService.searchProducts("bolt");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSku()).isEqualTo("BOLT-M6-SS");
    }

    @Test
    @DisplayName("searchProducts — should return empty for no matches")
    void searchProducts_noMatch_returnsEmpty() {
        when(productRepository.searchCatalog("xyz"))
                .thenReturn(List.of());

        List<Product> results = productService.searchProducts("xyz");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("deactivateProduct — should set isActive to false")
    void deactivateProduct_activeProduct_setsInactive() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any())).thenReturn(sampleProduct);

        productService.deactivateProduct(1);

        assertThat(sampleProduct.isActive()).isFalse();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("activateProduct - should set isActive to true")
    void activateProduct_inactiveProduct_setsActive() {
        sampleProduct.setActive(false);
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any())).thenReturn(sampleProduct);

        productService.activateProduct(1);

        assertThat(sampleProduct.isActive()).isTrue();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("updateProduct — should update only provided fields")
    void updateProduct_partialUpdate_onlyChangesProvidedFields() {
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product updates = new Product();
        updates.setName("M6 Bolt Updated");
        updates.setCostPrice(3.00);

        Product result = productService.updateProduct(1, updates);

        assertThat(result.getName()).isEqualTo("M6 Bolt Updated");
        assertThat(result.getCostPrice()).isEqualTo(3.00);
        assertThat(result.getSku()).isEqualTo("BOLT-M6-SS");
        assertThat(result.getCategory()).isEqualTo("Fasteners");
    }

    @Test
    @DisplayName("getLowStockProducts — should return products with low stock from warehouse")
    void getLowStockProducts_warehouseReportsLowStock_returnsProducts() {
        StockLevelDto lowStockItem = new StockLevelDto();
        lowStockItem.setProductId(1L);
        lowStockItem.setWarehouseId(1L);
        lowStockItem.setQuantity(5);
        lowStockItem.setReorderThreshold(100);

        when(warehouseClient.getLowStockItems()).thenReturn(List.of(lowStockItem));
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

        List<Product> result = productService.getLowStockProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("BOLT-M6-SS");
    }

    @Test
    @DisplayName("getLowStockProducts — should return empty list if warehouse-service is down")
    void getLowStockProducts_warehouseDown_returnsEmptyList() {
        when(warehouseClient.getLowStockItems())
                .thenThrow(new RuntimeException("Connection refused"));

        List<Product> result = productService.getLowStockProducts();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getByBarcode — should return product for valid barcode")
    void getByBarcode_validBarcode_returnsProduct() {
        when(productRepository.findByBarcode("5901234123457"))
                .thenReturn(Optional.of(sampleProduct));

        Optional<Product> result = productService.getByBarcode("5901234123457");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("M6 Stainless Steel Bolt");
    }
}
