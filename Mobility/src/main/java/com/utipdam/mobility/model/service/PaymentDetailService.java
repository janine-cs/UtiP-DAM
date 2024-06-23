package com.utipdam.mobility.model.service;


import com.utipdam.mobility.model.entity.PaymentDetail;
import com.utipdam.mobility.model.repository.PaymentDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


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
    public List<PaymentDetail> findAllByUserId(Long userId) {
        return paymentDetailRepository.findAllByUserId(userId);
    }

    public List<PaymentDetail> findAllByUserIdAndPaymentSource(Long userId, String paymentSource) {
        return paymentDetailRepository.findAllByUserIdAndPaymentSource(userId, paymentSource);
    }

    public PaymentDetail findByOrderId(Integer orderId) {
        return paymentDetailRepository.findByOrderId(orderId);
    }
    public PaymentDetail save(PaymentDetail paymentDetail) {
        return paymentDetailRepository.save(paymentDetail);
    }

    public boolean validateApiKey(UUID requestApiKey) {
        //this is a simplistic implementation. Prod
        //implementation will check for expired key and other business logic
        var optionalClientCred = paymentDetailRepository.findByDatasetActivationKey(requestApiKey);
        return optionalClientCred.isPresent();
    }
    public void delete(Integer id) {
        paymentDetailRepository.deleteById(id);
    }
}
