package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.PaymentDetail;
import com.utipdam.mobility.model.repository.PaymentDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class PaymentDetailService {
    private final PaymentDetailRepository paymentDetailRepository;

    @Autowired
    public PaymentDetailService(PaymentDetailRepository paymentDetailRepository) {
        this.paymentDetailRepository = paymentDetailRepository;
    }

    public Optional<PaymentDetail> findById(Integer id) {
        return paymentDetailRepository.findById(id);
    }
    public PaymentDetail findByUserId(Long userId) {
        return paymentDetailRepository.findByUserId(userId);
    }

    public PaymentDetail findByOrderId(Integer orderId) {
        return paymentDetailRepository.findByOrderId(orderId);
    }
    public PaymentDetail save(PaymentDetail paymentDetail) {
        return paymentDetailRepository.save(paymentDetail);
    }

    public void delete(Integer id) {
        paymentDetailRepository.deleteById(id);
    }
}
