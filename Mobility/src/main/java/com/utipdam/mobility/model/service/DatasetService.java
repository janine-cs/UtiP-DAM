package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class DatasetService {
    private final DatasetRepository datasetRepository;

    @Autowired
    public DatasetService(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    public Dataset findByName(String name) {
        return datasetRepository.findByName(name);
    }

    public Optional<Dataset> findById(UUID id) {
        return datasetRepository.findById(id);
    }

    public List<Dataset> findAll() {
        return datasetRepository.findAll();
    }

    public Dataset save(Dataset dataset) {
        return datasetRepository.save(dataset);
    }
}
