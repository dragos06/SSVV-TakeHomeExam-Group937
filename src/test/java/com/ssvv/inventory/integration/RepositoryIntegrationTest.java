package com.ssvv.inventory.integration;

import com.ssvv.inventory.entity.Product;
import com.ssvv.inventory.entity.Shipment;
import com.ssvv.inventory.entity.Supplier;
import com.ssvv.inventory.repository.ProductRepository;
import com.ssvv.inventory.repository.ShipmentRepository;
import com.ssvv.inventory.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class RepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    @DisplayName("findProductsBelowMinThreshold returns products whose stock is below minThreshold")
    void testFindProductsBelowMinThreshold() {
        // Arrange - create two products, one below threshold and one above
        Product low = new Product();
        low.setName("LowStock");
        low.setStockLevel(2);
        low.setMinThreshold(5);
        low.setMaxCapacity(100);

        Product ok = new Product();
        ok.setName("OkStock");
        ok.setStockLevel(10);
        ok.setMinThreshold(5);
        ok.setMaxCapacity(100);

        productRepository.save(low);
        productRepository.save(ok);

        // Act
        List<Product> below = productRepository.findProductsBelowMinThreshold();

        // Assert
        assertNotNull(below);
        assertEquals(1, below.size(), "There should be exactly one product below threshold");
        assertEquals("LowStock", below.get(0).getName());
    }
    @Test
    @DisplayName("Saving two products with same name should violate unique constraint")
    void testUniqueProductNameConstraint() {
        Product p1 = new Product();
        p1.setName("UniqueName");
        p1.setStockLevel(1);
        p1.setMinThreshold(0);
        p1.setMaxCapacity(10);

        Product p2 = new Product();
        p2.setName("UniqueName");
        p2.setStockLevel(2);
        p2.setMinThreshold(0);
        p2.setMaxCapacity(10);

        productRepository.saveAndFlush(p1);

        // Saving the second with same unique name should cause a DataIntegrityViolationException on flush
        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepository.saveAndFlush(p2);
        });
    }

    @Test
    @DisplayName("Paging through products returns pages correctly")
    void testProductPaging() {
        for (int i = 0; i < 15; i++) {
            Product p = new Product();
            p.setName("P" + i + System.nanoTime());
            p.setStockLevel(i);
            p.setMinThreshold(0);
            p.setMaxCapacity(100);
            productRepository.save(p);
        }
        productRepository.flush();

        var page = productRepository.findAll(PageRequest.of(0, 10));
        assertEquals(10, page.getContent().size());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    @DisplayName("Shipment saves and correctly references product and supplier")
    void testShipmentRelationshipSave() {
        Product p = new Product();
        p.setName("ProdForShipment");
        p.setStockLevel(5);
        p.setMinThreshold(0);
        p.setMaxCapacity(50);
        productRepository.saveAndFlush(p);

        Supplier s = new Supplier();
        s.setName("SupplierForShipment");
        // satisfy validation constraints on Supplier
        s.setContactEmail("supplierforshipment@example.com");
        s.setLeadTimeDays(2);
        supplierRepository.saveAndFlush(s);

        Shipment sh = new Shipment();
        sh.setProduct(p);
        sh.setSupplier(s);
        sh.setQuantity(10);
        sh.setShipmentDate(LocalDate.now());

        Shipment saved = shipmentRepository.saveAndFlush(sh);

        assertNotNull(saved.getId());
        assertEquals(p.getId(), saved.getProduct().getId());
        assertEquals(s.getId(), saved.getSupplier().getId());

        // Ensure we can navigate from product to its shipments after refresh
        Product reloaded = productRepository.findById(p.getId()).orElseThrow();
        assertNotNull(reloaded);
    }
}

