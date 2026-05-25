package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.TableResponse;
import com.ramsai.kitchen.models.dtos.UpdateTablePositionRequest;
import com.ramsai.kitchen.services.TableService;
import com.ramsai.kitchen.enums.TableStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TableService tableService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateTablePosition_Success() throws Exception {
        UpdateTablePositionRequest request = new UpdateTablePositionRequest(5, 5);
        TableResponse response = new TableResponse(1L, 1, TableStatus.FREE, 5, 5, null);

        when(tableService.updateTablePosition(eq(1L), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(put("/api/v1/tables/1/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Table position updated successfully"))
                .andExpect(jsonPath("$.data.xPos").value(5))
                .andExpect(jsonPath("$.data.yPos").value(5));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void updateTablePosition_WaiterSuccess() throws Exception {
        UpdateTablePositionRequest request = new UpdateTablePositionRequest(5, 5);
        TableResponse response = new TableResponse(1L, 1, TableStatus.FREE, 5, 5, null);

        when(tableService.updateTablePosition(eq(1L), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(put("/api/v1/tables/1/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateTablePosition_CustomerForbidden() throws Exception {
        UpdateTablePositionRequest request = new UpdateTablePositionRequest(5, 5);

        mockMvc.perform(put("/api/v1/tables/1/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
