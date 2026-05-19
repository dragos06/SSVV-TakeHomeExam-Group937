# System Requirements Document

**Project:** Inventory Management System  
**Version:** 1.0  

---

## 1. SYSTEM INITIALIZATION & STATE

| ID | Requirement |
|---|---|
| **INIT-01** | Upon application startup, the system SHALL execute a `CommandLineRunner` data seeding component (`DataInitializer`) to populate the database with mock data for demonstration and testing purposes. This component SHALL check if both the Product and Supplier tables are empty (`count() == 0` for both). If and only if both tables are empty, the system SHALL insert the following seed data: |

**Seed Suppliers:**

| Name | Contact Email | Lead Time (Days) |
|---|---|---|
| TechCorp Logistics | sales@techcorp.com | 5 |
| Global Supplies Inc. | orders@globalsupplies.com | 2 |

**Seed Products:**

| Name | Stock Level | Min Threshold | Max Capacity |
|---|---|---|---|
| Laptop Monitors | 50 | 20 | 200 |
| Wireless Keyboards | 5 | 15 | 100 |
| USB-C Cables | 950 | 100 | 1000 |

> [!NOTE]
> The seed data intentionally includes one product ("Wireless Keyboards") where `stockLevel < minThreshold`, ensuring the Low Stock Report (REQ-16) has a verifiable initial result.

---

## 2. CORE FUNCTIONAL REQUIREMENTS

### 2.1 Product Management (CRUD)

| ID | Requirement |
|---|---|
| **REQ-01** | The system SHALL allow a user to **create** a new Product by submitting a form to `POST /products/save`. |
| **REQ-02** | The system SHALL allow a user to **view** a list of all Products by navigating to `GET /products`. The list SHALL be rendered in the `products` Thymeleaf view. |
| **REQ-03** | The system SHALL allow a user to **edit** an existing Product by navigating to `GET /products/edit/{id}`, which SHALL pre-populate the form with the existing Product data. Submitting the form SHALL update the record via `POST /products/save`. |
| **REQ-04** | The system SHALL allow a user to **delete** a Product by navigating to `GET /products/delete/{id}`. Upon successful deletion, the system SHALL redirect to `GET /products`. |

**Product Validation Constraints:**

| ID | Field | Constraint |
|---|---|---|
| **REQ-05** | `name` | Must be **non-blank**. Database column is `NOT NULL` and `UNIQUE`. Error: *"Product name cannot be blank"*. |
| **REQ-06** | `stockLevel` | Must be **non-null** and **≥ 0**. Error: *"Stock level is required"* / *"Stock cannot be negative"*. |
| **REQ-07** | `minThreshold` | Must be **non-null** and **≥ 0**. Error: *"Minimum threshold is required"* / *"Threshold cannot be negative"*. |
| **REQ-08** | `maxCapacity` | Must be **non-null** and **≥ 1**. Error: *"Maximum capacity is required"* / *"Max capacity must be at least 1"*. |

---

### 2.2 Supplier Management (CRUD)

| ID | Requirement |
|---|---|
| **REQ-09** | The system SHALL allow a user to **create** a new Supplier by submitting a form to `POST /suppliers/save`. |
| **REQ-10** | The system SHALL allow a user to **view** a list of all Suppliers by navigating to `GET /suppliers`. The list SHALL be rendered in the `suppliers` Thymeleaf view. |
| **REQ-11** | The system SHALL allow a user to **edit** an existing Supplier by navigating to `GET /suppliers/edit/{id}`, which SHALL pre-populate the form with the existing Supplier data. Submitting the form SHALL update the record via `POST /suppliers/save`. |
| **REQ-12** | The system SHALL allow a user to **delete** a Supplier by navigating to `GET /suppliers/delete/{id}`. Upon successful deletion, the system SHALL redirect to `GET /suppliers`. |

**Supplier Validation Constraints:**

| ID | Field | Constraint |
|---|---|---|
| **REQ-09a** | `name` | Must be **non-blank**. Database column is `NOT NULL` and `UNIQUE`. Error: *"Supplier name cannot be blank"*. |
| **REQ-09b** | `contactEmail` | Must be **non-blank** and conform to a **valid email format**. Error: *"Email cannot be blank"* / *"Must be a valid email format"*. |
| **REQ-09c** | `leadTimeDays` | Must be **non-null** and **≥ 0**. Error: *"Lead time is required"* / *"Lead time cannot be negative"*. |

---

### 2.3 Shipment Management (Read / Delete)

