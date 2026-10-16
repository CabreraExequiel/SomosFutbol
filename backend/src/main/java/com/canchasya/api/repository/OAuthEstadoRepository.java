package com.canchasya.api.repository;
import com.canchasya.api.model.OAuthEstado;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
public interface OAuthEstadoRepository extends JpaRepository<OAuthEstado,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from OAuthEstado e where e.hash=:hash") Optional<OAuthEstado> bloquear(String hash);
 void deleteByExpiresAtBefore(Instant ahora);
}
