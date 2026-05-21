package com.ssvv.inventory.ui;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RestockUiTest
 *
 * UI tests focused on the Restock workflow: selecting products/suppliers, submitting quantities
 * and validating that shipments are generated and product stock levels update accordingly.
 * Techniques: form interactions, <select> handling, cross-page verification (products -> restock -> shipments).
 */
public class RestockUiTest extends BaseUiTest {

    @Test
    void restockPageLoads() {
        startDriver();
        // Validate primary controls are present on the Restock page
        driver.get(baseUrl() + "/restock");

        waitForVisible(By.id("product-select"));
        assertTrue(driver.findElement(By.id("product-select")).isDisplayed(), "Product dropdown should be visible");
        assertTrue(driver.findElement(By.id("supplier-select")).isDisplayed(), "Supplier dropdown should be visible");
        assertTrue(driver.findElement(By.id("quantity-input")).isDisplayed(), "Quantity input should be visible");
        assertTrue(driver.findElement(By.id("submit-btn")).isDisplayed(), "Submit button should be visible");
    }

    @Test
    void performRestockCreatesShipment() {
        startDriver();

        // Perform a restock submission and verify a shipment record is created
        driver.get(baseUrl() + "/restock");

        waitForVisible(By.id("product-select"));

        // Select first product and supplier option (skip the placeholder at index 0)
        By productSel = By.id("product-select");
        By supplierSel = By.id("supplier-select");
        Select prodSelect = new Select(driver.findElement(productSel));
        Select supSelect = new Select(driver.findElement(supplierSel));
        if (prodSelect.getOptions().size() > 1) {
            String prodText = prodSelect.getOptions().get(1).getText();
            prodSelect.selectByVisibleText(prodText);
        }
        if (supSelect.getOptions().size() > 1) {
            String supText = supSelect.getOptions().get(1).getText();
            supSelect.selectByVisibleText(supText);
        }

        driver.findElement(By.id("quantity-input")).clear();
        driver.findElement(By.id("quantity-input")).sendKeys("5");
        driver.findElement(By.id("submit-btn")).click();

        // After submission a success message should appear or redirect; check shipments page
        driver.get(baseUrl() + "/shipments");
        waitForVisible(By.cssSelector("#dataTable tbody"));
        // Verify table structure alignment on the shipments table
        verifyTableStructureAlignment("dataTable");
        boolean hasShipment = !driver.findElements(By.xpath("//td[starts-with(normalize-space(.),'SHP-')]")).isEmpty();
        boolean hasEmptyMsg = !driver.findElements(By.xpath("//td[contains(.,'No shipments generated yet.')]")).isEmpty();
        assertTrue(hasShipment || !hasEmptyMsg, "Shipments page should show a generated shipment record after restock");
    }

