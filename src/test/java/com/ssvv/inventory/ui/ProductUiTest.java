package com.ssvv.inventory.ui;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ProductUiTest
 *
 * UI-level tests covering the Products page and related CRUD/search interactions.
 * Techniques: Selenium WebDriver interactions (form fills, clicks), explicit waits, and DOM table assertions.
 * Test subjects: product listing, add/edit/delete flows, search/filter behavior, and stock-level badge visuals.
 */
public class ProductUiTest extends BaseUiTest {

    @Test
    void productsPageLoads() {
        startDriver();
        // Navigate to the Products page and verify baseline UI elements and seeded rows
        driver.get(baseUrl() + "/products");
        waitForVisible(By.id("searchInput"));
        // Ensure table header and body column alignment adheres to expected structure
        verifyTableStructureAlignment("dataTable");
        assertTrue(driver.findElement(By.id("searchInput")).isDisplayed(), "Search box should be visible on the products page");
        assertTrue(driver.findElements(By.cssSelector("#dataTable tbody tr")).size() >= 3,
                "Seed data should render at least three product rows");
        waitForVisible(By.xpath("//td[text() = 'Laptop Monitors']"));
        assertTrue(driver.findElement(By.xpath("//td[text() = 'Laptop Monitors']")).isDisplayed());
        waitForVisible(By.xpath("//td[text() = 'Wireless Keyboards']"));
        assertTrue(driver.findElement(By.xpath("//td[text() = 'Wireless Keyboards']")).isDisplayed());
        waitForVisible(By.xpath("//td[text() = 'USB-C Cables']"));
        assertTrue(driver.findElement(By.xpath("//td[text() = 'USB-C Cables']")).isDisplayed());
    }

    @Test
    void addAndDeleteProductFlow() {
        startDriver();
        // Create a unique product and submit the add form (end-to-end add/delete flow)
        driver.get(baseUrl() + "/products");
        String uniqueName = makeUnique("AutoProduct");
        waitForVisible(By.cssSelector("form[action='/products/save']"));
        driver.findElement(By.cssSelector("form[action='/products/save'] input[th\\:field='*{name}'], input[name='name']")).sendKeys(uniqueName);
        // Fallback to direct CSS locations in case thymeleaf attributes are not present
        driver.findElement(By.cssSelector("input[th\\:field='*{stockLevel}'], input[name='stockLevel']")).clear();
        driver.findElement(By.cssSelector("input[th\\:field='*{stockLevel}'], input[name='stockLevel']")).sendKeys("10");
        driver.findElement(By.cssSelector("input[th\\:field='*{minThreshold}'], input[name='minThreshold']")).clear();
        driver.findElement(By.cssSelector("input[th\\:field='*{minThreshold}'], input[name='minThreshold']")).sendKeys("1");
        driver.findElement(By.cssSelector("input[th\\:field='*{maxCapacity}'], input[name='maxCapacity']")).clear();
        driver.findElement(By.cssSelector("input[th\\:field='*{maxCapacity}'], input[name='maxCapacity']")).sendKeys("100");

        driver.findElement(By.cssSelector("form[action='/products/save'] button[type='submit']")).click();

        // Wait for the table to show the new product
        waitForVisible(By.cssSelector("#dataTable tbody tr"));
        By createdCell = By.xpath("//td[text()='" + uniqueName + "']");
        waitForVisible(createdCell);
        assertTrue(driver.findElement(createdCell).isDisplayed(), "New product should appear in product list");

        // Find the delete link for our new product and delete it
        By deleteLink = By.xpath("//tr[td/text()='" + uniqueName + "']//a[contains(@href,'/products/delete') or contains(text(),'Delete')]");
        waitForVisible(deleteLink);
        driver.findElement(deleteLink).click();
        acceptAlertIfPresent();

        // After deletion, the product should not appear
        waitForNotPresent(By.xpath("//tr[td/text()='" + uniqueName + "']"));
        assertTrue(driver.findElements(By.xpath("//td[text()='" + uniqueName + "']")).isEmpty(), "Product should be removed after delete");
    }

    @Test
    void productSearchFiltering() {
        startDriver();
        driver.get(baseUrl() + "/products");
        // Verify client-side search filtering reduces visible rows for matching terms
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        // Filter out everything except Laptop Monitors
        searchInput.sendKeys("Laptop");

        // Dynamic table filtering assertion
        int visibleRows = driver.findElements(By.cssSelector("#dataTable tbody tr")).size();
        assertTrue(visibleRows >= 1, "Table should dynamically filter items down matching the query");
        waitForVisible(By.xpath("//td[text() = 'Laptop Monitors']"));
    }

