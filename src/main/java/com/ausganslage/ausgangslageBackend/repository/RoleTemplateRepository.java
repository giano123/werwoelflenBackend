package com.ausganslage.ausgangslageBackend.repository;

import com.ausganslage.ausgangslageBackend.enums.RoleName;
import com.ausganslage.ausgangslageBackend.model.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, Long> {
    Optional<RoleTemplate> findByName(RoleName name);
}

