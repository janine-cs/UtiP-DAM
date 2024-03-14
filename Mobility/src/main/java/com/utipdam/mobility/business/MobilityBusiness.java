package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.exception.DefaultException;
import com.utipdam.mobility.model.entity.Mobility;
import com.utipdam.mobility.model.service.MobilityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@BusinessService
public class MobilityBusiness {
    @Autowired
    private MobilityService mobilityService;


    public List<Mobility> getAll() {
        return mobilityService.findAll();
    }

    public Optional<Mobility> getById(UUID id) {
        return mobilityService.findById(id);
    }

    public Mobility save(Mobility mobility){
        UUID uuid = UUID.randomUUID();
        mobility.setId(uuid);
        return mobilityService.save(mobility);
    }

    public Mobility update(UUID id, Mobility mobility) throws DefaultException {
        if (id == null) {
            throw new DefaultException("id can not be null");
        }
        Optional<Mobility> mobilityOptional = mobilityService.findById(id);
        if (mobilityOptional.isPresent()){
            mobilityOptional.get().update(mobility);
            return mobilityService.save(mobilityOptional.get());
        }else{
            return null;
        }
    }

}
