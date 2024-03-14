package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository extends JpaRepository<Dataset, UUID> {
    List<Dataset> findAll();
    Optional<Dataset> findById(@Param("id") UUID id);
    Dataset findByName(@Param("name") String name);
}
