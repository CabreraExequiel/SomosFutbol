package com.canchasya.api.service;

import com.canchasya.api.model.BitacoraEvento;
import com.canchasya.api.repository.BitacoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BitacoraWriter {
    private final BitacoraRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardar(BitacoraEvento evento) { repository.saveAndFlush(evento); }
}
