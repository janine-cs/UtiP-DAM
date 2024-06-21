package com.utipdam.mobility.controller;


import com.utipdam.mobility.KeyUtil;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.OrderBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.model.LicenseDTO;
import com.utipdam.mobility.model.OrderDTO;
import com.utipdam.mobility.model.PurchaseDTO;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.UserRepository;
import net.bytebuddy.utility.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.sql.Timestamp;
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

    @Value("${utipdam.app.domain}")
    private String DOMAIN;

    private final int[] MONTH_LICENSE = {3,6,12};


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
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            order.setUserId(user.getId());
            username = user.getUsername();

            if (order.isSelectedDate()) {
                if (order.getDatasetIds() == null || order.getDatasetIds().isEmpty()){
                    response.put("error", "Select at least one date");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }

            String currency = PaymentDetail.Currency.EUR.name();
            if (order.getCurrency() == null) {
                currency = PaymentDetail.Currency.EUR.name();
            } else {
                if (Arrays.stream(PaymentDetail.Currency.values()).noneMatch(i -> i.name().equals(order.getCurrency()))) {
                    response.put("error", "Invalid currency value. " + Arrays.asList(PaymentDetail.Currency.values()));
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }

            if (order.isFutureDate()) {
                if (order.getMonthLicense() == null) {
                    response.put("error", "Please select future date month license");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                } else {
                    if (Arrays.stream(MONTH_LICENSE).noneMatch(i -> i == order.getMonthLicense())) {
                        response.put("error", "Invalid license month value. " + Arrays.toString(MONTH_LICENSE));
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                }
            }


//            List<OrderItem> items = orderBusiness.getOrderItemByUserId(order.getUserId());
//            long count = items.stream().filter(i -> i.getDatasetDefinitionId().equals(order.getDatasetDefinitionId())
//                    && i.isPastDate() == order.isPastDate()
//                    && i.isFutureDate() == order.isFutureDate()).count();
//
//            if (count > 0) {
//                response.put("error", "Duplicate order");
//                return new ResponseEntity<>(response, HttpStatus.CONFLICT);
//            }

            OrderDetail orderDetail = new OrderDetail(order.getUserId(), null, null);
            OrderDetail orderDetailSave = orderBusiness.saveOrderDetail(orderDetail);

            OrderItem orderItem = new OrderItem(order.getDatasetDefinitionId(), orderDetail.getId(),
                   order.isSelectedDate(), order.isPastDate(), order.isFutureDate(), order.getMonthLicense());
            OrderItem orderItemSave = orderBusiness.saveOrderItem(orderItem);

            if (order.isSelectedDate()){
                for (UUID dataset : order.getDatasetIds()){
                    OrderItemDataset orderItemDataset = new OrderItemDataset(orderItemSave.getId(), dataset);
                    orderBusiness.saveOrderItemDataset(orderItemDataset);
                }
            }


            Optional<DatasetDefinition> dtOpt = datasetDefinitionBusiness.getById(order.getDatasetDefinitionId());
            double total = 0D;
            Date licenseStartDate = null, licenseEndDate = null;
            String licenseKey = null;
            if (dtOpt.isPresent()) {
                DatasetDefinition dt = dtOpt.get();
                if (order.isSelectedDate()) {
                    total += (dt.getFee1d() == null ? 0D : dt.getFee1d()) * order.getDatasetIds().size();
                    licenseStartDate = licenseEndDate = new Date(System.currentTimeMillis());
                }

                if (order.isPastDate()) {
                    total += (dt.getFee() == null ? 0D : dt.getFee());
                    licenseStartDate = licenseEndDate = new Date(System.currentTimeMillis());
                }

                if (order.isFutureDate()) {
                    licenseStartDate = new Date(System.currentTimeMillis());
                    LocalDate ld = licenseStartDate.toLocalDate();
                    LocalDate monthLater;
                    if (order.getMonthLicense() == 12) {
                        total += (dt.getFee12mo() == null ? 0D : dt.getFee12mo());
                        monthLater = ld.plusMonths(12);
                    } else if (order.getMonthLicense() == 6) {
                        total += (dt.getFee6mo() == null ? 0D : dt.getFee6mo());
                        monthLater = ld.plusMonths(6);
                    } else {
                        total += (dt.getFee3mo() == null ? 0D : dt.getFee3mo());
                        monthLater = ld.plusMonths(3);
                    }
                    licenseEndDate = Date.valueOf(monthLater);

                } else {
                    order.setMonthLicense(null);
                }

                if (order.getPaymentSource().equalsIgnoreCase("paypal")){
                    licenseKey = KeyUtil.createLicenseKey(username, order.getPaymentId());
                }
            }else{
                response.put("error", "Dataset not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            logger.info("Frontend total= " + order.getTotalAmount() + ", backend total = " + total);
            PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), order.getTotalAmount(), order.getDescription(),
                    currency, order.getPaymentStatus(), licenseKey, order.getPaymentId(),
                    order.getPayerId(), order.getPaymentSource());

            paymentDetail.setLicenseStartDate(licenseStartDate);
            paymentDetail.setLicenseEndDate(licenseEndDate);
            PaymentDetail paymentDetailSave = orderBusiness.savePaymentDetail(paymentDetail);


            //Updating order detail
            Optional<OrderDetail> orderDetailUpdate = orderBusiness.getOrderDetailById(orderDetailSave.getId());
            if (orderDetailUpdate.isPresent()) {
                OrderDetail od = orderDetailUpdate.get();
                od.setTotal(order.getTotalAmount());
                od.setPaymentId(paymentDetailSave.getId());

                od.update(od);
                orderBusiness.saveOrderDetail(od);
            }

            response.put("data", paymentDetailSave);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/myPurchases")
    public ResponseEntity<Map<String, Object>> myPurchases() {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserId(userData.getId()).stream().filter(py ->
                    py.getStatus().equalsIgnoreCase("COMPLETED") ||
                            py.getStatus().equalsIgnoreCase("LICENSE_ONLY")).toList();
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                List<PurchaseDTO> data = p.stream().
                        map(d -> {
                            List<OrderItem> oiList = orderBusiness.getOrderItemByOrderId(d.getOrderId());
                            Optional<UUID> datasetDefinitionIdOpt = oiList.stream().map(OrderItem::getDatasetDefinitionId).findFirst();
                            UUID datasetDefinitionId;
                            DatasetDefinition datasetDefinition;

                            if (datasetDefinitionIdOpt.isPresent()) {
                                datasetDefinitionId = datasetDefinitionIdOpt.get();
                                String url = DOMAIN + "/api/dataset/" + datasetDefinitionId;
                                Optional<DatasetDefinition> dOpt = datasetDefinitionBusiness.getById(datasetDefinitionId);
                                if (dOpt.isPresent()) {
                                    datasetDefinition = dOpt.get();
                                    String status = d.getLicenseEndDate().after(new Date(System.currentTimeMillis())) || d.getLicenseEndDate().toLocalDate().isEqual(new Date(System.currentTimeMillis()).toLocalDate()) ? PaymentDetail.Status.ACTIVE.name() : PaymentDetail.Status.ARCHIVED.name();

                                    return new PurchaseDTO(d.getId(), datasetDefinitionId, datasetDefinition.getName(), datasetDefinition.getDescription(),
                                            status, d.getStatus(), d.getDatasetActivationKey(), d.getLicenseStartDate(), d.getLicenseEndDate(), url,
                                            d.getLicenseStartDate(), d.getAmount(), d.getCurrency(), datasetDefinition.getOrganization().getName(), d.getCreatedAt(), d.getModifiedAt());
                                }
                            }

                            return null;

                        }).collect(Collectors.toList());

                if (data.isEmpty()) {
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                } else {
                    response.put("data", data);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/purchase/{id}")
    public ResponseEntity<Map<String, Object>> purchaseDetail(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();

            Optional<PaymentDetail> p = orderBusiness.getPaymentById(id).filter(py -> py.getStatus().equalsIgnoreCase(PaymentDetail.PaymentStatus.COMPLETED.name()));
            if (p.isPresent()) {
                PaymentDetail payment = p.get();

                Optional<OrderDetail> odOpt = orderBusiness.getOrderDetailById(payment.getOrderId());

                if (odOpt.isPresent()) {
                    OrderDetail od = odOpt.get();
                    if (od.getUserId().equals(userData.getId())) {
                        List<OrderItem> oiList = orderBusiness.getOrderItemByOrderId(payment.getOrderId());
                        Optional<UUID> datasetDefinitionIdOpt = oiList.stream().map(OrderItem::getDatasetDefinitionId).findFirst();
                        UUID datasetDefinitionId;
                        DatasetDefinition datasetDefinition;

                        if (datasetDefinitionIdOpt.isPresent()) {
                            datasetDefinitionId = datasetDefinitionIdOpt.get();
                            String url = DOMAIN + "/api/dataset/" + datasetDefinitionId;
                            Optional<DatasetDefinition> dOpt = datasetDefinitionBusiness.getById(datasetDefinitionId);
                            if (dOpt.isPresent()) {
                                datasetDefinition = dOpt.get();
                                String status = payment.getLicenseEndDate().after(new Date(System.currentTimeMillis())) || payment.getLicenseEndDate().toLocalDate().isEqual(new Date(System.currentTimeMillis()).toLocalDate()) ? PaymentDetail.Status.ACTIVE.name() : PaymentDetail.Status.ARCHIVED.name();
                                PurchaseDTO data = new PurchaseDTO(payment.getId(), datasetDefinitionId, datasetDefinition.getName(), datasetDefinition.getDescription(),
                                        status, payment.getStatus(), payment.getDatasetActivationKey(),
                                        payment.getLicenseStartDate(), payment.getLicenseEndDate(), url,
                                        payment.getLicenseStartDate(), payment.getAmount(), payment.getCurrency(), datasetDefinition.getOrganization().getName(), payment.getCreatedAt(), payment.getModifiedAt());
                                response.put("data", data);
                                return new ResponseEntity<>(response, HttpStatus.OK);

                            }
                        }
                    }
                }

            }
        }

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("/license")
    public ResponseEntity<Map<String, Object>> createLicense(@RequestBody LicenseDTO license) {
        Map<String, Object> response = new HashMap<>();
        //owner
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            Optional<DatasetDefinition> datasetOpt = datasetDefinitionBusiness.getById(license.getDatasetDefinitionId());
            if (datasetOpt.isPresent()){
                //if user owns the dataset in the license
                if (datasetOpt.get().getUser().getId().equals(userOpt.get().getId())){
                    Optional<User> recipientUserOpt = userRepository.findByEmail(license.getRecipientEmail());
                    if (recipientUserOpt.isPresent()) {
                        User recipientUser = recipientUserOpt.get();

                        String currency = PaymentDetail.Currency.EUR.name();

                        if (license.isFutureDate()) {
                            if (license.getMonthLicense() == null) {
                                response.put("error", "Please select future date month license");
                                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                            } else {
                                if (Arrays.stream(MONTH_LICENSE).noneMatch(i -> i == license.getMonthLicense())) {
                                    response.put("error", "Invalid license month value. " + Arrays.toString(MONTH_LICENSE));
                                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                                }
                            }
                        }


                        OrderDetail orderDetail = new OrderDetail(recipientUser.getId(), null, null);
                        OrderDetail orderDetailSave = orderBusiness.saveOrderDetail(orderDetail);

                        OrderItem orderItem = new OrderItem(license.getDatasetDefinitionId(), orderDetail.getId(),
                                false, license.isPastDate(), license.isFutureDate(), license.getMonthLicense());
                        orderBusiness.saveOrderItem(orderItem);


                        Optional<DatasetDefinition> dtOpt = datasetDefinitionBusiness.getById(license.getDatasetDefinitionId());

                        Date licenseStartDate = null, licenseEndDate = null;
                        if (dtOpt.isPresent()) {
                            if (license.isPastDate()) {
                                licenseStartDate = licenseEndDate = new Date(System.currentTimeMillis());
                            }

                            if (license.isFutureDate()) {
                                licenseStartDate = new Date(System.currentTimeMillis());
                                LocalDate ld = licenseStartDate.toLocalDate();
                                LocalDate monthLater;
                                if (license.getMonthLicense() == 12) {
                                    monthLater = ld.plusMonths(12);
                                } else if (license.getMonthLicense() == 6) {
                                    monthLater = ld.plusMonths(6);
                                } else {
                                    monthLater = ld.plusMonths(3);
                                }
                                licenseEndDate = Date.valueOf(monthLater);

                            } else {
                                license.setMonthLicense(null);
                            }


                        }else{
                            response.put("error", "Dataset not found");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }
                        String licenseKey = KeyUtil.createLicenseKey(license.getRecipientEmail(), String.valueOf(new RandomString()));
                        PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), 0D, license.getDescription(),
                                currency, "LICENSE_ONLY", licenseKey, null,
                                null, "provided");

                        paymentDetail.setLicenseStartDate(licenseStartDate);
                        paymentDetail.setLicenseEndDate(licenseEndDate);
                        PaymentDetail paymentDetailSave = orderBusiness.savePaymentDetail(paymentDetail);


                        //Updating order detail
                        Optional<OrderDetail> orderDetailUpdate = orderBusiness.getOrderDetailById(orderDetailSave.getId());
                        if (orderDetailUpdate.isPresent()) {
                            OrderDetail od = orderDetailUpdate.get();
                            od.setTotal(0D);
                            od.setPaymentId(paymentDetailSave.getId());

                            od.update(od);
                            orderBusiness.saveOrderDetail(od);
                        }

                        response.put("data", paymentDetailSave);

                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }else{
                        response.put("error", "Recipient email does not exist. User not found");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }


                }else{
                    response.put("error", "Only dataset owners are allowed to create new license.");
                    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
                }
            } else {
                response.put("error", "Dataset not found.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        } else {
            response.put("error", "User not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/licenses/approval")
    public ResponseEntity<Map<String, Object>> getInvoices(@RequestParam(required = false) Boolean pending) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserIdAndPaymentSource(userData.getId(), "bank transfer");
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                if (pending != null) {
                    if (pending) {
                        p = p.stream().filter(item -> !item.getStatus().equals(PaymentDetail.PaymentStatus.COMPLETED.name())).collect(Collectors.toList());
                    }
                }

                response.put("data", p);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/licenses")
    public ResponseEntity<Map<String, Object>> getLicenses(@RequestParam(required = false) Boolean active) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserId(userData.getId());
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                if (active != null) {
                    if (active) {
                        p = p.stream().filter(item -> item.getStatus().equals(PaymentDetail.PaymentStatus.COMPLETED.name())).collect(Collectors.toList());
                    }
                }

                response.put("data", p);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/invoice/licenseActivation/{id}")
    public ResponseEntity<?>  activateLicense(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            PaymentDetail p = orderBusiness.activateLicense(id, userData.getUsername());
            if (p == null){
                return ResponseEntity.internalServerError().build();
            }else{
                return ResponseEntity.ok(p);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/invoice/licenseDeactivation/{id}")
    public void deactivateLicense(@PathVariable Integer id) {
        orderBusiness.deactivateLicense(id);
    }

    @DeleteMapping("/license/{id}")
    public ResponseEntity<Map<String, Object>> deleteLicense(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<PaymentDetail> paymentOpt = orderBusiness.getPaymentById(id);
            if (paymentOpt.isPresent()){
                PaymentDetail payment = paymentOpt.get();
                List<OrderItem> orderList = orderBusiness.getOrderItemByOrderId(payment.getOrderId());
                if (orderList != null && !orderList.isEmpty()) {
                    Optional<DatasetDefinition> datasetOpt = datasetDefinitionBusiness.getById(orderList.get(0).getDatasetDefinitionId());
                    if (datasetOpt.isPresent()){
                        //if user owns the dataset in the license
                        if (datasetOpt.get().getUser().getId().equals(user.getId())){

                            if (payment.getStatus().equalsIgnoreCase("LICENSE_ONLY")){
                                payment.setStatus("LICENSE_REVOKED");
                                payment.setDeactivate(true);
                                payment.setModifiedAt(new Timestamp(System.currentTimeMillis()));
                                orderBusiness.savePaymentDetail(payment);

                                response.put("status", "Deleted");
                                return new ResponseEntity<>(response, HttpStatus.OK);

                            }
                        }
                    }
                }

            }

        }
        response.put("error", "Unauthorized");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

}