| ID | Requirement |
|---|---|
| **REQ-13** | The system SHALL allow a user to **view** a list of all Shipments by navigating to `GET /shipments`. The list SHALL be rendered in the `shipments` Thymeleaf view. |
| **REQ-14** | The system SHALL allow a user to **delete** a Shipment by navigating to `GET /shipments/delete/{id}`. Upon successful deletion, the system SHALL redirect to `GET /shipments`. |


---

### 2.4 Process Restock Transaction

| ID | Requirement |
|---|---|
| **REQ-15** | The system SHALL provide a "Restock" page at `GET /restock` that displays dropdown selectors populated with all existing Products and Suppliers, and an input field for `quantity`. Upon form submission to `POST /restock`, the system SHALL invoke `InventoryService.processRestock(productId, supplierId, quantity)`. |

**Execution Contract for `processRestock`:**

| Step | Behavior |
|---|---|
| **REQ-15a** | **Guard: Non-positive quantity.** If `quantity <= 0`, the method SHALL throw an `IllegalArgumentException` with the message *"Cannot order a zero or negative quantity."* **before** any database lookup occurs. |
| **REQ-15b** | **Guard: Product existence.** The system SHALL look up the Product by `productId`. If no Product is found, the method SHALL throw an `IllegalArgumentException` with the message *"Product not found"*. |
| **REQ-15c** | **Guard: Supplier existence.** The system SHALL look up the Supplier by `supplierId`. If no Supplier is found, the method SHALL throw an `IllegalArgumentException` with the message *"Supplier not found"*. |
| **REQ-15d** | **Guard: Max capacity overflow.** If `(product.stockLevel + quantity) > product.maxCapacity`, the method SHALL throw an `IllegalStateException` with the message *"Restock quantity exceeds maximum warehouse capacity for this product."* No stock update SHALL occur. |
| **REQ-15e** | **State mutation: Update stock.** If all guards pass, the system SHALL increment the Product's `stockLevel` by `quantity` and persist the updated Product entity. |
| **REQ-15f** | **State mutation: Create Shipment.** The system SHALL create a new Shipment record with the associated Product, Supplier, the input `quantity`, and `shipmentDate` set to the current date (`LocalDate.now()`). The Shipment SHALL be persisted and returned. |
| **REQ-15g** | **UI feedback: Success.** On success, the controller SHALL add the attribute `successMessage` with value *"Restock successful!"* to the model and re-render the `restock` view. |
| **REQ-15h** | **UI feedback: Failure.** On any exception, the controller SHALL add the attribute `errorMessage` with the exception's message to the model and re-render the `restock` view. No redirect SHALL occur. |

---

### 2.5 Low Stock Alert Report

| ID | Requirement |
|---|---|
| **REQ-16** | The system SHALL provide a report page at `GET /report`. This page SHALL query for all Products where `stockLevel < minThreshold` (strict less-than) and display them as the `lowStockProducts` attribute in the `report` Thymeleaf view. |

---

## 3. NON-FUNCTIONAL REQUIREMENTS & ERROR HANDLING

### 3.1 Transactional Integrity

| ID | Requirement |
|---|---|
| **NFR-01** | The `processRestock` method SHALL execute within a **single transactional boundary** (annotated with `@Transactional`). If any exception occurs after the transaction begins, all database mutations within that invocation (stock level update and shipment creation) SHALL be **rolled back atomically**. |
| **NFR-02** | Standard CRUD operations on Product and Supplier (save, delete) are executed directly via `JpaRepository` methods within the Controller layer and do **not** carry an explicit service-level `@Transactional` annotation. Each repository call operates within its own default JPA transaction. |

---

### 3.2 Validation & Error Handling at the Controller Layer

| ID | Requirement |
|---|---|
| **NFR-03** | For **Product** and **Supplier** save operations: the Controller SHALL accept entities annotated with `@Valid`. If `BindingResult` contains validation errors, the controller SHALL **not** persist the entity. Instead, it SHALL re-render the same view with the current entity list and the `BindingResult` errors bound to the form, enabling the UI to display field-level error messages contextually. |
| **NFR-04** | For **Product** and **Supplier** save operations: if the `repository.save()` call throws a runtime exception (e.g., unique constraint violation on `name`), the controller SHALL catch the exception, add a programmatic field error to the `BindingResult` via `result.rejectValue("name", ...)` with a contextual message (e.g., *"Product name already exists or database error."*), and re-render the view without redirecting. |

---


