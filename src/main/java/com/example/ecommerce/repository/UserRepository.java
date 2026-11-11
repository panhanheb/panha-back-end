package com.example.ecommerce.repository;

import com.example.ecommerce.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u where u.username = :login or lower(u.email) = lower(:login)")
    Optional<User> findByUsernameOrEmail(@Param("login") String login);

    Optional<User> findByPasswordResetTokenHash(String tokenHash);

    /** Current session version of an enabled account; empty when the account is gone or disabled. */
    @Query("select u.tokenVersion from User u where u.id = :id and u.enabled = true")
    Optional<Integer> findActiveSessionVersion(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:keyword is null
                   or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
              and (:enabled is null or u.enabled = :enabled)
            """)
    Page<User> search(@Param("keyword") String keyword, @Param("enabled") Boolean enabled, Pageable pageable);
}
