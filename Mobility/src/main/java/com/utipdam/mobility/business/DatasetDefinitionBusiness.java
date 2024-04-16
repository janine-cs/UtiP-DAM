package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.DatasetDefinitionDTO;
import com.utipdam.mobility.model.entity.DatasetDefinition;
import com.utipdam.mobility.model.entity.Organization;
import com.utipdam.mobility.model.entity.Server;
import com.utipdam.mobility.model.service.DatasetDefinitionService;
import com.utipdam.mobility.model.service.OrganizationService;
import com.utipdam.mobility.model.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@BusinessService
public class DatasetDefinitionBusiness {
    @Autowired
    private DatasetDefinitionService datasetDefinitionService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private ServerService serverService;


    private long DEFAULT_USER = 3; //admin
    private String DEFAULT_SERVER = "lucky";

    public List<DatasetDefinition> getAll() {
        return datasetDefinitionService.findAll();
    }

    public DatasetDefinition getByName(String name) {
        return datasetDefinitionService.findByName(name);
    }

    public Optional<DatasetDefinition> getById(UUID id) {
        return datasetDefinitionService.findById(id);
    }

    public DatasetDefinition save(DatasetDefinitionDTO dataset){
        UUID uuid = UUID.randomUUID();
        DatasetDefinition ds = new DatasetDefinition();
        ds.setId(uuid);
        ds.setName(dataset.getName());
        ds.setDescription(dataset.getDescription());
        ds.setCountryCode(dataset.getCountryCode());
        ds.setCity(dataset.getCity());
        ds.setFee(dataset.getFee());
        ds.setInternal(dataset.isInternal());
        ds.setPublish(dataset.isPublish());
        if (dataset.isInternal() && dataset.getServer() == null){
            Server server = serverService.findByName(DEFAULT_SERVER);
            ds.setServer(server);
        }
        if (dataset.getServer() != null){
            Server sv = serverService.findByName(dataset.getServer().getName());
            if (sv == null) {
                Server s = new Server(dataset.getServer().getName(), dataset.getServer().getDomain());
                ds.setServer(serverService.save(s));
            }else{
                ds.setServer(sv);
            }
        }
        if (dataset.getUserId() == null){
            ds.setUserId(DEFAULT_USER);
        }else{
            ds.setUserId(dataset.getUserId());
        }

        if (dataset.getOrganization() != null){
            Organization response = organizationService.findByNameAndEmail(dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
            if (response == null) {
                UUID orgUUID = UUID.randomUUID();
                Organization org = new Organization(orgUUID, dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
                ds.setOrganization(organizationService.save(org));
            }else{
                ds.setOrganization(response);
            }
        }
        return datasetDefinitionService.save(ds);
    }

    public DatasetDefinition update(UUID id, DatasetDefinitionDTO dataset) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<DatasetDefinition> ds = datasetDefinitionService.findById(id);
        if (ds.isPresent()){
            DatasetDefinition data = new DatasetDefinition();
            data.setId(id);
            data.setName(dataset.getName() == null ? ds.get().getName() : dataset.getName());
            data.setDescription(dataset.getDescription() == null ? ds.get().getDescription() : dataset.getDescription());
            data.setCountryCode(dataset.getCountryCode() == null ? ds.get().getCountryCode() : dataset.getCountryCode());
            data.setCity(dataset.getCity() == null ? ds.get().getCity() : dataset.getCity());
            data.setFee(dataset.getFee() == null ? ds.get().getFee() : dataset.getFee());
            if (dataset.getOrganization() == null) {
                data.setOrganization(ds.get().getOrganization());
            }else{
                Organization response = organizationService.findByNameAndEmail(dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
                if (response == null) {
                    UUID orgUUID = UUID.randomUUID();
                    Organization org = new Organization(orgUUID, dataset.getOrganization().getName(), dataset.getOrganization().getEmail());
                    data.setOrganization(organizationService.save(org));
                }else{
                    data.setOrganization(response);
                }
            }
            data.setPublish(dataset.isPublish());
            data.setInternal(dataset.isInternal());
            if (dataset.isInternal() && dataset.getServer() == null){
                Server server = serverService.findByName(DEFAULT_SERVER);
                data.setServer(server);
            }
            if (dataset.getServer() != null){
                Server sv = serverService.findByName(dataset.getServer().getName());
                if (sv == null) {
                    Server s = new Server(dataset.getServer().getName(), dataset.getServer().getDomain());
                    data.setServer(serverService.save(s));
                }else{
                    data.setServer(sv);
                }
            }
            if (dataset.getUserId() == null){
                data.setUserId(DEFAULT_USER);
            }else{
                data.setUserId(dataset.getUserId());
            }

            ds.get().update(data);
            return datasetDefinitionService.save(ds.get());
        }else{
            return null;
        }
    }

    public void delete(UUID id) {
        datasetDefinitionService.delete(id);
    }
}
