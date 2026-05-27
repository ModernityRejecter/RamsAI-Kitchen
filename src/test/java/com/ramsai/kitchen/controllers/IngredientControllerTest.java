package com.ramsai.kitchen.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramsai.kitchen.enums.InventoryChangeReason;
import com.ramsai.kitchen.models.dtos.IngredientResponse;
import com.ramsai.kitchen.models.dtos.IngredientUpsertRequest;
import com.ramsai.kitchen.models.dtos.StockAdjustmentRequest;
import com.ramsai.kitchen.services.IngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngredientService ingredientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    void createIngredient_ManagerRoleSuccess() throws Exception {
        IngredientUpsertRequest request = new IngredientUpsertRequest("Salt", "g", 1000.0, 100.0);
        IngredientResponse response = new IngredientResponse(1L, "Salt", "g", 1000.0, 100.0);

        when(ingredientService.create(any(IngredientUpsertRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingredient created successfully"))
                .andExpect(jsonPath("$.data.name").value("Salt"));
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void createIngredient_ChefForbidden() throws Exception {
        IngredientUpsertRequest request = new IngredientUpsertRequest("Salt", "g", 1000.0, 100.0);

        mockMvc.perform(post("/api/v1/inventory/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createIngredient_CustomerForbidden() throws Exception {
        IngredientUpsertRequest request = new IngredientUpsertRequest("Salt", "g", 1000.0, 100.0);

        mockMvc.perform(post("/api/v1/inventory/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void adjustIngredientStock_ManagerSuccess() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(50.0, InventoryChangeReason.RESTOCK);
        IngredientResponse response = new IngredientResponse(1L, "Salt", "g", 1050.0, 100.0);

        when(ingredientService.adjustStock(eq(1L), eq(50.0), eq(InventoryChangeReason.RESTOCK))).thenReturn(response);

        mockMvc.perform(put("/api/v1/inventory/ingredients/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ingredient stock adjusted successfully"));
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void adjustIngredientStock_ChefForbidden() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(50.0, InventoryChangeReason.RESTOCK);

        mockMvc.perform(put("/api/v1/inventory/ingredients/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
