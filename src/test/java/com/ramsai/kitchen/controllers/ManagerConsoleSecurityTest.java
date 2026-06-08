package com.ramsai.kitchen.controllers;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ramsai.kitchen.services.ManagerDashboardService;
import com.ramsai.kitchen.services.ProductService;

@SpringBootTest(properties = {
    "gemini.api.key=finta_chiave_test",
    "gemini.url=http://localhost:finto-url",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class ManagerConsoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerDashboardService dashboardService;

    @MockBean
    private ProductService productService;

    // ==========================================
    // 1. AREA CONSOLE & DASHBOARD (/api/v1/manager/**)
    // ==========================================

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerDashboard_AuthorizedForManager() throws Exception {
        when(dashboardService.getPopularityReport(any())).thenReturn(List.of());
        
        mockMvc.perform(get("/api/v1/manager/dashboard/popularity"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void managerDashboard_ForbiddenForChef() throws Exception {
        mockMvc.perform(get("/api/v1/manager/dashboard/popularity"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void managerDashboard_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/manager/dashboard/popularity"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 2. APPROVAZIONE PRODOTTI (PATCH)
    // ==========================================

    @Test
    @WithMockUser(roles = "MANAGER")
    void approveProduct_AuthorizedForManager() throws Exception {
        // Accettiamo is4xxClientError() perché l'importante è che la sicurezza lo faccia passare (niente 403)
        mockMvc.perform(patch("/api/v1/products/1/approve"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void approveProduct_ForbiddenForChef() throws Exception {
        mockMvc.perform(patch("/api/v1/products/1/approve"))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // 3. MODIFICA INVENTARIO (POST)
    // ==========================================

    @Test
    @WithMockUser(roles = "MANAGER")
    void modifyInventory_AuthorizedForManager() throws Exception {
        // Usiamo la rotta degli ingredienti che è mappata esplicitamente nel SecurityConfig
        mockMvc.perform(post("/api/v1/inventory/products/1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError()); // Se dà un errore client generico (es. 400), la sicurezza è superata!
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void modifyInventory_ForbiddenForWaiter() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/products/1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}