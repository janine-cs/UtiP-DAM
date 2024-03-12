package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    List<Organization> findAll();
    Optional<Organization> findById(@Param("id") Integer id);
    Organization findByName(@Param("name") String name);
}
