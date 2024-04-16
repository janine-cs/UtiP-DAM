package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Integer> {
    List<PaymentDetail> findAll();
    Optional<PaymentDetail> findById(@Param("id") Integer id);

    PaymentDetail findByOrderId(@Param("orderId") Integer orderId);
}
