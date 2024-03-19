package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDTO;
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

    public List<Dataset> getAllLatest() {
        return datasetService.findAllByOrderByStartDateDesc();
    }


    public Optional<Dataset> getById(UUID id) {
        return datasetService.findById(id);
    }

    public Dataset getByDatasetDefinitionIdAndStartDate(UUID datasetDefinitionId, String startDate) {
        return datasetService.findByDatasetDefinitionIdAndStartDate(datasetDefinitionId, startDate);
    }


    public Dataset save(Dataset dataset){
        UUID uuid = UUID.randomUUID();
        dataset.setId(uuid);
        return datasetService.save(dataset);
    }

    public Dataset update(UUID id, DatasetDTO dataset) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<Dataset> datasetOptional = datasetService.findById(id);
        if (datasetOptional.isPresent()){
            datasetOptional.get().update(dataset);
            return datasetService.save(datasetOptional.get());
        }else{
            return null;
        }
    }

}
