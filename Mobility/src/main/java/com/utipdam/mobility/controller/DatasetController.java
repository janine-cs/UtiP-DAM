package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.entity.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class DatasetController {
    private static final Logger logger = LoggerFactory.getLogger(DatasetController.class);

    @Autowired
    private DatasetBusiness datasetBusiness;


    @GetMapping("datasets")
    public ResponseEntity<Map<String, Object>> getAllOrganizations(@RequestParam(required = false) String name) {
        Map<String, Object> response = new HashMap<>();
        if (name == null){
            response.put("data", datasetBusiness.getAll());
        }else{
            response.put("data", datasetBusiness.getByName(name));
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("dataset/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", datasetBusiness.getById(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("dataset")
    public ResponseEntity<Map<String, Object>> save(@RequestBody DatasetDTO dataset) {
        HttpHeaders responseHeaders = new HttpHeaders();
        if (dataset.getName() == null) {
            logger.error("Name is required");
            responseHeaders.set("error", "Name is required");
            return ResponseEntity.badRequest()
                    .headers(responseHeaders).body(null);
        }

        Map<String, Object> response = new HashMap<>();
        Dataset ds = datasetBusiness.getByName(dataset.getName());

        if (ds == null) {
            response.put("data", datasetBusiness.save(dataset));
        } else {
            logger.error("Name already exists. Please choose another.");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("dataset/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody DatasetDTO dataset) throws DefaultException {
        Map<String, Object> response = new HashMap<>();
        response.put("data", datasetBusiness.update(id, dataset));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}