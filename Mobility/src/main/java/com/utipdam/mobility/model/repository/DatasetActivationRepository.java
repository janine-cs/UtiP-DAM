package com.utipdam.mobility.model.repository;

import com.utipdam.mobility.model.entity.DatasetActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetActivationRepository extends JpaRepository<DatasetActivation, Integer> {
    List<DatasetActivation> findAll();
    Optional<DatasetActivation> findByPaymentDetailId(@Param("paymentDetailId") Integer paymentDetailId);

    List<DatasetActivation> findByUserId(@Param("userId") Long userId);

//    @Query(nativeQuery = true, value = "SELECT p.* FROM payment_detail p INNER JOIN order_detail o ON p.order_id = o.id " +
//            "WHERE o.user_id = :userId AND p.payment_source = :paymentSource order by id DESC")
//    List<DatasetActivation> findAllByUserIdAndPaymentSource(@Param("userId") Long userId, @Param("paymentSource") String paymentSource);
    DatasetActivation findByOrderItemId(@Param("orderItemId") Integer orderItemId);

    Optional<DatasetActivation> findByApiKey(@Param("apiKey") UUID apiKey);
}
