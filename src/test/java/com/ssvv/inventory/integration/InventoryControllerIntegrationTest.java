package com.ssvv.inventory.integration;

import com.ssvv.inventory.entity.Product;
import com.ssvv.inventory.entity.Supplier;
import com.ssvv.inventory.repository.ProductRepository;
import com.ssvv.inventory.repository.SupplierRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;
import com.ssvv.inventory.service.InventoryService;
import com.ssvv.inventory.entity.Shipment;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryControllerIntegrationTest
 *
 * Integration tests that exercise the Spring MVC controller layer using a lightweight "driver":
 * TestRestTemplate. The repositories are replaced with stubs (@MockBean) so we control
 * data returned to the controller while still running inside a SpringBootTest with an
 * embedded web environment. This pattern is often called an "interaction-style" integration
 * test: wiring and HTTP transport are exercised while external collaborators are stubbed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InventoryControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private InventoryService inventoryService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ProductRepository productRepositoryMock() {
            return Mockito.mock(ProductRepository.class);
        }

        @Bean
        @Primary
        public SupplierRepository supplierRepositoryMock() {
            return Mockito.mock(SupplierRepository.class);
        }

        @Bean
        @Primary
        public InventoryService inventoryServiceMock() {
            return Mockito.mock(InventoryService.class);
        }
    }


    @Test
    @DisplayName("GET /restock returns page containing stubbed products and suppliers")
    void testGetRestockPageContainsStubbedData() {
        // Arrange - prepare stubbed repository responses
        Product p1 = new Product(); p1.setId(1L); p1.setName("Widget-A");
        Product p2 = new Product(); p2.setId(2L); p2.setName("Widget-B");
        Supplier s1 = new Supplier(); s1.setId(10L); s1.setName("Acme Supplies");

        when(productRepo.findAll()).thenReturn(Arrays.asList(p1, p2));
        when(supplierRepo.findAll()).thenReturn(List.of(s1));

        // Act - drive the application over HTTP
        String url = "http://localhost:" + port + "/restock";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

        // Assert - page returned and contains expected product and supplier names
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        String body = resp.getBody();
        assertTrue(body.contains("Widget-A") || body.contains("Widget-B"), "Page should contain at least one product name");
        assertTrue(body.contains("Acme Supplies"), "Page should contain supplier name from stub");
    }

    @Test
    @DisplayName("POST /restock success path - controller displays success message when InventoryService succeeds")
    void testPostRestockDisplaysSuccessMessage() {
        // For this test we stub repositories to provide selectable values.
        Product p = new Product(); p.setId(1L); p.setName("Gadget");
        Supplier s = new Supplier(); s.setId(2L); s.setName("BestSupplier");

        when(productRepo.findAll()).thenReturn(List.of(p));
        when(supplierRepo.findAll()).thenReturn(List.of(s));

        // Arrange InventoryService to succeed so controller sets successMessage
        when(inventoryService.processRestock(1L, 2L, 5)).thenReturn(new Shipment());

        // Execute POST as a form submission. The controller handles exceptions internally and
        // returns the same view; on success it adds a 'successMessage' attribute that ends up
        // rendered in the returned page. We assert the HTML contains that text.
        String url = "http://localhost:" + port + "/restock";
        String form = "productId=1&supplierId=2&quantity=5";

        ResponseEntity<String> resp = restTemplate.postForEntity(url + "?" + form, null, String.class);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().contains("Restock successful!"), "Controller should show success message when restock succeeds");
    }

    @Test
    @DisplayName("POST /restock error path - controller displays error message when service throws exception")
    void testPostRestockDisplaysErrorMessage() {
        Product p = new Product(); p.setId(1L); p.setName("Gadget");
        Supplier s = new Supplier(); s.setId(2L); s.setName("BestSupplier");

        when(productRepo.findAll()).thenReturn(List.of(p));
        when(supplierRepo.findAll()).thenReturn(List.of(s));

        // Force the service stub to throw an exception
        when(inventoryService.processRestock(1L, 2L, 100))
                .thenThrow(new IllegalArgumentException("Quantity exceeds maximum capacity."));

        String url = "http://localhost:" + port + "/restock";
        String form = "productId=1&supplierId=2&quantity=100";

        ResponseEntity<String> resp = restTemplate.postForEntity(url + "?" + form, null, String.class);

        // The controller should handle it and return a 200 OK view showing the error
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());

        // Adjust this string to match the error attribute/binding name rendered in your HTML template
        assertTrue(resp.getBody().contains("Quantity exceeds maximum capacity."),
                "Controller should display the error message on the page");
    }

    @Test
    @DisplayName("GET /restock returns empty arrays when no products or suppliers exist")
    void testGetRestockPageWhenDatabaseIsEmpty() {
        // Return empty lists from stubbed repositories
        when(productRepo.findAll()).thenReturn(List.of());
        when(supplierRepo.findAll()).thenReturn(List.of());

        String url = "http://localhost:" + port + "/restock";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());

        // Ensure standard UI elements render safely without encountering NullPointerExceptions
        assertTrue(resp.getBody().contains("Restock"), "Page title/header should still render");
    }

    @Test
    @DisplayName("POST /restock missing quantity parameter returns client error or fallback")
    void testPostRestockMissingQuantityParameter() {
        String url = "http://localhost:" + port + "/restock";
        // Malformed query: Missing the quantity entirely
        String malformedForm = "productId=1&supplierId=2";

        ResponseEntity<String> resp = restTemplate.postForEntity(url + "?" + malformedForm, null, String.class);

        // Depending on your validation config, Spring MVC will throw a MissingServletRequestParameterException.
        // This generally maps to an HTTP 400 Bad Request if unhandled, or a 200 with binding errors if caught.
        assertTrue(resp.getStatusCode().is4xxClientError() || resp.getStatusCode().is2xxSuccessful(),
                "Should gracefully handle missing web parameters");
    }
}


