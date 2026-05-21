package com.ssvv.inventory.ui;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SupplierUiTest
 *
 * UI tests for the Suppliers page including list rendering, add/edit/delete flows and search behaviors.
 * Techniques employed: form submissions, DOM table assertions, explicit waits, and event-triggering key events.
 */
public class SupplierUiTest extends BaseUiTest {

    @Test
    void suppliersPageLoads() {
        startDriver();
        // Verify the Suppliers page loads and displays the seeded supplier rows and search control
        driver.get(baseUrl() + "/suppliers");
        waitForVisible(By.id("searchInput"));
        assertTrue(driver.findElement(By.id("searchInput")).isDisplayed(), "Search box should be visible on the suppliers page");
        assertTrue(driver.findElements(By.cssSelector("#dataTable tbody tr")).size() >= 2,
                "Seed data should render at least two supplier rows");
        waitForVisible(By.xpath("//td[text() = 'TechCorp Logistics']"));
        assertTrue(driver.findElement(By.xpath("//td[text() = 'TechCorp Logistics']")).isDisplayed());
        waitForVisible(By.xpath("//td[text() = 'Global Supplies Inc.']"));
        assertTrue(driver.findElement(By.xpath("//td[text() = 'Global Supplies Inc.']")).isDisplayed());
    }

    @Test
    void addAndDeleteSupplierFlow() {
        startDriver();
        // End-to-end add and delete supplier flow: create a unique supplier and remove it
        driver.get(baseUrl() + "/suppliers");
        String uniqueName = makeUnique("AutoSupplier");
        waitForVisible(By.cssSelector("form[action='/suppliers/save']"));
        driver.findElement(By.cssSelector("form[action='/suppliers/save'] input[th\\:field='*{name}'], input[name='name']")).sendKeys(uniqueName);
        driver.findElement(By.cssSelector("input[th\\:field='*{contactEmail}'], input[name='contactEmail']")).sendKeys(uniqueName + "@example.com");
        driver.findElement(By.cssSelector("input[th\\:field='*{leadTimeDays}'], input[name='leadTimeDays']")).clear();
        driver.findElement(By.cssSelector("input[th\\:field='*{leadTimeDays}'], input[name='leadTimeDays']")).sendKeys("3");

        driver.findElement(By.cssSelector("form[action='/suppliers/save'] button[type='submit']")).click();

        waitForVisible(By.cssSelector("#dataTable tbody tr"));
        By createdCell = By.xpath("//td[text()='" + uniqueName + "']");
        waitForVisible(createdCell);
        assertTrue(driver.findElement(createdCell).isDisplayed(), "New supplier should appear in supplier list");

        By deleteLink = By.xpath("//tr[td/text()='" + uniqueName + "']//a[contains(@href,'/suppliers/delete') or contains(text(),'Delete')]");
        waitForVisible(deleteLink);
        driver.findElement(deleteLink).click();
        acceptAlertIfPresent();

        waitForNotPresent(By.xpath("//tr[td/text()='" + uniqueName + "']"));
        assertTrue(driver.findElements(By.xpath("//td[text()='" + uniqueName + "']")).isEmpty(), "Supplier should be removed after delete");
    }

    @Test
    void supplierSearchFiltering() {
        startDriver();
        // Validate supplier search filtering reduces visible rows appropriately
        driver.get(baseUrl() + "/suppliers");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        searchInput.sendKeys("Global");

        waitForVisible(By.xpath("//td[text() = 'Global Supplies Inc.']"));
        // Assert that unaligned values are properly filtered out from view
        boolean nonMatchingVisible = !driver.findElements(By.xpath("//td[text() = 'TechCorp Logistics']")).isEmpty()
                && driver.findElement(By.xpath("//td[text() = 'TechCorp Logistics']")).isDisplayed();
        assertFalse(nonMatchingVisible, "Filtered out rows should not stay visible inside the layout view context");
    }

    @Test
    void testSearchWithZeroMatchingResults() {
        startDriver();
        // Ensures zero-match search yields an empty visible row set or an appropriate empty state
        driver.get(baseUrl() + "/suppliers");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        searchInput.sendKeys("XYZ-NON-EXISTENT-TOKEN-999");

        List<WebElement> rows = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        long visible = rows.stream().filter(WebElement::isDisplayed).count();

        boolean noRows = rows.isEmpty() || visible == 0;

        assertTrue(noRows, "The suppliers UI should gracefully handle zero search matches without breaking layout.");
    }

    @Test
    void testSearchCanBeClearedToRestoreDataRows() {
        startDriver();
        // Verifies clearing the search control restores the original table rows
        driver.get(baseUrl() + "/suppliers");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        java.util.List<WebElement> rowsBefore = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        long visibleBefore = rowsBefore.stream().filter(WebElement::isDisplayed).count();

        // Perform a search that reduces results
        searchInput.sendKeys("Global");

        // Clear and trigger events to restore
        searchInput.clear();
        searchInput.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);

        // Wait until the visible row count equals the previous count
        webWait().until(d -> {
            java.util.List<WebElement> rowsNow = d.findElements(By.cssSelector("#dataTable tbody tr"));
            long visibleNow = rowsNow.stream().filter(WebElement::isDisplayed).count();
            return visibleNow == visibleBefore;
        });

        java.util.List<WebElement> rowsAfter = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        long visibleAfter = rowsAfter.stream().filter(WebElement::isDisplayed).count();
        assertTrue(visibleAfter == visibleBefore, "Clearing the search should restore the original visible supplier rows.");
    }

    @Test
    void editSupplierFlow() {
        startDriver();
        // Edit flow for a supplier: modify lead time and assert persisted change
        driver.get(baseUrl() + "/suppliers");

        By firstEditBtn = By.cssSelector("#dataTable tbody tr:first-child a[href*='edit']");
        waitForVisible(firstEditBtn);
        driver.findElement(firstEditBtn).click();

        By leadTimeInput = By.cssSelector("input[name='leadTimeDays']");
        waitForVisible(leadTimeInput);
        driver.findElement(leadTimeInput).clear();
        driver.findElement(leadTimeInput).sendKeys("12");

        driver.findElement(By.cssSelector("button[type='submit']")).click();
        waitForVisible(By.cssSelector("#dataTable tbody"));
        assertTrue(driver.getPageSource().contains("12"), "Modified metric state values should update in row UI data arrays");
    }

    @Test
    void verifySupplierNavbarActiveState() {
        startDriver();
        // Verify the Suppliers nav link is active while on /suppliers
        driver.get(baseUrl() + "/suppliers");
        verifyActiveNavigationLink("Suppliers");
    }
}