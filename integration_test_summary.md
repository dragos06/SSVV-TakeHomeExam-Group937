# Integration Test Summary

## Purpose

This document describes the current integration-style and end-to-end tests in this project, where they live, what they verify, how they run, and common troubleshooting tips. The goal is to make the tests easier to maintain and the assertions easier to update when templates or messages change.

## Summary of existing tests

- Interaction-style controller tests
  - File: `src/test/java/com/ssvv/inventory/integration/InventoryControllerIntegrationTest.java`
  - Approach: SpringBootTest with an embedded web environment (RANDOM_PORT) and a lightweight HTTP driver (`TestRestTemplate`). The test class provides test-scoped primary beans (Mockito mocks) for repositories and the service so controller wiring and view rendering can be exercised while external collaborators are deterministic.
  - Key behaviours asserted:
    - GET /restock returns 200 and contains product and supplier names provided by the mocked repositories
    - POST /restock success path: when `InventoryService.processRestock(...)` is configured to succeed, the controller renders a success message (test looks for the text "Restock successful!")
    - POST /restock error path: when the service throws an exception, the controller returns the view and the error text is present in the HTML
    - GET /restock when repository lists are empty still returns a safe page (title/header renders)
    - POST /restock missing parameters: test accepts either a 4xx client error or a 2xx with binding errors depending on validation config

- Service-level integration tests (wire real service, stub repos)
  - File: `src/test/java/com/ssvv/inventory/integration/InventoryServiceIntegrationWithStubsTest.java`
  - Approach: SpringBootTest that autowires the real `InventoryService` bean while supplying primary/mock repository beans in a `@TestConfiguration`. This verifies business rules and service wiring without touching a real database.
  - Key behaviours asserted:
    - Invalid quantities (<= 0) throw IllegalArgumentException and do not interact with repositories
    - Missing product/supplier results in IllegalArgumentException with appropriate message
    - Successful restock updates the product stock level and saves a `Shipment` (mocks configured to return saved instances)
    - Exceeding maximum capacity throws IllegalStateException (message validated in test)
    - If `ShipmentRepository.save(...)` fails (simulated RuntimeException), the test asserts the failure is propagated and that save was attempted
  - Note: tests call `clearInvocations(...)` at the start of many methods to ignore repository calls performed by the application startup `DataInitializer`.

- Repository (JPA) integration tests

  - Purpose: verify JPA repository queries, constraints and entity mappings against an in-memory database (H2). These tests exercise real persistence wiring, entity validation, relationships and paging behavior.
  - Files:
    - `src/test/java/com/ssvv/inventory/integration/RepositoryIntegrationTest.java`
    - `src/test/java/com/ssvv/inventory/integration/RepositoryMoreIntegrationTest.java`
  - Approach: `@DataJpaTest` — fast slice tests that start a lightweight Spring context with an embedded H2 datasource and the Spring Data repositories. They save and flush entities to validate SQL-level constraints and JPQL queries.
  - Key behaviours asserted:
    - `findProductsBelowMinThreshold()` returns only products whose `stockLevel < minThreshold`.
    - Unique constraints on `Product.name` trigger a `DataIntegrityViolationException` when violated (test uses `saveAndFlush` to force constraint evaluation).
    - Paging/sorting behavior for `ProductRepository.findAll(Pageable)` is exercised (verify page sizes and total pages).
    - Entity relationships: saving a `Shipment` correctly persists `product` and `supplier` foreign keys and allows navigation between entities.
  - Common gotchas / troubleshooting:
    - `Supplier` entity has validation annotations: `contactEmail` (must be non-empty and valid email) and `leadTimeDays` (required, non-negative). Tests that create `Supplier` instances must set `contactEmail` and `leadTimeDays` before saving or will fail with a `ConstraintViolationException`. Example valid values used in tests: `supplier.setContactEmail("supplier@example.com")` and `supplier.setLeadTimeDays(2)`.
    - Use `saveAndFlush(...)` when you need to force constraint checks and database exceptions within the test method rather than at JVM shutdown.

## How to run the tests

Run only the integration tests (interaction/service-level stubs):

```powershell
# From project root
mvn -Dtest=com.ssvv.inventory.integration.* test
```

Run the full test suite (all unit, integration, UI tests):

```powershell
mvn test
```

## CI / Environment notes:

- Keep the fast interaction-style and service-level integration tests in the regular CI pipeline. These are deterministic because repositories are mocked.
## Files references

- Controller tests: `src/test/java/com/ssvv/inventory/integration/InventoryControllerIntegrationTest.java`
- Service tests: `src/test/java/com/ssvv/inventory/integration/InventoryServiceIntegrationWithStubsTest.java`