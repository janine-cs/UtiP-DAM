package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@BusinessService
public class DatasetBusiness {
    @Autowired
    private DatasetService datasetService;


    public List<Dataset> getAll() {
        return datasetService.findAll();
    }

    public List<Dataset> getAllLatest() {
        return datasetService.findAllByOrderByStartDateDesc();
    }

    public Dataset getByDatasetIdLatest(UUID id) {
        return datasetService.findByDatasetIdAndOrderByStartDateDesc(id);
    }

    public Optional<Dataset> getById(UUID id) {
        return datasetService.findById(id);
    }

    public Dataset save(Dataset dataset){
        UUID uuid = UUID.randomUUID();
        dataset.setId(uuid);
        return datasetService.save(dataset);
    }

    public Dataset update(UUID id, Dataset mobility) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<Dataset> mobilityOptional = datasetService.findById(id);
        if (mobilityOptional.isPresent()){
            mobilityOptional.get().update(mobility);
            return datasetService.save(mobilityOptional.get());
        }else{
            return null;
        }
    }

}
