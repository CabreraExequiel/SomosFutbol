
        package com.canchasya.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {

    ADMINISTRADOR,
    USUARIO,
    DUENO;

    @JsonCreator
    public static Role fromString(String value) {
        return Role.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
