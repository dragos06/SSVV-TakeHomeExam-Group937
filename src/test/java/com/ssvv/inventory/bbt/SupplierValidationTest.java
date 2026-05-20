package com.ssvv.inventory.bbt;

import com.ssvv.inventory.entity.Supplier;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SupplierValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private Supplier createValidSupplier() {
        Supplier supplier = new Supplier();
        supplier.setName("Valid Supplier");
        supplier.setContactEmail("sales@techcorp.com");
        supplier.setLeadTimeDays(5);
        return supplier;
    }

    // --- Name Validation Tests (REQ-09a) ---

    @Test
    void testName_Valid() {
        Supplier supplier = createValidSupplier();
        supplier.setName("Global Supplies Inc.");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertTrue(violations.isEmpty(), "Valid supplier name should not produce violations");
    }

    @Test
    void testName_Null() {
        Supplier supplier = createValidSupplier();
        supplier.setName(null);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Supplier name cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testName_Empty() {
        Supplier supplier = createValidSupplier();
        supplier.setName("");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Supplier name cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testName_Whitespace() {
        Supplier supplier = createValidSupplier();
        supplier.setName("    ");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Supplier name cannot be blank", violations.iterator().next().getMessage());
    }

    // --- ContactEmail Validation Tests (REQ-09b) ---

    @Test
    void testContactEmail_Valid() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("orders@globalsupplies.com");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertTrue(violations.isEmpty(), "Valid email should not produce violations");
    }

    @Test
    void testContactEmail_Null() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail(null);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Email cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testContactEmail_Empty() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Email cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testContactEmail_Whitespace() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("    ");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        boolean hasBlankError = violations.stream()
                .anyMatch(v -> v.getMessage().equals("Email cannot be blank"));
        assertTrue(hasBlankError);
    }

    @Test
    void testContactEmail_InvalidFormat_NoAt() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("invalid-email.com");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Must be a valid email format", violations.iterator().next().getMessage());
    }

    @Test
    void testContactEmail_InvalidFormat_NoDomain() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("user@");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Must be a valid email format", violations.iterator().next().getMessage());
    }

    @Test
    void testContactEmail_InvalidFormat_NoUser() {
        Supplier supplier = createValidSupplier();
        supplier.setContactEmail("@domain.com");

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Must be a valid email format", violations.iterator().next().getMessage());
    }

    // --- LeadTimeDays Validation Tests (REQ-09c) ---

    @Test
    void testLeadTimeDays_ValidPositive() {
        Supplier supplier = createValidSupplier();
        supplier.setLeadTimeDays(10);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testLeadTimeDays_ZeroBoundary() {
        Supplier supplier = createValidSupplier();
        supplier.setLeadTimeDays(0);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testLeadTimeDays_NegativeBoundary() {
        Supplier supplier = createValidSupplier();
        supplier.setLeadTimeDays(-1);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Lead time cannot be negative", violations.iterator().next().getMessage());
    }

    @Test
    void testLeadTimeDays_Null() {
        Supplier supplier = createValidSupplier();
        supplier.setLeadTimeDays(null);

        Set<ConstraintViolation<Supplier>> violations = validator.validate(supplier);
        assertEquals(1, violations.size());
        assertEquals("Lead time is required", violations.iterator().next().getMessage());
    }
}
