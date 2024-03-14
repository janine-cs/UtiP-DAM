package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Mobility;
import com.utipdam.mobility.model.repository.MobilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class MobilityService {
    private final MobilityRepository mobilityRepository;

    @Autowired
    public MobilityService(MobilityRepository mobilityRepository) {
        this.mobilityRepository = mobilityRepository;
    }

    public Optional<Mobility> findById(UUID id) {
        return mobilityRepository.findById(id);
    }

    public List<Mobility> findAll() {
        return mobilityRepository.findAll();
    }

    public Mobility save(Mobility organization) {
        return mobilityRepository.save(organization);
    }
}
