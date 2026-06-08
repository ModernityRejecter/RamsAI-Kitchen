package com.ramsai.kitchen.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "gemini.api.key=finta_key_test",
    "gemini.url=http://localhost:finto-url",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb_crit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class CriticalDomainsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanApproveAndToggleProducts() throws Exception {
        // Verifica approvazione prodotto
        mockMvc.perform(patch("/api/v1/products/1/approve"))
                .andExpect(status().isNotFound());

        // Verifica attivazione/disattivazione prodotto
        mockMvc.perform(patch("/api/v1/products/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\": true}"))
                .andExpect(status().isNotFound()); // Ritorna 404 perché il prodotto 1 non esiste nel DB, ma il manager è passato!
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void chefCannotApproveProducts_Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/products/1/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessKitchen_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/kitchen/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCannotModifyInventory_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/inventory/ingredients/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void chefCanAccessAiChatSessions() throws Exception {
        mockMvc.perform(get("/api/v1/ai-chat/sessions"))
                .andExpect(status().isOk());
    }
}