package com.canchasya.api.repository;
import com.canchasya.api.model.MercadoPagoCuenta;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface MercadoPagoCuentaRepository extends JpaRepository<MercadoPagoCuenta,Long> {
 Optional<MercadoPagoCuenta> findByMpUserId(String id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from MercadoPagoCuenta c where c.duenoId=:id")
 Optional<MercadoPagoCuenta> bloquear(Long id);
}
