package com.pizzastore.controller.integration;

import com.pizzastore.config.WebSecurityConfig;
import com.pizzastore.controller.InventoryController;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Inventory;
import com.pizzastore.entity.InventoryBatch;
import com.pizzastore.entity.Product;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.InventoryBatchRepository;
import com.pizzastore.repository.InventoryRepository;
import com.pizzastore.repository.ProductRepository;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsServiceImpl;
import com.pizzastore.service.BranchAccessService;
import com.pizzastore.service.MenuAvailabilityRealtimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private InventoryRepository inventoryRepository;

    @MockBean
    private InventoryBatchRepository inventoryBatchRepository;

    @MockBean
    private BranchRepository branchRepository;

    @MockBean
    private BranchAccessService branchAccessService;

    @MockBean
    private MenuAvailabilityRealtimeService menuAvailabilityRealtimeService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @WithMockUser(roles = "CHEF")
    void getProductsShouldReturnCatalogForAuthorizedRole() throws Exception {
        Product flour = product(1L, "Flour", "kg");
        when(productRepository.findAll()).thenReturn(List.of(flour));

        mockMvc.perform(get("/api/inventory/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Flour"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void addProductShouldRejectDuplicateName() throws Exception {
        when(productRepository.existsByName("Flour")).thenReturn(true);

        mockMvc.perform(post("/api/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Flour",
                                  "unit": "kg"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteProductShouldDeleteExistingProduct() throws Exception {
        when(productRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/inventory/products/1"))
                .andExpect(status().isOk());

        verify(productRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getStockByBranchShouldAssertBranchAccess() throws Exception {
        Branch branch = branch(1L, "Branch A");
        Product flour = product(2L, "Flour", "kg");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(inventoryRepository.findByBranch(branch)).thenReturn(List.of(new Inventory(branch, flour, 10.0)));

        mockMvc.perform(get("/api/inventory/stock/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantityAvailable").value(10.0));

        verify(branchAccessService).assertCanAccessBranch("manager", branch);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void importStockShouldCreateBatchUpdateInventoryAndPublishAvailability() throws Exception {
        Branch branch = branch(1L, "Branch A");
        Product flour = product(2L, "Flour", "kg");
        Inventory inventory = new Inventory(branch, flour, 5.0);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(productRepository.findById(2L)).thenReturn(Optional.of(flour));
        when(inventoryRepository.findByBranchAndProduct(branch, flour)).thenReturn(Optional.of(inventory));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(invocation -> {
            InventoryBatch batch = invocation.getArgument(0);
            batch.setId(50L);
            return batch;
        });

        mockMvc.perform(post("/api/inventory/stock/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": 1,
                                  "productId": 2,
                                  "quantityAdded": 3.5,
                                  "importedDate": "2026-05-01",
                                  "expiredAt": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.quantityImported").value(3.5))
                .andExpect(jsonPath("$.quantityRemaining").value(3.5));

        verify(branchAccessService).assertCanAccessBranch("manager", branch);
        verify(inventoryRepository).save(inventory);
        verify(menuAvailabilityRealtimeService).publishChanged(branch);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void importStockShouldRejectNonPositiveQuantity() throws Exception {
        Branch branch = branch(1L, "Branch A");
        Product flour = product(2L, "Flour", "kg");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(productRepository.findById(2L)).thenReturn(Optional.of(flour));

        mockMvc.perform(post("/api/inventory/stock/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": 1,
                                  "productId": 2,
                                  "quantityAdded": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void importStockShouldRejectExpiredDateInPast() throws Exception {
        Branch branch = branch(1L, "Branch A");
        Product flour = product(2L, "Flour", "kg");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(productRepository.findById(2L)).thenReturn(Optional.of(flour));

        mockMvc.perform(post("/api/inventory/stock/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": 1,
                                  "productId": 2,
                                  "quantityAdded": 1,
                                  "expiredAt": "2020-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void productsEndpointShouldRejectStaffRole() throws Exception {
        mockMvc.perform(get("/api/inventory/products"))
                .andExpect(status().isForbidden());
    }

    private Branch branch(Long id, String name) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        return branch;
    }

    private Product product(Long id, String name, String unit) {
        Product product = new Product(name, unit);
        product.setId(id);
        return product;
    }
}
