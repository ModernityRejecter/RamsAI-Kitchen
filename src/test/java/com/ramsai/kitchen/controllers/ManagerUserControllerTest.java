package com.ramsai.kitchen.controllers;

import com.ramsai.kitchen.models.dtos.UserResponse;
import com.ramsai.kitchen.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "gemini.api.key=finta_chiave_test",
    "gemini.url=http://localhost:finto-url",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
@AutoConfigureMockMvc
class ManagerUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAllUsers_AuthorizedForManager() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                new UserResponse(1L, "waiter1", "waiter1@example.com", "WAITER", true),
                new UserResponse(2L, "chef1", "chef1@example.com", "CHEF", true)
        ));

        mockMvc.perform(get("/api/v1/manager/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("waiter1"))
                .andExpect(jsonPath("$.data[1].role").value("CHEF"));
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void getAllUsers_ForbiddenForChef() throws Exception {
        mockMvc.perform(get("/api/v1/manager/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateUserRole_AuthorizedForManager() throws Exception {
        doNothing().when(userService).updateUserRole(eq(1L), eq("CHEF"), any());

        mockMvc.perform(put("/api/v1/manager/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CHEF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"));
    }

    @Test
    @WithMockUser(roles = "CHEF")
    void updateUserRole_ForbiddenForChef() throws Exception {
        mockMvc.perform(put("/api/v1/manager/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CHEF\"}"))
                .andExpect(status().isForbidden());
    }
}
