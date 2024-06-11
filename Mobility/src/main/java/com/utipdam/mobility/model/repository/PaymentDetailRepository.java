package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Integer> {
    List<PaymentDetail> findAll();
    Optional<PaymentDetail> findById(@Param("id") Integer id);
    @Query(nativeQuery = true, value = "SELECT p.* FROM payment_detail p INNER JOIN order_detail o ON p.order_id = o.id WHERE o.user_id = :userId")
    PaymentDetail findByUserId(@Param("userId") Long userId);

    PaymentDetail findByOrderId(@Param("orderId") Integer orderId);
}
