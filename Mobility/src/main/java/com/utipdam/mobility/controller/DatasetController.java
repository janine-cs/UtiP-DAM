package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.DatasetResponseDTO;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.DatasetDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class DatasetController {
    private static final Logger logger = LoggerFactory.getLogger(DatasetController.class);

    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @Autowired
    private DatasetBusiness datasetBusiness;


    @GetMapping("datasets")
    public ResponseEntity<Map<String, Object>> getAllOrganizations() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", datasetBusiness.getAllLatest().stream().
                map(d -> new DatasetResponseDTO(d.getId(), d.getDatasetDefinition().getName(),
                         d.getDatasetDefinition().getDescription(), d.getDatasetDefinition().getCountryCode(),
                         d.getDatasetDefinition().getFee(), d.getDatasetDefinition().getPublish(),
                        d.getDatasetDefinition().getInternal(),d.getDatasetDefinition().getOrganization(), d.getDatasetDefinition().getId(),
                        d.getResolution(), d.getStartDate(), d.getEndDate(), d.getUpdatedOn(), d.getKValue(), d.getDataPoints()))
                .collect(Collectors.toList()));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("dataset/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();
        Optional<Dataset> opt = datasetBusiness.getById(id);
        if (opt.isPresent()){
            Dataset d = opt.get();
            response.put("data", new DatasetResponseDTO(d.getId(), d.getDatasetDefinition().getName(),
                    d.getDatasetDefinition().getDescription(), d.getDatasetDefinition().getCountryCode(),
                    d.getDatasetDefinition().getFee(), d.getDatasetDefinition().getPublish(),
                    d.getDatasetDefinition().getInternal(),d.getDatasetDefinition().getOrganization(), d.getDatasetDefinition().getId(),
                    d.getResolution(), d.getStartDate(), d.getEndDate(), d.getUpdatedOn(), d.getKValue(), d.getDataPoints()));
            return new ResponseEntity<>(response, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

    }


    @GetMapping("datasetDefinition/{id}")
    public ResponseEntity<Map<String, Object>> getDefinitionById(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", datasetDefinitionBusiness.getById(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @PostMapping("datasetDefinition")
    public ResponseEntity<Map<String, Object>> save(@RequestBody DatasetDTO dataset) {
        HttpHeaders responseHeaders = new HttpHeaders();
        if (dataset.getName() == null) {
            logger.error("Name is required");
            responseHeaders.set("error", "Name is required");
            return ResponseEntity.badRequest()
                    .headers(responseHeaders).body(null);
        }

        Map<String, Object> response = new HashMap<>();
        DatasetDefinition ds = datasetDefinitionBusiness.getByName(dataset.getName());

        if (ds == null) {
            response.put("data", datasetDefinitionBusiness.save(dataset));
        } else {
            logger.error("Name already exists. Please choose another.");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("datasetDefinition/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody DatasetDTO dataset) throws DefaultException {
        Map<String, Object> response = new HashMap<>();
        response.put("data", datasetDefinitionBusiness.update(id, dataset));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}