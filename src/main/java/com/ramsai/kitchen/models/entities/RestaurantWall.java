package com.ramsai.kitchen.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "restaurant_walls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantWall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "x_pos")
    private Integer xpos;

    @Column(name = "y_pos")
    private Integer ypos;
}
