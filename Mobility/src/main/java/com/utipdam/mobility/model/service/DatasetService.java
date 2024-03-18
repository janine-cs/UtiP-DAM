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

    public Optional<Dataset> findById(UUID id) {
        return datasetRepository.findById(id);
    }

    public List<Dataset> findAllByOrderByStartDateDesc() {
        return datasetRepository.findAllByOrderByStartDateDesc();
    }

    public Dataset save(Dataset organization) {
        return datasetRepository.save(organization);
    }
}
