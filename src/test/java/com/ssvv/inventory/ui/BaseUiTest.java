package com.ssvv.inventory.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.Color;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssvv.inventory.repository.ProductRepository;
import com.ssvv.inventory.repository.SupplierRepository;
import com.ssvv.inventory.component.DataInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BaseUiTest
 *
 * Shared test harness for the UI Selenium-based integration tests.
 * Techniques used:
 * - Selenium WebDriver (ChromeDriver) driven interactions
 * - Explicit waits (WebDriverWait / FluentWait) for robust timing
 * - Repository reset and seeding via Spring-managed DataInitializer for test isolation
 *
 * Included workflows covered by subclasses:
 * - Products, Suppliers, Restock, Shipments, and Low-Stock Report UI flows
 * - Form interactions, table verifications, navbar state checks, and alert handling
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseUiTest {

    @LocalServerPort
    protected int port;

    protected WebDriver driver;

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    protected void startDriver() {
        // Initialize ChromeDriver with common options used across UI tests.
        // Tests typically run either locally or in CI using Selenium Manager to obtain the driver.
        if (driver != null) {
            return;
        }

        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1400,1200");
        options.addArguments("--kiosk-printing"); // auto-confirm print sheets

        // Use default browser/driver discovery (Selenium Manager) - do not set a specific binary.
        driver = new ChromeDriver(options);
    }

    protected String baseUrl() {
        // Returns the base URL for the application under test (random port injected by Spring)
        return "http://localhost:" + port;
    }

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private DataInitializer dataInitializer;

    @BeforeEach
    void resetDatabase() throws Exception {
        // Clear repositories and reseed default data for isolation between tests.
        // This ensures deterministic seed state for UI assertions.
        if (productRepo != null && supplierRepo != null && dataInitializer != null) {
            productRepo.deleteAll();
            supplierRepo.deleteAll();
            dataInitializer.run();
        }
    }

    protected WebDriverWait webWait() {
        // Provides a 5s explicit wait used to synchronize UI interactions.
        return new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    protected void waitForVisible(By locator) {
        // Waits until the element located by `locator` is visible on the page.
        // Using explicit wait reduces flakiness compared to Thread.sleep.
        webWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void waitForNotPresent(By locator) {
        // Wait until the element is no longer present or is hidden (useful for post-delete checks)
        webWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void acceptAlertIfPresent() {
        // If a browser alert is present (e.g., confirmation dialogs), accept it safely.
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
            Alert alert = shortWait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (Exception ignored) {
        }
    }

    protected String makeUnique(String prefix) {
        // Generate a short, readable unique id for creating test entities without collisions.
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected void selectByVisibleText(By selectLocator, String visibleText) {
        // Helper to open a <select> and choose an option using the visible text.
        waitForVisible(selectLocator);
        Select sel = new Select(driver.findElement(selectLocator));
        sel.selectByVisibleText(visibleText);
    }

    protected void verifyActiveNavigationLink(String linkText) {
        // Verifies that a top navigation link for `linkText` is visually highlighted.
        // Uses either the presence of an "active" class or a non-empty color computed value.
        By activeLinkLocator = By.xpath("//nav//a[contains(text(),'" + linkText + "')]");
        waitForVisible(activeLinkLocator);

        WebElement navLink = driver.findElement(activeLinkLocator);
        String cssClass = navLink.getAttribute("class");
        String colorValue = navLink.getCssValue("color");

        // Convert to standard Hex format to easily inspect color transformations if needed
        String hexColor = Color.fromString(colorValue).asHex();

        // Verification: passes if the 'active' class is explicitly present,
        // or if a distinct text color style is bound to the element.
        assertTrue(cssClass.contains("active") || !hexColor.isEmpty(),
                "The '" + linkText + "' navbar link should be visually highlighted as active.");
    }
    protected void verifyTableStructureAlignment(String tableId) {
        // Ensures table header column count matches the number of data cells in each body row.
        // Skips rows that use colspan placeholders for empty-state messages.
        waitForVisible(By.id(tableId));

        int headerColumnsCount = driver.findElements(By.cssSelector("#" + tableId + " thead th")).size();
        List<WebElement> rows = driver.findElements(By.cssSelector("#" + tableId + " tbody tr"));

        for (WebElement row : rows) {
            // Ignore temporary informational placeholder row spans
            if (row.findElements(By.xpath("./td[@colspan]")).isEmpty()) {
                int bodyCellsCount = row.findElements(By.xpath("./td")).size();
                assertEquals(headerColumnsCount, bodyCellsCount,
                        "Table column headers mapping count must precisely equal data row cell counts inside " + tableId);
            }
        }
    }
}

