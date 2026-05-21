# UI Testing Summary

## Overview
This document summarizes the Selenium-based UI testing approach used in this project, the concrete techniques implemented in the test harness and tests, and what UI pages and flows are covered by the test suite.

## Test harness and common helpers
- Test harness: `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` is used so tests start the application on a random port and exercise full end-to-end flows.
- Browser: Selenium `ChromeDriver` (via Selenium Manager / default discovery) with `ChromeOptions` configured in `src/test/java/com/ssvv/inventory/ui/BaseUiTest.java`.
  - Default runs headless (controlled by system property `selenium.headless`, default `true`). Options include `--disable-gpu`, `--no-sandbox`, `--disable-dev-shm-usage`, `--window-size=1400,1200`, and `--kiosk-printing` to support Print checks.
- Test isolation: `@BeforeEach` resets repositories (`ProductRepository`, `SupplierRepository`) and calls the `DataInitializer` to reseed deterministic data for UI assertions.
- Synchronization: explicit waits via `WebDriverWait` and helper methods `waitForVisible`, `waitForNotPresent`, and `webWait()` reduce flakiness compared to sleeps.
- Utility helpers in `BaseUiTest`:
  - `makeUnique(prefix)` to generate unique names for entities
  - `selectByVisibleText` / Select usage for `<select>` controls
  - `acceptAlertIfPresent` for alert/confirmation dialogs
  - `verifyTableStructureAlignment(tableId)` to ensure table header/row alignment
  - `verifyActiveNavigationLink(linkText)` to assert navigation highlight (class OR computed color via Selenium Color helper)

## Techniques implemented in tests
- Selenium WebDriver interactions: navigation, clicking, typing, clearing form fields, submitting forms.
- Explicit waits and conditional polling to synchronize with client-side rendering.
- Robust locator strategies: CSS selectors and XPath fallbacks; tests often include multiple selector forms to handle thymeleaf attributes or plain HTML fallbacks.
- Select handling: using org.openqa.selenium.support.ui.Select for dropdowns, with logic to skip placeholder options.
- Alert handling: accepting browser alerts after destructive actions (delete flows).
- Table and data-grid assertions: counting rows, checking visible rows vs hidden rows, and the `verifyTableStructureAlignment` helper ensures header/body column counts match.
- Visual/assertive checks: verify badge color/status (uses Selenium Color conversion) and active nav state.
- Cross-page workflow verification: several tests exercise multi-page flows (e.g., restock -> shipments -> products -> report) to validate side effects across endpoints.
- Validation & negative tests: submitting invalid values (empty fields, negative numbers, over-capacity) and asserting graceful handling (validation messages or staying on page rather than failing with server error).
- Print button/flow checks: Print button presence and clickability are asserted; Chrome started with kiosk-printing to reduce print dialog interference.

## Coverage mapping (what's covered and where)
- Products (`/products`)
  - File: `src/test/java/com/ssvv/inventory/ui/ProductUiTest.java`
  - Coverage: page loads, product list presence, add product (end-to-end create + delete), edit product, search/filter behavior, empty-search handling, clearing search, form validation on save, badge color coding (low/high stock), navbar active state

- Suppliers (`/suppliers`)
  - File: `src/test/java/com/ssvv/inventory/ui/SupplierUiTest.java`
  - Coverage: page loads, add + delete supplier, edit supplier, search/filter, zero-match handling, clearing search, navbar active state

- Restock (`/restock`)
  - File: `src/test/java/com/ssvv/inventory/ui/RestockUiTest.java`
  - Coverage: page loads, dropdown placeholders and submission blocking, create restock (quantity), shipments creation via restock, numeric input validation, restock updating product stock metrics, end-to-end restock->shipment->product lifecycle, reject over-max capacity submissions, navbar active state

- Shipments (`/shipments`)
  - File: `src/test/java/com/ssvv/inventory/ui/ShipmentUiTest.java`
  - Coverage: page loads and informational note, table header/body alignment, delete shipment if exists, search/filtering, zero-match handling, clearing search restores rows

- Reports / Low Stock (`/report`)
  - File: `src/test/java/com/ssvv/inventory/ui/ReportUiTest.java`
  - Coverage: report rendering, header/alert badge, print button interactivity, restock-now link navigation to `/restock`, low-stock deficit calculations for seeded items, restocking a low-stock item removes it from the report, navbar active state

## Additional notes on quality and robustness
- Care is taken to avoid fragile selectors: tests try both thymeleaf attributes and standard input/name-based selectors.
- Explicit waits limit timing-related flakiness. Tests use short waits for alerts and 5s waits for general UI elements; these can be increased if CI environment is slow.
- Tests verify empty-state messages in addition to row counts to support both client-side and server-side implementations of filtering.
- Use of deterministic reseeding of data per test ensures reproducible assertions.

## How to run the UI tests
On Windows using the provided Maven wrapper (`mvnw.cmd`) from the repository root (PowerShell):

```powershell
# Run the full test suite (default: headless=true)
.\mvnw.cmd test
```

### CI / environment notes
- By default ChromeDriver is discovered via Selenium Manager; CI images must have a compatible Chrome/Chromium binary. If CI requires a specific driver, set up appropriate browser/driver installation steps.
- Keep `selenium.headless=true` for CI to avoid the need for a display server.
- If CI is flaky due to timing, increase explicit wait durations in `BaseUiTest.webWait()` or add retries around the most flaky interaction points.
- Consider capturing screenshots and page source on test failure for faster debugging. A recommended approach is to add a JUnit TestWatcher or extension that on failure writes a screenshot to `target/` and attaches it to the CI artifact bundle.

### Files referenced
- `src/test/java/com/ssvv/inventory/ui/BaseUiTest.java`
- `src/test/java/com/ssvv/inventory/ui/ProductUiTest.java`
- `src/test/java/com/ssvv/inventory/ui/SupplierUiTest.java`
- `src/test/java/com/ssvv/inventory/ui/RestockUiTest.java`
- `src/test/java/com/ssvv/inventory/ui/ShipmentUiTest.java`
- `src/test/java/com/ssvv/inventory/ui/ReportUiTest.java`


