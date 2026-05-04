package com.stockpro.supplier.service.impl;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void createSupplierSavesWhenEmailAndTaxIdAreUnique() {
        Supplier supplier = supplier("FastTech Supplies");

        when(supplierRepository.existsByEmail("orders@fasttech.test")).thenReturn(false);
        when(supplierRepository.existsByTaxId("GST-101")).thenReturn(false);
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier saved = supplierService.createSupplier(supplier);

        assertThat(saved).isSameAs(supplier);
        verify(supplierRepository).save(supplier);
    }

    @Test
    void createSupplierRejectsDuplicateEmail() {
        Supplier supplier = supplier("FastTech Supplies");

        when(supplierRepository.existsByEmail("orders@fasttech.test")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(supplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("orders@fasttech.test");
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void createSupplierRejectsDuplicateTaxId() {
        Supplier supplier = supplier("FastTech Supplies");

        when(supplierRepository.existsByEmail("orders@fasttech.test")).thenReturn(false);
        when(supplierRepository.existsByTaxId("GST-101")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(supplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GST-101");
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void getByIdThrowsWhenSupplierDoesNotExist() {
        when(supplierRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateSupplierOnlyAppliesProvidedAndPositiveFields() {
        Supplier existing = supplier("FastTech Supplies");
        existing.setSupplierId(7);
        existing.setCity("Mumbai");
        existing.setCountry("India");
        existing.setLeadTimeDays(5);

        Supplier update = new Supplier();
        update.setName("FastTech Global");
        update.setEmail("orders@fasttech.test");
        update.setCity("Pune");
        update.setLeadTimeDays(0);

        when(supplierRepository.findById(7)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);

        Supplier updated = supplierService.updateSupplier(7, update);

        assertThat(updated.getName()).isEqualTo("FastTech Global");
        assertThat(updated.getEmail()).isEqualTo("orders@fasttech.test");
        assertThat(updated.getCity()).isEqualTo("Pune");
        assertThat(updated.getCountry()).isEqualTo("India");
        assertThat(updated.getLeadTimeDays()).isEqualTo(5);
        verify(supplierRepository, never()).existsByEmail("orders@fasttech.test");
        verify(supplierRepository).save(existing);
    }

    @Test
    void updateSupplierRejectsEmailUsedByAnotherSupplier() {
        Supplier existing = supplier("FastTech Supplies");
        existing.setSupplierId(7);

        Supplier update = new Supplier();
        update.setEmail("new@fasttech.test");

        when(supplierRepository.findById(7)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByEmail("new@fasttech.test")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.updateSupplier(7, update))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("new@fasttech.test");
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void deactivateSupplierMarksSupplierInactive() {
        Supplier supplier = supplier("FastTech Supplies");
        supplier.setSupplierId(7);

        when(supplierRepository.findById(7)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        supplierService.deactivateSupplier(7);

        assertThat(supplier.isActive()).isFalse();
        verify(supplierRepository).save(supplier);
    }

    @Test
    void deleteSupplierDeletesWhenSupplierExists() {
        when(supplierRepository.existsById(7)).thenReturn(true);

        supplierService.deleteSupplier(7);

        verify(supplierRepository).deleteById(7);
    }

    @Test
    void updateRatingCalculatesWeightedAverageAndIncrementsCount() {
        Supplier supplier = supplier("FastTech Supplies");
        supplier.setSupplierId(7);
        supplier.setRating(4.0);
        supplier.setRatingCount(2);

        when(supplierRepository.findById(7)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(supplier)).thenReturn(supplier);

        Supplier updated = supplierService.updateRating(7, 5.0);

        assertThat(updated.getRating()).isEqualTo(4.33);
        assertThat(updated.getRatingCount()).isEqualTo(3);
        verify(supplierRepository).save(supplier);
    }

    @Test
    void updateRatingRejectsScoreOutsideAllowedRange() {
        assertThatThrownBy(() -> supplierService.updateRating(7, 5.5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("between 1.0 and 5.0");
        verify(supplierRepository, never()).findById(7);
    }

    @Test
    void getActiveSuppliersDelegatesToRepository() {
        Supplier supplier = supplier("FastTech Supplies");

        when(supplierRepository.findByIsActive(true)).thenReturn(List.of(supplier));

        assertThat(supplierService.getActiveSuppliers()).containsExactly(supplier);
    }

    private Supplier supplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setContactPerson("Priya Sharma");
        supplier.setEmail("orders@fasttech.test");
        supplier.setPhone("9999999999");
        supplier.setAddress("Industrial Estate");
        supplier.setCity("Mumbai");
        supplier.setCountry("India");
        supplier.setTaxId("GST-101");
        supplier.setPaymentTerms("NET-30");
        supplier.setLeadTimeDays(5);
        supplier.setActive(true);
        return supplier;
    }
}
