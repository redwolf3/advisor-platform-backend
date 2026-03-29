package com.advisorplatform.domain.repository;

import com.advisorplatform.domain.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VisitorRepository extends JpaRepository<Visitor, UUID> {
    Optional<Visitor> findByBrowserToken(String browserToken);
    Optional<Visitor> findByEmail(String email);
}
