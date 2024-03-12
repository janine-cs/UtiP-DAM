package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Organization;
import com.utipdam.mobility.model.repository.DatasetRepository;
import com.utipdam.mobility.model.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization findByName(String name) {
        return organizationRepository.findByName(name);
    }

    public Optional<Organization> findById(Integer id) {
        return organizationRepository.findById(id);
    }

    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }
}
