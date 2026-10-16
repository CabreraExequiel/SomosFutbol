package com.canchasya.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IniciarConversacionRequest {

    @NotNull
    private Long otroUserId;
}