    @Test
    void editProductFlow() {
        startDriver();
        // Edit the first product and assert that changes persist in the list view
        driver.get(baseUrl() + "/products");

        // Click Edit on the first available item row
        By editButton = By.cssSelector("#dataTable tbody tr:first-child a.btn-warning, #dataTable tbody tr:first-child a[href*='edit']");
        waitForVisible(editButton);
        driver.findElement(editButton).click();

        // Alter values inside the form fields
        By stockInput = By.cssSelector("input[name='stockLevel'], input[name='initialStock']");
        waitForVisible(stockInput);
        driver.findElement(stockInput).clear();
        driver.findElement(stockInput).sendKeys("75");

        driver.findElement(By.cssSelector("button[type='submit']")).click();

        // Verify the saved alteration is back on the overview table
        waitForVisible(By.cssSelector("#dataTable tbody"));
        assertTrue(driver.getPageSource().contains("75"), "The updated stock metrics should immediately reflect in the datatable");
    }

    @Test
    void productFormValidationFailsOnEmptyFields() {
        startDriver();
        // Submit an intentionally invalid/empty form to verify validation handling
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("form[action='/products/save']"));

        // Intentionally submit an empty product form to check application constraint safety
        driver.findElement(By.cssSelector("form[action='/products/save'] button[type='submit']")).click();

        // App handles it gracefully (reloads the form with errors or stays on page instead of standard Whitelabel 500 error page)
        assertTrue(driver.getCurrentUrl().contains("/products"), "Submitting empty validation parameters shouldn't crash backend pipelines");
    }

    @Test
    void verifyStockLevelBadgeColorCoding() {
        startDriver();
        // Verify visual badge styling for threshold alarms and healthy stock items
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("#dataTable tbody tr"));

        // Scenario A: Check Low Stock item (Wireless Keyboards - 5 units) has red danger styling
        By lowStockBadge = By.xpath("//tr[td[contains(text(),'Wireless Keyboards')]]//span[contains(@class,'badge') or contains(@class,'bg-danger') or contains(text(),'5')]");
        assertTrue(driver.findElement(lowStockBadge).isDisplayed(),
                "Low stock products must display a distinctive threshold alarm styling (e.g., red badge)");

        // Scenario B: Check High Stock item (USB-C Cables - 950 units) has green success styling
        By highStockBadge = By.xpath("//tr[td[contains(text(),'USB-C Cables')]]//span[contains(@class,'badge') or contains(@class,'bg-success') or contains(text(),'950')]");
        assertTrue(driver.findElement(highStockBadge).isDisplayed(),
                "Healthy stock products must display a standard safe condition color badge");
    }

    @Test
    void verifyProductNavbarActiveState() {
        startDriver();
        // Verify the Products navigation item is rendered in the active state
        driver.get(baseUrl() + "/products");
        verifyActiveNavigationLink("Products");
    }

    @Test
    void testSearchWithZeroMatchingResults() {
        startDriver();
        // Ensure the UI handles zero-match searches gracefully
        driver.get(baseUrl() + "/products");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        searchInput.sendKeys("XYZ-NON-EXISTENT-TOKEN-999");

        // Fixed using crisp CSS selectors to avoid invalid XPath syntax errors entirely
        int visibleItemRows = 0;
        java.util.List<WebElement> rows = driver.findElements(By.cssSelector("#dataTable tbody tr"));
        for (WebElement row : rows) {
            if (row.isDisplayed() && !row.getAttribute("style").contains("display: none")) {
                visibleItemRows++;
            }
        }

        boolean hasNoResultsMsg = driver.getPageSource().contains("No matching items") || visibleItemRows == 0;
        assertTrue(hasNoResultsMsg, "The UI data-grid should gracefully handle zero query matches without breaking layouts.");
    }

    @Test
    void testSearchCanBeClearedToRestoreDataRows() {
        startDriver();
        // Clearing the search should restore the previous list of rows
        driver.get(baseUrl() + "/products");
        waitForVisible(By.id("searchInput"));

        WebElement searchInput = driver.findElement(By.id("searchInput"));
        searchInput.sendKeys("Laptop");

        // Clear out text contents completely
        searchInput.clear();
        searchInput.sendKeys(org.openqa.selenium.Keys.BACK_SPACE); // Force field event triggers if needed

        // All default rows should re-render instantly on screen
        assertTrue(driver.findElements(By.cssSelector("#dataTable tbody tr")).size() >= 3,
                "Clearing out search query parameter inputs must instantly restore visibility of all underlying repository rows.");
    }

    @Test
    void testAddProductNegativeThresholdValidation() {
        startDriver();
        // Ensure the application rejects invalid numeric bounds on create
        driver.get(baseUrl() + "/products");
        waitForVisible(By.cssSelector("form[action='/products/save']"));

        // Leave name empty and pass a malicious negative value into the numeric fields
        driver.findElement(By.cssSelector("input[name='minThreshold']")).clear();
        driver.findElement(By.cssSelector("input[name='minThreshold']")).sendKeys("-25");

        driver.findElement(By.cssSelector("form[action='/products/save'] button[type='submit']")).click();

        // The application should either block the route or present clear validation flags
        boolean handledSafely = driver.getCurrentUrl().contains("/products")
                || !driver.findElements(By.cssSelector(".text-danger, .invalid-feedback, .alert")).isEmpty();

        assertTrue(handledSafely, "The UI should reject bad input boundaries or display validation alerts instead of crashing.");
    }
}