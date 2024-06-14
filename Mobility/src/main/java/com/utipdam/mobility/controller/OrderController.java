package com.utipdam.mobility.controller;

import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
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

    @Autowired
    private DatasetDefinitionBusiness datasetDefinitionBusiness;

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam Long userId) {
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

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> order(@RequestBody OrderDTO order) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> user = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);
        user.ifPresent(value -> order.setUserId(value.getId()));

        OrderDetail orderDetail = new OrderDetail(order.getUserId(), null, null);
        OrderDetail orderDetailSave = orderBusiness.saveOrderDetail(orderDetail);

        OrderItem orderItem = new OrderItem(order.getDatasetDefinitionId(), orderDetail.getId(),
                order.getStartDate(), order.getEndDate(), order.isOneDay(), order.isPastDate(), order.isFutureDate(), order.getMonthLicense());
        response.put("data", orderBusiness.saveOrderItem(orderItem));

        Optional<DatasetDefinition> dtOpt = datasetDefinitionBusiness.getById(order.getDatasetDefinitionId());
        double total = 0D;
        if (dtOpt.isPresent()){
            DatasetDefinition dt = dtOpt.get();
            if (order.isOneDay()){
                total += (dt.getFee1d() == null ? 0D : dt.getFee1d());
            }else{
                if (order.isPastDate()){
                    total +=  (dt.getFee() == null ? 0D : dt.getFee());
                }

                if (order.isFutureDate()){
                    if (order.getMonthLicense() == 12){
                        total +=  (dt.getFee12mo() == null ? 0D : dt.getFee12mo());
                    } else if (order.getMonthLicense() == 6){
                        total +=  (dt.getFee6mo() == null ? 0D : dt.getFee6mo());
                    } else{
                        total +=  (dt.getFee3mo() == null ? 0D : dt.getFee3mo());
                    }

                }
            }

        }

        PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), total, order.getDescription(),
                "PAID", order.getPaypalOrderID(),
                order.getPayerID(), order.getPaymentID(), order.getPaymentSource());
        PaymentDetail paymentDetailSave = orderBusiness.savePaymentDetail(paymentDetail);


        //Updating order detail
        Optional<OrderDetail> orderDetailUpdate = orderBusiness.getOrderDetailById(orderDetailSave.getId());
        if (orderDetailUpdate.isPresent()){
            OrderDetail od = orderDetailUpdate.get();
            od.setTotal(total);
            od.setPaymentId(paymentDetailSave.getId());

            od.update(od);
            orderBusiness.saveOrderDetail(od);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


//    @PostMapping("/checkout")
//    public ResponseEntity<Map<String, Object>> payment(@RequestBody PaymentDTO payment) {
//        Map<String, Object> response = new HashMap<>();
//        Optional<OrderDetail> orderDetail = orderBusiness.getOrderDetailById(payment.getOrderId());
//
//        if (orderDetail.isPresent()) {
//            PaymentDetail paymentDetail = orderBusiness.getPaymentDetailByOrderId(payment.getOrderId());
//            if (paymentDetail == null) {
//                paymentDetail = new PaymentDetail(payment.getOrderId(), orderDetail.get().getTotal(), payment.getDescription(), "pending");
//            } else {
//                paymentDetail.setAmount(orderDetail.get().getTotal());
//                paymentDetail.setDescription(payment.getDescription());
//                paymentDetail.setStatus("pending");
//            }
//
//            response.put("data", orderBusiness.savePaymentDetail(paymentDetail));
//
//        }
//        return new ResponseEntity<>(response, HttpStatus.OK);
//    }

    @GetMapping("/invoices")
    public ResponseEntity<Map<String, Object>> getInvoices() {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            PaymentDetail p = orderBusiness.getPaymentByUserId(userData.getId());
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
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