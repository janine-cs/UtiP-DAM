package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.DatasetDefinitionDTO;
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
import java.util.stream.Stream;

@RestController
public class DatasetController {
    private static final Logger logger = LoggerFactory.getLogger(DatasetController.class);

    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @Autowired
    private DatasetBusiness datasetBusiness;


    @GetMapping("/datasets")
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam(required = false) Boolean publish) {
        Map<String, Object> response = new HashMap<>();

        Stream<DatasetResponseDTO> data = datasetDefinitionBusiness.getAll().stream().
                map(d -> new DatasetResponseDTO(d.getName(),
                        d.getDescription(), d.getCountryCode(), d.getCity(),
                        d.getFee(), d.getPublish(),
                        d.getOrganization(), d.getId(),
                        d.getUpdatedOn(), null, datasetBusiness.getAllByDatasetDefinitionId(d.getId()))

                );

        if (publish == null) {
            response.put("data", data
                    .collect(Collectors.toList()));
        } else {
            if (publish) {
                response.put("data", data
                        .filter(dt -> dt.getPublish() != null && dt.getPublish())
                        .collect(Collectors.toList()));
            } else {
                response.put("data", data
                        .filter(dt -> dt.getPublish() != null && !dt.getPublish())
                        .collect(Collectors.toList()));

            }


        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/dataset/{datasetDefinitionId}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID datasetDefinitionId) {
        Map<String, Object> response = new HashMap<>();
        Optional<DatasetDefinition> opt = datasetDefinitionBusiness.getById(datasetDefinitionId);
        if (opt.isPresent()) {
            DatasetDefinition d = opt.get();
            response.put("data", new DatasetResponseDTO(d.getName(),
                    d.getDescription(), d.getCountryCode(), d.getCity(),
                    d.getFee(), d.getPublish(),
                    d.getOrganization(), d.getId(),
                    d.getUpdatedOn(), null, datasetBusiness.getAllByDatasetDefinitionId(d.getId())));
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

    }

    @GetMapping("/datasetDefinitions")
    public ResponseEntity<Map<String, Object>> getAllDatasetDefinitions(@RequestParam(required = false) Boolean internal) {
        Map<String, Object> response = new HashMap<>();

        List<DatasetDefinition> data = datasetDefinitionBusiness.getAll();
        if (internal == null) {
            response.put("data", data);
        } else {
            if (internal) {
                response.put("data", data.stream()
                        .filter(dt -> dt.getInternal() != null && dt.getInternal())
                        .collect(Collectors.toList()));
            } else {
                response.put("data", data.stream()
                        .filter(dt -> dt.getInternal() != null && !dt.getInternal())
                        .collect(Collectors.toList()));
            }
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/datasetDefinition/{id}")
    public ResponseEntity<Map<String, Object>> getDefinitionById(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", datasetDefinitionBusiness.getById(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/datasetDefinition")
    public ResponseEntity<Map<String, Object>> save(@RequestBody DatasetDefinitionDTO dataset) {
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

    @PutMapping("/datasetDefinition/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody DatasetDefinitionDTO dataset) throws DefaultException {
        Map<String, Object> response = new HashMap<>();
        response.put("data", datasetDefinitionBusiness.update(id, dataset));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/dataset")
    public ResponseEntity<Map<String, Object>> dataset(@RequestBody DatasetDTO datasetDTO) {
        Map<String, Object> response = new HashMap<>();

        Dataset d = datasetBusiness.getByDatasetDefinitionIdAndStartDate(datasetDTO.getDatasetDefinitionId(), datasetDTO.getStartDate());
        if (d == null) {
            Optional<Dataset> datasetIdCheck = datasetBusiness.getById(datasetDTO.getId());
            if (datasetIdCheck.isEmpty()) {

                Optional<DatasetDefinition> datasetDefinition = datasetDefinitionBusiness.getById(datasetDTO.getDatasetDefinitionId());
                if (datasetDefinition.isPresent()) {
                    Dataset dataset = new Dataset();
                    dataset.setId(datasetDTO.getId());
                    dataset.setDatasetDefinition(datasetDefinition.get());
                    dataset.setStartDate(datasetDTO.getStartDate());
                    dataset.setEndDate(datasetDTO.getEndDate());
                    dataset.setResolution(datasetDTO.getResolution());
                    dataset.setK(datasetDTO.getK());
                    dataset.setDataPoints(datasetDTO.getDataPoints());
                    response.put("data", datasetBusiness.save(dataset));
                } else {
                    logger.error("Dataset definition not found");
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } else {
                logger.error("Dataset already exists");
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

        } else {
            logger.error("Dataset already exists");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/dataset/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody DatasetDTO dataset) throws DefaultException {
        Map<String, Object> response = new HashMap<>();
        response.put("data", datasetBusiness.update(id, dataset));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}