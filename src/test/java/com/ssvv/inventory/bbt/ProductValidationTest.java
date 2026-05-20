package com.ssvv.inventory.bbt;

import com.ssvv.inventory.entity.Product;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Product createValidProduct() {
        Product product = new Product();
        product.setName("Valid Product");
        product.setStockLevel(10);
        product.setMinThreshold(5);
        product.setMaxCapacity(100);
        return product;
    }

    // --- Name Validation Tests (REQ-05) ---

    @Test
    void testName_Valid() {
        Product product = createValidProduct();
        product.setName("Laptop Monitors");

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty(), "Valid product name should not produce violations");
    }

    @Test
    void testName_Null() {
        Product product = createValidProduct();
        product.setName(null);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Product name cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testName_Empty() {
        Product product = createValidProduct();
        product.setName("");

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Product name cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testName_Whitespace() {
        Product product = createValidProduct();
        product.setName("    ");

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Product name cannot be blank", violations.iterator().next().getMessage());
    }

    // --- StockLevel Validation Tests (REQ-06) ---

    @Test
    void testStockLevel_ValidPositive() {
        Product product = createValidProduct();
        product.setStockLevel(50);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testStockLevel_ZeroBoundary() {
        Product product = createValidProduct();
        product.setStockLevel(0);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testStockLevel_NegativeBoundary() {
        Product product = createValidProduct();
        product.setStockLevel(-1);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Stock cannot be negative", violations.iterator().next().getMessage());
    }

    @Test
    void testStockLevel_Null() {
        Product product = createValidProduct();
        product.setStockLevel(null);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Stock level is required", violations.iterator().next().getMessage());
    }

    // --- MinThreshold Validation Tests (REQ-07) ---

    @Test
    void testMinThreshold_ValidPositive() {
        Product product = createValidProduct();
        product.setMinThreshold(20);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMinThreshold_ZeroBoundary() {
        Product product = createValidProduct();
        product.setMinThreshold(0);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMinThreshold_NegativeBoundary() {
        Product product = createValidProduct();
        product.setMinThreshold(-1);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Threshold cannot be negative", violations.iterator().next().getMessage());
    }

    @Test
    void testMinThreshold_Null() {
        Product product = createValidProduct();
        product.setMinThreshold(null);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Minimum threshold is required", violations.iterator().next().getMessage());
    }

    // --- MaxCapacity Validation Tests (REQ-08) ---

    @Test
    void testMaxCapacity_ValidPositive() {
        Product product = createValidProduct();
        product.setMaxCapacity(200);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMaxCapacity_OneBoundary() {
        Product product = createValidProduct();
        product.setMaxCapacity(1);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMaxCapacity_ZeroBoundary() {
        Product product = createValidProduct();
        product.setMaxCapacity(0);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Max capacity must be at least 1", violations.iterator().next().getMessage());
    }

    @Test
    void testMaxCapacity_NegativeBoundary() {
        Product product = createValidProduct();
        product.setMaxCapacity(-1);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Max capacity must be at least 1", violations.iterator().next().getMessage());
    }

    @Test
    void testMaxCapacity_Null() {
        Product product = createValidProduct();
        product.setMaxCapacity(null);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertEquals(1, violations.size());
        assertEquals("Maximum capacity is required", violations.iterator().next().getMessage());
    }
}
