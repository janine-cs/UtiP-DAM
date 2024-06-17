package com.utipdam.mobility.controller;


import com.utipdam.mobility.KeyUtil;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.OrderBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.model.OrderDTO;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);
        String username;
        if (userOpt.isPresent()){
            User user = userOpt.get();
            order.setUserId(user.getId());
            username = user.getUsername();

            String currency = PaymentDetail.Currency.EUR.name();
            if (order.getCurrency() == null ){
                currency = PaymentDetail.Currency.EUR.name();
            }else{
                if (Arrays.stream(PaymentDetail.Currency.values()).noneMatch(i -> i.name().equals(order.getCurrency()))){
                    response.put("error", "Invalid currency value. " + Arrays.asList(PaymentDetail.Currency.values()));
                }
            }


            OrderDetail orderDetail = new OrderDetail(order.getUserId(), null, null);
            OrderDetail orderDetailSave = orderBusiness.saveOrderDetail(orderDetail);

            OrderItem orderItem = new OrderItem(order.getDatasetDefinitionId(), orderDetail.getId(),
                    order.getStartDate(), order.getEndDate(), order.isOneDay(), order.isPastDate(), order.isFutureDate(), order.getMonthLicense());
            response.put("data", orderBusiness.saveOrderItem(orderItem));

            Optional<DatasetDefinition> dtOpt = datasetDefinitionBusiness.getById(order.getDatasetDefinitionId());
            double total = 0D;
            Date licenseStartDate = null, licenseEndDate = null;
            String licenseKey = null;
            if (dtOpt.isPresent()){
                DatasetDefinition dt = dtOpt.get();
                if (order.isOneDay()){
                    total += (dt.getFee1d() == null ? 0D : dt.getFee1d());
                    licenseStartDate=licenseEndDate=new Date(System.currentTimeMillis());
                }else{
                    if (order.isPastDate()){
                        total +=  (dt.getFee() == null ? 0D : dt.getFee());
                        licenseStartDate=licenseEndDate=new Date(System.currentTimeMillis());
                    }

                    if (order.isFutureDate()){
                        licenseStartDate=new Date(System.currentTimeMillis());
                        LocalDate ld = licenseStartDate.toLocalDate();
                        LocalDate monthLater;
                        if (order.getMonthLicense() == 12){
                            total +=  (dt.getFee12mo() == null ? 0D : dt.getFee12mo());
                            monthLater = ld.plusMonths( 12 );
                        } else if (order.getMonthLicense() == 6){
                            total +=  (dt.getFee6mo() == null ? 0D : dt.getFee6mo());
                            monthLater = ld.plusMonths( 6 );
                        } else{
                            total +=  (dt.getFee3mo() == null ? 0D : dt.getFee3mo());
                            monthLater = ld.plusMonths( 3 );
                        }
                        licenseEndDate= Date.valueOf( monthLater );

                    }
                }
                licenseKey= KeyUtil.createLicenseKey(username, order.getPaymentId());
            }

            PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), total, order.getDescription(),
                    currency, PaymentDetail.PaymentStatus.COMPLETED.name(), licenseKey, order.getPaymentId(),
                    order.getPayerId(),order.getPaymentSource());

            paymentDetail.setLicenseStartDate(licenseStartDate);
            paymentDetail.setLicenseEndDate(licenseEndDate);
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
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<Map<String, Object>> getInvoices(@RequestParam String status) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserId(userData.getId());
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                if (status.equalsIgnoreCase(PaymentDetail.Status.PENDING.name())){
                    p = p.stream().filter(item -> !item.getStatus().equals(PaymentDetail.PaymentStatus.COMPLETED.name())).collect(Collectors.toList());
                }else if (status.equalsIgnoreCase(PaymentDetail.Status.ACTIVE.name())){

                    p = p.stream().filter(item -> item.getLicenseEndDate().after(new Date(System.currentTimeMillis()))).collect(Collectors.toList());
                }else if (status.equalsIgnoreCase(PaymentDetail.Status.ARCHIVED.name())){
                    p = p.stream().filter(item -> item.getLicenseEndDate().before(new Date(System.currentTimeMillis()))).collect(Collectors.toList());
                }else {
                    response.put("error", "Invalid status value. " + Arrays.asList(PaymentDetail.Status.values()));
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }

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
            paymentDetail.setStatus(PaymentDetail.PaymentStatus.COMPLETED.name());
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