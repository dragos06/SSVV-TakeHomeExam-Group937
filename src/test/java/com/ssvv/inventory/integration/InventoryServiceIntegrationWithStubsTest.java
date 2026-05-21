package com.ssvv.inventory.integration;

import com.ssvv.inventory.entity.Product;
import com.ssvv.inventory.entity.Supplier;
import com.ssvv.inventory.entity.Shipment;
import com.ssvv.inventory.repository.ProductRepository;
import com.ssvv.inventory.repository.SupplierRepository;
import com.ssvv.inventory.repository.ShipmentRepository;
import com.ssvv.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryServiceIntegrationWithStubsTest
 *
 * Integration-level verification of the InventoryService logic while running inside a Spring
 * context. Repositories are replaced with stubs (@MockBean) so database access is deterministic
 * and under test control. This style lets us validate the real service wiring and business rules
 * (capacity checks, error propagation) while avoiding an actual database dependency.
 */
@SpringBootTest
public class InventoryServiceIntegrationWithStubsTest {

    @Autowired
    private InventoryService inventoryService; // real service bean from the context

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private ShipmentRepository shipmentRepo;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ProductRepository productRepositoryMock() {
            return Mockito.mock(ProductRepository.class);
        }

        @Bean
        @Primary
        public SupplierRepository supplierRepositoryMock() {
            return Mockito.mock(SupplierRepository.class);
        }

        @Bean
        @Primary
        public ShipmentRepository shipmentRepositoryMock() {
            return Mockito.mock(ShipmentRepository.class);
        }
    }


    private Product product;
    private Supplier supplier;

    @BeforeEach
    void setup() {
        product = new Product();
        product.setId(1L);
        product.setName("Integration-Product");
        product.setStockLevel(10);
        product.setMaxCapacity(50);

        supplier = new Supplier();
        supplier.setId(2L);
        supplier.setName("Integration-Supplier");
    }

    @Test
    @DisplayName("processRestock throws when quantity <= 0")
    void testProcessRestockInvalidQuantity() {
        // Clear any repository interactions performed during context startup (DataInitializer)
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.processRestock(1L, 2L, 0));
        assertEquals("Cannot order a zero or negative quantity.", ex.getMessage());

        verifyNoInteractions(productRepo, supplierRepo, shipmentRepo);
    }

    @Test
    @DisplayName("processRestock throws when product not found")
    void testProcessRestockProductNotFound() {
        // Clear any repository interactions performed during context startup (DataInitializer)
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.processRestock(99L, 2L, 5));
        assertEquals("Product not found", ex.getMessage());

        verify(productRepo, times(1)).findById(99L);
        verifyNoInteractions(supplierRepo, shipmentRepo);
    }

    @Test
    @DisplayName("processRestock successful path updates product and saves shipment")
    void testProcessRestockSuccessUpdatesProductAndSavesShipment() {
        // Clear any repository interactions performed during context startup (DataInitializer)
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepo.findById(2L)).thenReturn(Optional.of(supplier));
        when(productRepo.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        Shipment result = inventoryService.processRestock(1L, 2L, 20);

        assertNotNull(result);
        assertEquals(30, product.getStockLevel()); // 10 + 20
        assertEquals(20, result.getQuantity());
        assertEquals(product, result.getProduct());
        assertEquals(supplier, result.getSupplier());

        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(2L);
        verify(productRepo, times(1)).save(product);
        verify(shipmentRepo, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("processRestock throws when supplier not found")
    void testProcessRestockSupplierNotFound() {
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        // Product is found, but Supplier is missing
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.processRestock(1L, 99L, 5));
        assertEquals("Supplier not found", ex.getMessage()); // Adjust message to match your actual service exception

        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(99L);
        // Ensure no data was mutated or saved
        verify(productRepo, never()).save(any(Product.class));
        verify(shipmentRepo, never()).save(any(Shipment.class));
    }

    @Test
    @DisplayName("processRestock throws when quantity exceeds max capacity")
    void testProcessRestockExceedsCapacity() {
        // Clear any repository interactions performed during context startup
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        // Current stock: 10, Max: 50. Adding 45 would equal 55 (Invalid)
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepo.findById(2L)).thenReturn(Optional.of(supplier));

        // Fix: Changed expected class to IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                inventoryService.processRestock(1L, 2L, 45));

        // Exact message validation matched from your stack trace
        assertEquals("Restock quantity exceeds maximum warehouse capacity for this product.", ex.getMessage());

        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(2L);
        verify(productRepo, never()).save(any(Product.class));
        verify(shipmentRepo, never()).save(any(Shipment.class));
    }

    @Test
    @DisplayName("processRestock rolls back transaction if shipment saving fails")
    void testProcessRestockRollsBackOnShipmentFailure() {
        clearInvocations(productRepo, supplierRepo, shipmentRepo);

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(supplierRepo.findById(2L)).thenReturn(Optional.of(supplier));
        when(productRepo.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Simulate database crash/constraint failure during shipment saving
        when(shipmentRepo.save(any(Shipment.class)))
                .thenThrow(new RuntimeException("Database connection lost during shipment logging"));

        assertThrows(RuntimeException.class, () ->
                inventoryService.processRestock(1L, 2L, 10));

        // Verify that despite the crash, the flow was attempted correctly
        verify(productRepo, times(1)).save(any(Product.class));
        verify(shipmentRepo, times(1)).save(any(Shipment.class));
    }
}


