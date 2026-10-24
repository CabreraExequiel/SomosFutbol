package com.canchasya.api.repository;
import com.canchasya.api.model.MercadoPagoCuenta;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface MercadoPagoCuentaRepository extends JpaRepository<MercadoPagoCuenta,Long> {
 Optional<MercadoPagoCuenta> findByMpUserId(String id);
 @Query(value="SELECT c.* FROM mercadopago_cuentas c WHERE c.dueno_id=:id FOR UPDATE", nativeQuery=true)
 Optional<MercadoPagoCuenta> bloquear(@Param("id") Long id);
}
