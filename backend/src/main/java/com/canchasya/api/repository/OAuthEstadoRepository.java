package com.canchasya.api.repository;
import com.canchasya.api.model.OAuthEstado;
import org.springframework.data.jpa.repository.*;
import java.time.Instant;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface OAuthEstadoRepository extends JpaRepository<OAuthEstado,String> {
 @Query(value="SELECT e.* FROM oauth_estados e WHERE e.hash=:hash FOR UPDATE", nativeQuery=true) Optional<OAuthEstado> bloquear(@Param("hash") String hash);
 void deleteByExpiresAtBefore(Instant ahora);
}
