package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Integer> {
    List<Dataset> findAll();
    Optional<Dataset> findById(@Param("id") Integer id);
    Dataset findByName(@Param("name") String name);
}