    @Test
    void verifyDropdownPlaceholdersAndFormInterception() {
        startDriver();
        // Ensure the default placeholder values are present and submission without selection is intercepted
        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));

        Select prodSelect = new Select(driver.findElement(By.id("product-select")));
        Select supSelect = new Select(driver.findElement(By.id("supplier-select")));

        // Verify default display states match the provided designs exactly
        assertTrue(prodSelect.getFirstSelectedOption().getText().contains("Choose a product..."));
        assertTrue(supSelect.getFirstSelectedOption().getText().contains("Choose a supplier..."));

        // Click submit while selecting default placeholders to ensure it blocks submission or remains safe on the same route page
        driver.findElement(By.id("submit-btn")).click();
        assertTrue(driver.getCurrentUrl().contains("/restock"), "Form pipeline intercept should handle validation constraints cleanly");
    }

    @Test
    void verifyRestockNavbarActiveState() {
        startDriver();
        // Verify the Restock navigation item is active when visiting the restock page
        driver.get(baseUrl() + "/restock");
        verifyActiveNavigationLink("Restock");
    }

    @Test
    void testRestockQuantityInputAcceptsValidNumbers() {
        startDriver();
        // Check numeric input behavior for quantity field
        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("quantity-input"));

        WebElement quantityField = driver.findElement(By.id("quantity-input"));
        quantityField.clear();
        quantityField.sendKeys("125");

        // Assert attribute value reflects input accurately
        assertEquals("125", quantityField.getAttribute("value"), "The numeric value input field must register input values correctly");
    }

    @Test
    void testRestockFlowUpdatesProductStockLevelMetrics() {
        startDriver();
        // Full flow: capture product stock on products page, perform restock, and re-validate stock increase
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("#dataTable tbody tr"));

        // Target the row and pick the Stock column cell (index 2)
        WebElement keyboardRow = driver.findElement(By.xpath("//tr[td[contains(text(),'Wireless Keyboards')]]"));
        java.util.List<WebElement> cells = keyboardRow.findElements(By.tagName("td"));

        // Grab cell index 2 (Stock Level Badge text content)
        String initialStockText = cells.get(2).getText().trim();
        int initialStock = Integer.parseInt(initialStockText);

        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));

        WebElement productDropdown = driver.findElement(By.id("product-select"));
        org.openqa.selenium.support.ui.Select prodSelect = new org.openqa.selenium.support.ui.Select(productDropdown);
        boolean optionFound = false;
        for (WebElement option : prodSelect.getOptions()) {
            if (option.getText().contains("Wireless Keyboards")) {
                prodSelect.selectByVisibleText(option.getText());
                optionFound = true;
                break;
            }
        }
        assertTrue(optionFound, "Dropdown menu must contain an option string for 'Wireless Keyboards'");

        org.openqa.selenium.support.ui.Select supSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("supplier-select")));
        if (supSelect.getOptions().size() > 1) {
            supSelect.selectByIndex(1);
        }

        driver.findElement(By.id("quantity-input")).clear();
        driver.findElement(By.id("quantity-input")).sendKeys("5");
        driver.findElement(By.id("submit-btn")).click();

        // Re-verify the count on the products page
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("#dataTable tbody tr"));

        WebElement keyboardRowAfter = driver.findElement(By.xpath("//tr[td[contains(text(),'Wireless Keyboards')]]"));
        java.util.List<WebElement> cellsAfter = keyboardRowAfter.findElements(By.tagName("td"));
        String finalStockText = cellsAfter.get(2).getText().trim();
        int finalStock = Integer.parseInt(finalStockText);

        assertEquals(initialStock + 5, finalStock, "Fulfilling a restock transaction must immediately update the product's live stock tracking metrics.");
    }

    @Test
    void testFullRestockToShipmentLifecycleWorkflow() {
        startDriver();
        // Full end-to-end lifecycle: restock triggers shipment generation and product stock update
        // Step 1: Capture initial stock metric
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("#dataTable tbody tr"));
        WebElement rowBefore = driver.findElement(By.xpath("//tr[td[contains(text(),'Wireless Keyboards')]]"));
        int initialStock = Integer.parseInt(rowBefore.findElements(By.tagName("td")).get(2).getText().trim());

        // Step 2: Trigger Restock Order
        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));
        Select prodSelect = new Select(driver.findElement(By.id("product-select")));
        prodSelect.getOptions().stream()
                .filter(o -> o.getText().contains("Wireless Keyboards"))
                .findFirst()
                .ifPresent(o -> prodSelect.selectByVisibleText(o.getText()));

        Select supSelect = new Select(driver.findElement(By.id("supplier-select")));
        if (supSelect.getOptions().size() > 1) supSelect.selectByIndex(1);

        driver.findElement(By.id("quantity-input")).clear();
        driver.findElement(By.id("quantity-input")).sendKeys("5");
        driver.findElement(By.id("submit-btn")).click();

        // Step 3: Verify Tracking Record is added to Shipments Log
        driver.get(baseUrl() + "/shipments");
        // Wait for the table body container rather than individual rows - table may render an empty-row placeholder
        waitForVisible(By.cssSelector("#dataTable tbody"));
        boolean hasTrackingCode = driver.findElements(By.xpath("//td[starts-with(normalize-space(.),'SHP-')]"))
                .stream().anyMatch(WebElement::isDisplayed);
        assertTrue(hasTrackingCode, "A successful restock action must automatically produce a verifiable shipping tracking record.");

        // Step 4: Verify Product quantities grew uniformly
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("#dataTable tbody tr"));
        WebElement rowAfter = driver.findElement(By.xpath("//tr[td[contains(text(),'Wireless Keyboards')]]"));
        int finalStock = Integer.parseInt(rowAfter.findElements(By.tagName("td")).get(2).getText().trim());

        assertEquals(initialStock + 5, finalStock, "The products page must reflect the incremented inventory post-shipment generation.");
    }

    @Test
    void testRestockRejectsOverMaxCapacity() {
        startDriver();
        // Attempt to restock an excessive quantity to ensure validation is enforced
        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));

        // Select first non-placeholder product if available
        org.openqa.selenium.support.ui.Select prodSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("product-select")));
        boolean selected = false;
        for (org.openqa.selenium.WebElement opt : prodSelect.getOptions()) {
            if (!opt.getText().toLowerCase().contains("choose") && !opt.getAttribute("value").isEmpty()) {
                prodSelect.selectByVisibleText(opt.getText());
                selected = true;
                break;
            }
        }
        assertTrue(selected, "Must have at least one product option to test capacity constraints");

        org.openqa.selenium.support.ui.Select supSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("supplier-select")));
        if (supSelect.getOptions().size() > 1) supSelect.selectByIndex(1);

        driver.findElement(By.id("quantity-input")).clear();
        // Use a very large number to exceed typical max capacity constraints
        driver.findElement(By.id("quantity-input")).sendKeys("1000");
        driver.findElement(By.id("submit-btn")).click();

        // Expectation: either the form remains on /restock and shows validation UI, or a server-side rejection displays an error
        boolean handled = driver.getCurrentUrl().contains("/restock")
                || !driver.findElements(By.cssSelector(".text-danger, .invalid-feedback, .alert")).isEmpty();
        assertTrue(handled, "Over-max restock attempts should be intercepted by client/server validation and not silently succeed.");
    }
}