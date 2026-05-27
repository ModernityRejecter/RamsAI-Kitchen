package com.ramsai.kitchen.models.entities;

import com.ramsai.kitchen.enums.TableStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_tables")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TableStatus status = TableStatus.FREE;

    @Column(name = "x_pos")
    private Integer xpos;

    @Column(name = "y_pos")
    private Integer ypos;

    @Column(name = "occupied_by_user_id")
    private Long occupiedByUserId;

    @Column(name = "occupied_at")
    private LocalDateTime occupiedAt;
}
