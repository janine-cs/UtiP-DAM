package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.OrderBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.model.OrderDTO;
import com.utipdam.mobility.model.PaymentDTO;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderBusiness orderBusiness;

    @Autowired
    UserRepository userRepository;


    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrganizations(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", orderBusiness.getOrderDetailByUserId(userId));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();

        response.put("data", orderBusiness.getOrderDetailById(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> order(@RequestBody OrderDTO order) {
        Map<String, Object> response = new HashMap<>();

        OrderDetail orderDetail = orderBusiness.getOrderDetailByUserId(order.getUserId());
        if (orderDetail == null) {
            orderDetail = new OrderDetail(order.getUserId(), null, null);
            OrderDetail orderDetailObj = orderBusiness.saveOrderDetail(orderDetail);
            OrderItem orderItem = new OrderItem(order.getDatasetId(), orderDetailObj.getId());
            response.put("data", orderBusiness.saveOrderItem(orderItem));

        } else {

            orderDetail = orderBusiness.getOrderDetailByUserId(order.getUserId());

            OrderItem orderItem = new OrderItem(order.getDatasetId(), orderDetail.getId());
            response.put("data", orderBusiness.saveOrderItem(orderItem));

        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> payment(@RequestBody PaymentDTO payment) {
        Map<String, Object> response = new HashMap<>();

        Optional<OrderDetail> orderDetail = orderBusiness.getOrderDetailById(payment.getOrderId());

        if (orderDetail.isPresent()) {
            PaymentDetail paymentDetail = orderBusiness.getPaymentDetailByOrderId(payment.getOrderId());
            if (paymentDetail == null) {
                paymentDetail = new PaymentDetail(payment.getOrderId(), orderDetail.get().getTotal(), payment.getDescription(), "pending");
            } else {
                paymentDetail.setAmount(orderDetail.get().getTotal());
                paymentDetail.setDescription(payment.getDescription());
                paymentDetail.setStatus("pending");
            }

            response.put("data", orderBusiness.savePaymentDetail(paymentDetail));

        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/invoices")
    public ResponseEntity<Map<String, Object>> getInvoices() {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            PaymentDetail p = orderBusiness.getPaymentByUserId(userData.getId());
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }else{
                response.put("data", p);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/invoice/{id}")
    public ResponseEntity<Map<String, Object>> paymentConfirm(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();

        Optional<PaymentDetail> paymentDetailOpt = orderBusiness.getPaymentById(id);
        if (paymentDetailOpt.isPresent()) {
            PaymentDetail paymentDetail = paymentDetailOpt.get();
            paymentDetail.setStatus("paid");
            response.put("data", orderBusiness.savePaymentDetail(paymentDetail));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", "Id not found");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

    }

    @DeleteMapping("/order/{id}")
    public void delete(@PathVariable Integer id) {
        orderBusiness.delete(id);
    }

}