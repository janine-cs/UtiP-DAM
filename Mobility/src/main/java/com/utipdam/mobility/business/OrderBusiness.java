package com.utipdam.mobility.business;

import com.utipdam.mobility.KeyUtil;
import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.service.*;
import org.springframework.beans.factory.annotation.Autowired;


import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@BusinessService
public class OrderBusiness {

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private PaymentDetailService paymentDetailService;

    @Autowired
    private DownloadsByDayService downloadsByDayService;

    @Autowired
    private OrderItemDatasetService orderItemDatasetService;


    public OrderDetail saveOrderDetail(OrderDetail orderDetail){
        return orderDetailService.save(orderDetail);
    }

    public PaymentDetail savePaymentDetail(PaymentDetail paymentDetail){
        return paymentDetailService.save(paymentDetail);
    }

    public OrderItem saveOrderItem(OrderItem orderItem){
        return orderItemService.save(orderItem);
    }

    public OrderItemDataset saveOrderItemDataset(OrderItemDataset orderItemDataset){
        return orderItemDatasetService.save(orderItemDataset);
    }

    public OrderDetail getOrderDetailByUserId(Long userId) {
        return orderDetailService.findByUserId(userId);
    }

    public Optional<OrderDetail> getOrderDetailById(Integer orderId) {
        return orderDetailService.findById(orderId);
    }

    public List<OrderItem> getOrderItemByOrderId(Integer orderId) {
        return orderItemService.findAllByOrderId(orderId);
    }

    public List<OrderItem> getOrderItemByUserId(Long userId) {
        return orderItemService.findAllByUserId(userId);
    }

    public Optional<PaymentDetail> getPaymentById(Integer paymentId) {
        return paymentDetailService.findById(paymentId);
    }

    public List<PaymentDetail> getAllPurchasesByUserId(Long userId) {
        return paymentDetailService.findAllByUserId(userId);
    }
    public List<PaymentDetail> getAllPurchasesByUserIdAndPaymentSource(Long userId, String paymentSource) {
        return paymentDetailService.findAllByUserIdAndPaymentSource(userId, paymentSource);
    }


    public PaymentDetail getPaymentDetailByOrderId(Integer orderId) {
        return paymentDetailService.findByOrderId(orderId);
    }

//    public CartItem update(Integer id, CartItem cartItem) throws DefaultException {
//        if (id == null) {
//            throw new DefaultException("id can not be null");
//        }
//        Optional<CartItem> ds = cartItemService.findById(id);
//        if (ds.isPresent()){
//            ds.get().update(cartItem);
//            return cartItemService.save(ds.get());
//        }else{
//            return null;
//        }
//    }

    public void incrementCount(Integer id){
        downloadsByDayService.incrementCount(id);
    }

    public DownloadsByDay getByDatasetDefinitionIdAndDate(UUID datasetDefinitionId, Date date) {
        return downloadsByDayService.findByDatasetDefinitionIdAndDate(datasetDefinitionId, date);
    }

    public List<DownloadsByDay> getAllByDatasetDefinitionId(UUID datasetDefinitionId) {
        return downloadsByDayService.findByDatasetDefinitionId(datasetDefinitionId);
    }

    public void saveDownloads(DownloadsByDay downloadsByDay){
        downloadsByDayService.save(downloadsByDay);
    }

    public PaymentDetail activateLicense(Integer id, String username) {
        Optional<PaymentDetail> p = paymentDetailService.findById(id);
        if (p.isPresent()){
            PaymentDetail pDetail = p.get();
            pDetail.setDeactivate(false);
            pDetail.setDatasetActivationKey(KeyUtil.createLicenseKey(username, id.toString()));

            return paymentDetailService.save(pDetail);
        }else{
            return null;
        }

    }

    public void deactivateLicense(Integer id) {
        Optional<PaymentDetail> p = paymentDetailService.findById(id);
        if (p.isPresent()){
            PaymentDetail pDetail = p.get();
            pDetail.setDeactivate(true);
            pDetail.setModifiedAt(new Timestamp(System.currentTimeMillis()));

            paymentDetailService.save(pDetail);
        }
    }

    public void deleteInvoice(Integer id) {
        paymentDetailService.delete(id);
    }

}
