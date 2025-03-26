package com.example.queue.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DtoJoinRoom {

    private String roomId;
    private String email;
    private String name;
}
