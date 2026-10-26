package com.canchasya.api.service;

import com.canchasya.api.exception.ApiException;
import com.canchasya.api.model.Publicacion;
import com.canchasya.api.model.User;
import com.canchasya.api.repository.AmistadRepository;
import com.canchasya.api.repository.ComentarioRepository;
import com.canchasya.api.repository.LikeRepository;
import com.canchasya.api.repository.PublicacionRepository;
import com.canchasya.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceEliminacionVideoTest {

    @Mock PublicacionRepository publicacionRepository;
    @Mock ComentarioRepository comentarioRepository;
    @Mock LikeRepository likeRepository;
    @Mock UserRepository userRepository;
    @Mock AmistadService amistadService;
    @Mock AmistadRepository amistadRepository;
    @Mock NotificacionService notificacionService;
    @Mock VideoStorageService videoStorageService;
    @InjectMocks PublicacionService servicio;

    private User duenio;
    private Publicacion publicacion;

    @BeforeEach
    void preparar() {
        TransactionSynchronizationManager.initSynchronization();
        duenio = new User();
        duenio.setId(7L);
        publicacion = new Publicacion();
        publicacion.setId(11L);
        publicacion.setUser(duenio);
        when(publicacionRepository.findById(11L)).thenReturn(Optional.of(publicacion));
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void mp4SeEliminaSoloDespuesDelCommit() {
        publicacion.setVideoPublicId("c317b7d9-d471-4582-811d-274114c710d0");
        servicio.eliminar(11L, duenio);

        verify(publicacionRepository).delete(publicacion);
        verify(videoStorageService, never()).eliminar(anyString());

        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, callbacks.size());
        callbacks.getFirst().afterCommit();
        verify(videoStorageService).eliminar(publicacion.getVideoPublicId());
    }

    @Test
    void rollbackNoBorraMp4() {
        publicacion.setVideoPublicId("c317b7d9-d471-4582-811d-274114c710d0");
        servicio.eliminar(11L, duenio);

        for (TransactionSynchronization callback : TransactionSynchronizationManager.getSynchronizations()) {
            callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verify(videoStorageService, never()).eliminar(anyString());
    }

    @Test
    void sinVideoNoProgramaLimpieza() {
        servicio.eliminar(11L, duenio);
        verify(publicacionRepository).delete(publicacion);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(videoStorageService);
    }

    @Test
    void otroUsuarioNoPuedeBorrarLaPublicacionNiElVideo() {
        publicacion.setVideoPublicId("c317b7d9-d471-4582-811d-274114c710d0");
        User intruso = new User();
        intruso.setId(8L);

        assertThrows(ApiException.class, () -> servicio.eliminar(11L, intruso));
        verify(publicacionRepository, never()).delete(any(Publicacion.class));
        verifyNoInteractions(videoStorageService);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void errorAlBorrarArchivoNoRevierteElCommitYaConfirmado() {
        publicacion.setVideoPublicId("c317b7d9-d471-4582-811d-274114c710d0");
        servicio.eliminar(11L, duenio);
        doThrow(new IllegalStateException("Sin acceso al disco"))
                .when(videoStorageService).eliminar(publicacion.getVideoPublicId());
        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();

        assertDoesNotThrow(() -> callbacks.getFirst().afterCommit());
        verify(publicacionRepository).delete(publicacion);
    }
}
