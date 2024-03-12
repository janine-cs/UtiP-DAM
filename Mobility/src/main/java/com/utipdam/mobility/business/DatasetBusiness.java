package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDTO;
import com.utipdam.mobility.model.entity.Dataset;
import com.utipdam.mobility.model.entity.Organization;
import com.utipdam.mobility.model.service.DatasetService;
import com.utipdam.mobility.model.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;


@BusinessService
public class DatasetBusiness {
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private OrganizationService organizationService;


    public List<Dataset> getAll() {
        return datasetService.findAll();
    }

    public Dataset getByName(String name) {
        return datasetService.findByName(name);
    }

    public Optional<Dataset> getById(Integer id) {
        return datasetService.findById(id);
    }

    public Dataset save(DatasetDTO dataset){
        Dataset ds = new Dataset();
        ds.setName(dataset.getName());
        ds.setDescription(dataset.getDescription());
        ds.setCountryCode(dataset.getCountryCode());
        ds.setFee(dataset.getFee());
        if (dataset.getOrganization() != null){
            Organization response = organizationService.findByName(dataset.getOrganization().getName());
            if (response == null) {
                Organization org = new Organization(dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
                ds.setOrganizationId(organizationService.save(org).getId());
            }else{
                ds.setOrganizationId(response.getId());
            }
        }
        return datasetService.save(ds);
    }

    public Dataset update(Integer id, DatasetDTO dataset) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<Dataset> ds = datasetService.findById(id);
        if (ds.isPresent()){
            Dataset data = new Dataset();
            data.setId(id);
            data.setName(dataset.getName() == null ? ds.get().getName() : dataset.getName());
            data.setDescription(dataset.getDescription() == null ? ds.get().getDescription() : dataset.getDescription());
            data.setCountryCode(dataset.getCountryCode() == null ? ds.get().getCountryCode() : dataset.getCountryCode());
            data.setFee(dataset.getFee() == null ? ds.get().getFee() : dataset.getFee());
            if (dataset.getOrganization() == null) {
                data.setOrganizationId(ds.get().getOrganizationId());
            }else{
                Organization response = organizationService.findByName(dataset.getOrganization().getName());
                if (response == null) {
                    Organization org = new Organization(dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
                    data.setOrganizationId(organizationService.save(org).getId());
                }else{
                    data.setOrganizationId(response.getId());
                }
            }

            ds.get().update(data);
            return datasetService.save(ds.get());
        }else{
            return null;
        }
    }

}
