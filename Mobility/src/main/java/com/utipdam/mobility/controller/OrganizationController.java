package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.OrganizationBusiness;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.entity.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class OrganizationController {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationController.class);

    @Autowired
    private OrganizationBusiness organizationBusiness;

    @GetMapping("organizations")
    public ResponseEntity<Map<String, Object>> getAllOrganizations(@RequestParam(required = false) String name) {
        Map<String, Object> response = new HashMap<>();
        if (name == null){
            response.put("data", organizationBusiness.getAll());
        }else{
            response.put("data", organizationBusiness.getByName(name));
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("organization/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", organizationBusiness.getById(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("organization")
    public ResponseEntity<Map<String, Object>> save(@RequestBody Organization organization) {
        HttpHeaders responseHeaders = new HttpHeaders();
        if (organization.getName() == null) {
            logger.error("Name is required");
            responseHeaders.set("error", "Name is required");
            return ResponseEntity.badRequest()
                    .headers(responseHeaders).body(null);
        }

        Map<String, Object> response = new HashMap<>();
        Organization org = organizationBusiness.getByName(organization.getName());

        if (org == null) {
            response.put("data", organizationBusiness.save(organization));
        } else {
            logger.error("Name already exists. Please choose another.");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("organization/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id,
                                                      @RequestBody Organization organization) throws DefaultException {
        Map<String, Object> response = new HashMap<>();
        response.put("data", organizationBusiness.update(id, organization));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}