package org.example.panhabackend.repository;

import org.example.panhabackend.entity.Role;
import org.example.panhabackend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
