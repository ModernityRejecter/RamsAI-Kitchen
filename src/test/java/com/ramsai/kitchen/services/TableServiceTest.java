package com.ramsai.kitchen.services;

import com.ramsai.kitchen.models.entities.RestaurantTable;
import com.ramsai.kitchen.repositories.OrderRepository;
import com.ramsai.kitchen.repositories.RestaurantTableRepository;
import com.ramsai.kitchen.models.dtos.TableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

    @Mock
    private RestaurantTableRepository tableRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private TableService tableService;

    private RestaurantTable table;

    @BeforeEach
    void setUp() {
        table = new RestaurantTable();
        table.setId(1L);
        table.setTableNumber(1);
        table.setXpos(0);
        table.setYpos(0);
    }

    @Test
    void updateTablePosition_Success() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(tableRepository.findByXposAndYpos(5, 5)).thenReturn(Optional.empty());
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

        TableResponse response = tableService.updateTablePosition(1L, 5, 5);

        assertNotNull(response);
        assertEquals(5, table.getXpos());
        assertEquals(5, table.getYpos());
        verify(tableRepository).save(table);
    }

    @Test
    void updateTablePosition_OverlapThrowsException() {
        RestaurantTable otherTable = new RestaurantTable();
        otherTable.setId(2L);

        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(tableRepository.findByXposAndYpos(5, 5)).thenReturn(Optional.of(otherTable));

        assertThrows(RuntimeException.class, () -> tableService.updateTablePosition(1L, 5, 5));
        verify(tableRepository, never()).save(any());
    }

    @Test
    void updateTablePosition_SameTableOverlapSuccess() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(tableRepository.findByXposAndYpos(0, 0)).thenReturn(Optional.of(table));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(table);

        TableResponse response = tableService.updateTablePosition(1L, 0, 0);

        assertNotNull(response);
        verify(tableRepository).save(table);
    }
}
