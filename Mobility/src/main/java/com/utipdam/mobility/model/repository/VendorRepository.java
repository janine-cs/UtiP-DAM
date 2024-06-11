package com.utipdam.mobility.model.repository;


import com.utipdam.mobility.model.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    List<Vendor> findAll();
    Vendor findByAccountNo(@Param("accountNo") String accountNo);

    Vendor findByAccountName(@Param("accountName") String accountName);

    Vendor findByUserId(@Param("userId") Long userId);
}