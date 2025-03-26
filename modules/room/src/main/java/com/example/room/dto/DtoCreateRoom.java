package com.example.room.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DtoCreateRoom {

    private String name;
    private int maxCapacity;
    private int duration; //seconds
}
