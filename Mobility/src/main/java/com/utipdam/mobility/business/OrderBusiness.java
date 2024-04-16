package com.utipdam.mobility.business;

import com.utipdam.mobility.config.BusinessService;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.service.*;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.Optional;


@BusinessService
public class OrderBusiness {

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private PaymentDetailService paymentDetailService;

    public OrderDetail saveOrderDetail(OrderDetail orderDetail){
        return orderDetailService.save(orderDetail);
    }

    public PaymentDetail savePaymentDetail(PaymentDetail paymentDetail){
        return paymentDetailService.save(paymentDetail);
    }

    public OrderItem saveOrderItem(OrderItem orderItem){
        return orderItemService.save(orderItem);
    }

    public OrderDetail getOrderDetailByUserId(Long userId) {
        return orderDetailService.findByUserId(userId);
    }

    public Optional<OrderDetail> getOrderDetailById(Integer orderId) {
        return orderDetailService.findById(orderId);
    }

    public Optional<PaymentDetail> getPaymentById(Integer paymentId) {
        return paymentDetailService.findById(paymentId);
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

    public void delete(Integer id) {
        orderItemService.delete(id);
    }

}
