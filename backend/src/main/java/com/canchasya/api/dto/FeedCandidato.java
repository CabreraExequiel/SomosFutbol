package com.canchasya.api.dto;

import java.time.LocalDateTime;

/** Datos minimos para calcular la relevancia sin cargar la entidad completa. */
public record FeedCandidato(Long id, Long autorId, String ciudadAutor,
                            LocalDateTime createdAt) {}
