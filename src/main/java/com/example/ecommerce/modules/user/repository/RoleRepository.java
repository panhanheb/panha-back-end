package com.example.ecommerce.modules.user.repository;

import com.example.ecommerce.modules.user.entity.Role;
import com.example.ecommerce.common.security.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
