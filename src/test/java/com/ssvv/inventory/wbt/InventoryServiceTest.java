package com.ssvv.inventory.wbt;

import com.ssvv.inventory.entity.Product;
import com.ssvv.inventory.entity.Shipment;
import com.ssvv.inventory.entity.Supplier;
import com.ssvv.inventory.repository.ProductRepository;
import com.ssvv.inventory.repository.ShipmentRepository;
import com.ssvv.inventory.repository.SupplierRepository;
import com.ssvv.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepo;

    @Mock
    private SupplierRepository supplierRepo;

    @Mock
    private ShipmentRepository shipmentRepo;

    @InjectMocks
    private InventoryService inventoryService;

    private Product mockProduct;
    private Supplier mockSupplier;

    @BeforeEach
    void setUp() {
        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");
        mockProduct.setStockLevel(50);
        mockProduct.setMaxCapacity(200);

        mockSupplier = new Supplier();
        mockSupplier.setId(1L);
        mockSupplier.setName("Test Supplier");
    }

    /**
     * Path 1: quantity <= 0
     * Covers: Nodes 1, 2, 3, 11
     */
    @Test
    void testProcessRestock_Path1_InvalidQuantity() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.processRestock(1L, 1L, 0);
        });

        assertEquals("Cannot order a zero or negative quantity.", exception.getMessage());
        verifyNoInteractions(productRepo, supplierRepo, shipmentRepo);
    }

    /**
     * Path 2: Product not found
     * Covers: Nodes 1, 2, 4, 5, 11
     */
    @Test
    void testProcessRestock_Path2_ProductNotFound() {
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.processRestock(999L, 1L, 10);
        });

        assertEquals("Product not found", exception.getMessage());
        verify(productRepo, times(1)).findById(999L);
        verifyNoInteractions(supplierRepo, shipmentRepo);
    }

    /**
     * Path 3: Supplier not found
     * Covers: Nodes 1, 2, 4, 6, 7, 11
     */
    @Test
    void testProcessRestock_Path3_SupplierNotFound() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(supplierRepo.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.processRestock(1L, 999L, 10);
        });

        assertEquals("Supplier not found", exception.getMessage());
        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(999L);
        verifyNoInteractions(shipmentRepo);
    }

    /**
     * Path 4: Capacity Overload (stockLevel + quantity > maxCapacity)
     * Covers: Nodes 1, 2, 4, 6, 8, 9, 11
     */
    @Test
    void testProcessRestock_Path4_CapacityExceeded() {
        // 50 current stock + 160 restock = 210 (Max capacity is 200)
        int excessiveQuantity = 160;

        when(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(mockSupplier));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.processRestock(1L, 1L, excessiveQuantity);
        });

        assertEquals("Restock quantity exceeds maximum warehouse capacity for this product.", exception.getMessage());
        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(1L);
        verify(productRepo, never()).save(any(Product.class));
        verifyNoInteractions(shipmentRepo);
    }

    /**
     * Path 5: Successful execution execution path
     * Covers: Nodes 1, 2, 4, 6, 8, 10, 11
     */
    @Test
    void testProcessRestock_Path5_Success() {
        int validQuantity = 20;

        when(productRepo.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(supplierRepo.findById(1L)).thenReturn(Optional.of(mockSupplier));

        // Mock save operations to echo back items safely
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepo.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = inventoryService.processRestock(1L, 1L, validQuantity);

        assertNotNull(result);
        assertEquals(70, mockProduct.getStockLevel()); // 50 + 20
        assertEquals(validQuantity, result.getQuantity());
        assertEquals(mockProduct, result.getProduct());
        assertEquals(mockSupplier, result.getSupplier());

        verify(productRepo, times(1)).findById(1L);
        verify(supplierRepo, times(1)).findById(1L);
        verify(productRepo, times(1)).save(mockProduct);
        verify(shipmentRepo, times(1)).save(any(Shipment.class));
    }
}
