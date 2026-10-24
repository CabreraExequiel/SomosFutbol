package com.canchasya.api.repository;

import com.canchasya.api.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Bloquea el usuario durante la vinculación para que dos solicitudes simultáneas
    // no sobrescriban distintas identidades de Google. Compatible con MariaDB local.
    @Query(value = "SELECT * FROM users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSub(String googleSub);
    boolean existsByEmail(String email);

    @Query("""
            select u from User u
            where u.id <> :userId
              and (lower(u.name) like lower(concat('%', :query, '%'))
                   or lower(u.email) like lower(concat('%', :query, '%')))
            order by u.name asc
            """)
    List<User> buscarParaComunidad(
            @Param("userId") Long userId,
            @Param("query") String query,
            Pageable pageable
    );
}
