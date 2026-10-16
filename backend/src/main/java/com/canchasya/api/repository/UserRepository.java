package com.canchasya.api.repository;

import com.canchasya.api.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
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
