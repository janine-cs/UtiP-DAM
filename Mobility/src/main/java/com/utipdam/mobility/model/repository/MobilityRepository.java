package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.Mobility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MobilityRepository extends JpaRepository<Mobility, UUID> {
    List<Mobility> findAll();
    Optional<Mobility> findById(@Param("id") UUID id);
}
