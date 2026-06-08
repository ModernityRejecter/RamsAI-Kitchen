package com.ramsai.kitchen.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "gemini.api.key=finta_key_test",
    "gemini.url=http://localhost:finto-url",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb_e2e;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class BasicSmokeEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerSmokeFlow() throws Exception {
        mockMvc.perform(get("/api/v1/tables/map"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterSmokeFlow() throws Exception {
        mockMvc.perform(get("/api/v1/walls"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void chefSmokeFlow() throws Exception {
        mockMvc.perform(get("/api/v1/products/manage"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerSmokeFlow() throws Exception {
        // Il manager tenta di cancellare un ingrediente inesistente (999)
        mockMvc.perform(delete("/api/v1/inventory/ingredients/999"))
                .andExpect(status().isNotFound()); // Ritorna 404 Not Found, che prova il superamento della security!
    }
}
