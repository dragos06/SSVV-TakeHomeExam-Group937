package com.ssvv.inventory.ui;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ShipmentUiTest
 *
 * Tests for the Shipments page and related lifecycle behaviors. Covers generating shipments via the restock
 * flow, searching/filtering the shipments table, and deletion of shipment records.
 * Techniques: cross-page verification, search input simulation, and table row assertions.
 */
public class ShipmentUiTest extends BaseUiTest {

    @Test
    void shipmentsPageLoads() {
        startDriver();
        // Verify shipments page loads, show the informational note and expected search control
        driver.get(baseUrl() + "/shipments");

        // Wait for explicit search input by id to avoid fragile placeholder selector behaviour
        waitForVisible(By.id("searchInput"));
        // Ensure table header and body alignment on the shipments table
        waitForVisible(By.cssSelector("#dataTable tbody"));
        verifyTableStructureAlignment("dataTable");
        assertTrue(driver.findElement(By.id("searchInput")).isDisplayed(),
                "Search box should be visible on the shipments page");

        assertTrue(driver.getPageSource().contains("Shipments are generated automatically via the"),
                "Informational system callout note must be present");

        assertTrue(driver.getPageSource().contains("No shipments generated yet.") || !driver.findElements(By.cssSelector("#dataTable tbody tr")).isEmpty(),
                "Shipments page should either show an empty state message or at least one shipment row");
    }

    @Test
    void deleteShipmentIfExists() {
        startDriver();
        // If shipments exist, delete the first and verify it is removed from the list
        driver.get(baseUrl() + "/shipments");

        // Wait for table container area to load completely
        waitForVisible(By.cssSelector("#dataTable tbody"));

        // Variant: Explicitly check for the empty table indicator text
        boolean hasNoShipmentsMessage = driver.getPageSource().contains("No shipments generated yet.");

        // If the empty message is NOT present, we expect active shipment rows WITH delete capability to exist
        if (!hasNoShipmentsMessage) {
            By firstDelete = By.cssSelector("#dataTable tbody tr:first-child a[href*='/shipments/delete'], #dataTable tbody tr:first-child a.btn-danger");

            // This will intentionally throw a visible timeout if shipments exist but the action button is missing!
            waitForVisible(firstDelete);

            WebElement deleteButton = driver.findElement(firstDelete);
            WebElement correspondingRow = deleteButton.findElement(By.xpath("./ancestor::tr"));
            String targetRowText = correspondingRow.getText();

            deleteButton.click();
            acceptAlertIfPresent();

            waitForVisible(By.cssSelector("#dataTable tbody"));

            // Verify that the exact text content of that specific row is removed
            boolean isRowPresent = driver.getPageSource().contains(targetRowText) && !targetRowText.trim().isEmpty();
            assertFalse(isRowPresent, "Deleted shipment record should no longer match active table rows");
        }
    }

    @Test
    void verifyShipmentSearchFiltering() {
        startDriver();
        // Generate a shipment record first via the automated restock flow to provide live search data
        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));

        org.openqa.selenium.support.ui.Select prodSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("product-select")));
        org.openqa.selenium.support.ui.Select supSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("supplier-select")));

        if (prodSelect.getOptions().size() > 1 && supSelect.getOptions().size() > 1) {
            prodSelect.selectByIndex(1);
            supSelect.selectByIndex(1);

            driver.findElement(By.id("quantity-input")).clear();
            driver.findElement(By.id("quantity-input")).sendKeys("42");
            driver.findElement(By.id("submit-btn")).click();
        }

        // Navigate back to shipment table and apply filter
        driver.get(baseUrl() + "/shipments");
        By searchBox = By.id("searchInput");
        waitForVisible(searchBox);

        WebElement searchInput = driver.findElement(searchBox);
        searchInput.sendKeys("42");

        // Validate table content is selectively modified to match search expectations
        waitForVisible(By.cssSelector("#dataTable tbody tr"));
        assertTrue(driver.findElement(By.cssSelector("#dataTable tbody")).getText().contains("42"),
                "Search results must strictly retain elements matching the typed criteria value");
    }

    @Test
    void testSearchCanBeClearedToRestoreDataRows() {
        startDriver();
        // Ensure search can be cleared and the original visible rows are restored
        driver.get(baseUrl() + "/shipments");
        By searchBox = By.id("searchInput");
        waitForVisible(searchBox);

        java.util.List<WebElement> rowsBefore = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        long visibleBefore = rowsBefore.stream().filter(WebElement::isDisplayed).count();

        WebElement searchInput = driver.findElement(searchBox);
        searchInput.sendKeys("NON-EXISTENT-SEARCH-TERM-12345");

        // Clear search and trigger events
        searchInput.clear();
        searchInput.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);

        // Wait until visible count equals original
        webWait().until(d -> {
            java.util.List<WebElement> rowsNow = d.findElements(By.cssSelector("#dataTable tbody tr"));
            long visibleNow = rowsNow.stream().filter(WebElement::isDisplayed).count();
            return visibleNow == visibleBefore;
        });

        java.util.List<WebElement> rowsAfter = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        long visibleAfter = rowsAfter.stream().filter(WebElement::isDisplayed).count();
        assertTrue(visibleAfter == visibleBefore, "Clearing the shipment search should restore the original visible shipment rows.");
    }

    @Test
    void testSearchWithZeroMatchingResults() {
        startDriver();
        driver.get(baseUrl() + "/products");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        // Type a random sequence that cannot possibly match seed items
        searchInput.sendKeys("XYZ-NON-EXISTENT-TOKEN-999");

        // Fixed XPath: Selects rows that do not have style="display: none" applied by dynamic filter scripts
        int visibleItemRows = driver.findElements(By.xpath("//table[@id='dataTable']/tbody/tr[not(contains(@style, 'display: none'))]")).size();

        // Backup validation check fallback for applications using server-side reloads
        boolean hasNoResultsMsg = driver.getPageSource().contains("No matching items")
                || driver.findElements(By.cssSelector("#dataTable tbody tr")).isEmpty();

        assertTrue(visibleItemRows == 0 || hasNoResultsMsg, "The UI data-grid should gracefully handle zero query matches without breaking layouts.");
    }
}