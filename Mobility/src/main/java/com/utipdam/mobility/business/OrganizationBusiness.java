package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Organization;
import com.utipdam.mobility.model.service.DatasetService;
import com.utipdam.mobility.model.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;


@BusinessService
public class OrganizationBusiness {
    @Autowired
    private OrganizationService organizationService;

    public List<Organization> getAll() {
        return organizationService.findAll();
    }

    public Organization getByName(String name) {
        return organizationService.findByName(name);
    }

    public Optional<Organization> getById(Integer id) {
        return organizationService.findById(id);
    }

    public Organization save(Organization organization){
        return organizationService.save(organization);
    }

    public Organization update(Integer id, Organization organization) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<Organization> ds = organizationService.findById(id);
        if (ds.isPresent()){
            ds.get().update(organization);
            return organizationService.save(ds.get());
        }else{
            return null;
        }
    }

}
