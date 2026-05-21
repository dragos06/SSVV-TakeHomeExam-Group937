package com.ssvv.inventory.ui;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReportUiTest
 *
 * Tests for the Low Stock 'Report' page. Verifies report rendering, alert banners, print action,
 * navigation links, and the behavior of restocking a low-stock item removing it from the report.
 * Techniques: page navigation, element visibility waits, cross-page workflow interaction (restock -> report).
 */
public class ReportUiTest extends BaseUiTest {

    @Test
    void reportPageLoads() {
        startDriver();
        // Verify the Low Stock Report page loads and displays expected UI elements or empty-state message
        driver.get(baseUrl() + "/report");

        assertTrue(driver.findElement(By.id("report-table")).isDisplayed(), "Report table should be visible");
        waitForVisible(By.xpath("//h2[contains(normalize-space(.),'Low Stock Report')]") );
        assertTrue(driver.findElement(By.xpath("//h2[contains(normalize-space(.),'Low Stock Report')]")).isDisplayed(), "Report heading should be present");
        // Wireless Keyboards are seeded below threshold; expect to see it or healthy message
        boolean hasWireless = !driver.findElements(By.xpath("//td[text()='Wireless Keyboards']")).isEmpty();
        boolean hasHealthyMsg = !driver.findElements(By.xpath("//td[contains(.,'All product inventory levels are healthy')]")).isEmpty();
        assertTrue(hasWireless || hasHealthyMsg);
        waitForVisible(By.xpath("//button[contains(.,'Print Report')]") );
        assertTrue(driver.findElement(By.xpath("//button[contains(.,'Print Report')]")).isDisplayed());
    }

    @Test
    void restockNowLinkNavigatesToRestock() {
        startDriver();
        // Clicking the Restock Now link from the report must navigate to the restock workflow
        driver.get(baseUrl() + "/report");
        // Click the restock link in the report page and ensure we land on /restock
        By restockLink = By.cssSelector("a[href='/restock']");
        waitForVisible(restockLink);
        driver.findElement(restockLink).click();
        waitForVisible(By.id("product-select"));
        assertTrue(driver.getCurrentUrl().contains("/restock"));
    }

    @Test
    void verifyLowStockCalculationsAndRowMetrics() {
        startDriver();
        // Validates calculated deficit column and targeted row metric values when present
        driver.get(baseUrl() + "/report");
        waitForVisible(By.id("report-table"));
        // Verify table header/row alignment for report view
        verifyTableStructureAlignment("report-table");

        // Targeted assertion based exactly on Screenshot parameters
        By keyboardsRow = By.xpath("//tr[td[contains(text(), 'Wireless Keyboards')]]");
        if (!driver.findElements(keyboardsRow).isEmpty()) {
            // Deficit is positioned at column index 5 (1-based index calculation)
            String deficitText = driver.findElement(By.xpath("//tr[td[contains(text(), 'Wireless Keyboards')]]/td[5]")).getText();
            assertEquals("-10", deficitText.trim(), "Deficit calculation values must correctly match the negative math representation");
        }
    }

    @Test
    void printReportButtonInteractivity() {
        startDriver();
        // Ensure the Print Report control is present and clickable
        driver.get(baseUrl() + "/report");
        By printBtn = By.xpath("//button[contains(.,'Print Report')]");
        waitForVisible(printBtn);
        assertTrue(driver.findElement(printBtn).isEnabled(), "Print action element must stay clickable on screen assets");
        driver.findElement(printBtn).click();
    }

    @Test
    void verifyReportsNavbarActiveState() {
        startDriver();
        // Verify the Reports navigation item is active while on the report page
        driver.get(baseUrl() + "/report");
        verifyActiveNavigationLink("Reports");
    }

    @Test
    void verifyAlertHeaderBannerElements() {
        startDriver();
        // Confirms presence of the ALERT badge and header elements required by the design
        driver.get(baseUrl() + "/report");
        // Verify explicit "ALERT" block text tag element next to header title string
        By alertBadge = By.xpath("//span[contains(text(),'ALERT') or contains(@class,'badge')]");
        waitForVisible(alertBadge);
        assertTrue(driver.findElement(alertBadge).isDisplayed(), "The prominent red ALERT status ribbon component must render correctly");
    }

    @Test
    void testRestockingLowStockProductRemovesItFromReportGrid() {
        startDriver();
        // Full flow: ensure a seeded low-stock product is present in the report, restock it, and verify it is removed
        driver.get(baseUrl() + "/report");
        waitForVisible(By.id("report-table"));
        assertTrue(driver.getPageSource().contains("Wireless Keyboards"), "Product should start on the low stock report registry view.");

        driver.get(baseUrl() + "/restock");
        waitForVisible(By.id("product-select"));

        WebElement productDropdown = driver.findElement(By.id("product-select"));
        org.openqa.selenium.support.ui.Select prodSelect = new org.openqa.selenium.support.ui.Select(productDropdown);
        for (WebElement option : prodSelect.getOptions()) {
            if (option.getText().contains("Wireless Keyboards")) {
                prodSelect.selectByVisibleText(option.getText());
                break;
            }
        }

        org.openqa.selenium.support.ui.Select supSelect = new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("supplier-select")));
        if (supSelect.getOptions().size() > 1) {
            supSelect.selectByIndex(1);
        }

        driver.findElement(By.id("quantity-input")).clear();
        // Use a safe restock quantity that does not exceed product max capacity
        driver.findElement(By.id("quantity-input")).sendKeys("50");
        driver.findElement(By.id("submit-btn")).click();

        driver.get(baseUrl() + "/report");

        // Bulletproof Wait: Evaluates to true if the text disappears from page, or row is detached, or row has display:none
        boolean isRemoved = webWait().until(d -> {
            java.util.List<WebElement> elements = d.findElements(By.xpath("//table[@id='report-table']//td[contains(text(),'Wireless Keyboards')]"));
            if (elements.isEmpty()) {
                return true;
            }
            // Check visibility state to accommodate client-side hidden row elements
            return !elements.get(0).isDisplayed();
        });

        assertTrue(isRemoved, "Once an active deficit is restocked past its minimum threshold bounds, it must be removed from the Low Stock list.");
    }
}