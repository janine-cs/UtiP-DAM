package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.DatasetDefinition;
import com.utipdam.mobility.model.repository.DatasetDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class DatasetDefinitionService {
    private final DatasetDefinitionRepository datasetDefinitionRepository;

    @Autowired
    public DatasetDefinitionService(DatasetDefinitionRepository datasetDefinitionRepository) {
        this.datasetDefinitionRepository = datasetDefinitionRepository;
    }

    public DatasetDefinition findByName(String name) {
        return datasetDefinitionRepository.findByName(name);
    }

    public Optional<DatasetDefinition> findById(UUID id) {
        return datasetDefinitionRepository.findById(id);
    }

    public List<DatasetDefinition> findAll() {
        return datasetDefinitionRepository.findAll();
    }

    public DatasetDefinition save(DatasetDefinition datasetDefinition) {
        return datasetDefinitionRepository.save(datasetDefinition);
    }
}
