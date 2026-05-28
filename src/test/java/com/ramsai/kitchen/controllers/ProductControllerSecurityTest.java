package com.ramsai.kitchen.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ramsai.kitchen.models.dtos.ProductCreateRequest;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    void createProduct_ManagerSuccess() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Margherita Pizza",
                "Classic pizza",
                BigDecimal.valueOf(35),
                1L,
                false,
                false,
                null
        );

        ProductResponse response = new ProductResponse(
                10L, "Margherita Pizza", "Classic pizza", BigDecimal.valueOf(35),
                false, false, null, 0.0, "Pizza", 1L, "APPROVED", true, "None"
        );

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.name").value("Margherita Pizza"));
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void createProduct_ChefSuccess() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Margherita Pizza",
                "Classic pizza",
                BigDecimal.valueOf(35),
                1L,
                false,
                false,
                null
        );

        ProductResponse response = new ProductResponse(
                10L, "Margherita Pizza", "Classic pizza", BigDecimal.valueOf(35),
                false, false, null, 0.0, "Pizza", 1L, "APPROVED", true, "None"
        );

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void getAllProductsForManagement_ChefSuccess() throws Exception {
        when(productService.getAllProductsForManagement()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products/manage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }
}
