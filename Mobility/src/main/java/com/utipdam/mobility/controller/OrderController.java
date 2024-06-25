package com.utipdam.mobility.controller;


import com.utipdam.mobility.business.DatasetBusiness;
import com.utipdam.mobility.business.DatasetDefinitionBusiness;
import com.utipdam.mobility.business.OrderBusiness;
import com.utipdam.mobility.config.AuthTokenFilter;
import com.utipdam.mobility.model.LicenseDTO;
import com.utipdam.mobility.model.OrderDTO;
import com.utipdam.mobility.model.PurchaseDTO;
import com.utipdam.mobility.model.entity.*;
import com.utipdam.mobility.model.repository.UserRepository;
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

    @Autowired
    private DatasetBusiness datasetBusiness;

    @Value("${utipdam.app.domain}")
    private String DOMAIN;

    private final int[] MONTH_LICENSE = {3, 6, 12};


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

    //paypal, bank transfer transactions
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> order(@RequestBody OrderDTO order) {
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            order.setUserId(user.getId());

            if (order.isSelectedDate()) {
                if (order.getDatasetIds() == null || order.getDatasetIds().isEmpty()) {
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

            if (!order.isSelectedDate() && !order.isPastDate() && !order.isFutureDate()){
                response.put("error", "Invalid selection. Select a date, past or future");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

            Optional<DatasetDefinition> datasetDefinitionOpt = datasetDefinitionBusiness.getById(order.getDatasetDefinitionId());
            DatasetDefinition datasetDefinition;
            if (datasetDefinitionOpt.isPresent()) {
                datasetDefinition = datasetDefinitionOpt.get();
            } else {
                response.put("error", "Dataset definition not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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

            if (order.isSelectedDate()) {
                for (UUID dataset : order.getDatasetIds()) {
                    OrderItemDataset orderItemDataset = new OrderItemDataset(orderItemSave.getId(), dataset);
                    orderBusiness.saveOrderItemDataset(orderItemDataset);
                }
            }

            Optional<DatasetDefinition> dtOpt = datasetDefinitionBusiness.getById(order.getDatasetDefinitionId());
            double total = 0D;
            Date licenseStartDate = null, licenseEndDate = null;
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

            } else {
                response.put("error", "Dataset not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            logger.info("Frontend total= " + order.getTotalAmount() + ", backend total = " + total);
            PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), order.getTotalAmount(), order.getDescription(),
                    currency, order.getPaymentStatus(), order.getPaymentId(),
                    order.getPayerId(), order.getPaymentSource());

            paymentDetail.setLicenseStartDate(licenseStartDate);
            paymentDetail.setLicenseEndDate(licenseEndDate);
            PaymentDetail paymentDetailSave = orderBusiness.savePaymentDetail(paymentDetail);

            DatasetActivation datasetActivation = createApiKeyRequest(paymentDetailSave.getId(), orderItemSave.getId(),
                    orderDetailSave.getUserId(), licenseEndDate,
                    order.getPaymentSource().equalsIgnoreCase("paypal"), datasetDefinition.getUser().getId());
            DatasetActivation datasetActivationSave = orderBusiness.saveDatasetActivation(datasetActivation);

            //Updating order detail
            Optional<OrderDetail> orderDetailUpdate = orderBusiness.getOrderDetailById(orderDetailSave.getId());
            if (orderDetailUpdate.isPresent()) {
                OrderDetail od = orderDetailUpdate.get();
                od.setTotal(order.getTotalAmount());
                od.setPaymentId(paymentDetailSave.getId());

                od.update(od);
                orderBusiness.saveOrderDetail(od);
            }

            response.put("data", datasetActivationSave);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    private DatasetActivation createApiKeyRequest(Integer paymentId, Integer orderItemId,
                                                  Long userId, Date expirationDate, Boolean active,
                                                  Long datasetOwnerId) {
        DatasetActivation apiKey = new DatasetActivation();
        apiKey.setPaymentDetailId(paymentId);
        apiKey.setOrderItemId(orderItemId);
        apiKey.setUserId(userId);
        apiKey.setApiKey(UUID.randomUUID());
        apiKey.setExpirationDate(expirationDate);
        apiKey.setDatasetOwnerId(datasetOwnerId);
        apiKey.setActive(active);

        return apiKey;
    }

    @GetMapping("/myPurchases")
    public ResponseEntity<Map<String, Object>> myPurchases(@RequestParam(required = false) List<String> paymentStatus) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserId(userData.getId());
            if (paymentStatus != null){
                paymentStatus = paymentStatus.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                List<String> finalPaymentStatus = paymentStatus;
                p = p.stream().filter(mp -> finalPaymentStatus.contains(mp.getStatus().toLowerCase())).toList();
            }

            if (!p.isEmpty()) {
                List<PurchaseDTO> data = p.stream().
                        map(d -> {
                            Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(d.getId());
                            if (datasetActivationOpt.isPresent()) {

                                DatasetActivation datasetActivation = datasetActivationOpt.get();

                                if (datasetActivation.getUserId().equals(userData.getId())) {
                                    Optional<OrderItem> orderItemOpt = orderBusiness.getOrderItemById(datasetActivation.getOrderItemId());
                                    if (orderItemOpt.isPresent()) {
                                        OrderItem orderItem = orderItemOpt.get();
                                        List<UUID> datasetIds = null;
                                        List<String> selectedDates = new ArrayList<>();
                                        if (orderItem.isSelectedDate()){
                                            datasetIds = orderBusiness.getAllByOrderItemId(datasetActivation
                                                    .getOrderItemId()).stream().map(OrderItemDataset::getDatasetId).collect(Collectors.toList());

                                            for(UUID id : datasetIds){
                                                Optional<Dataset> dataset = datasetBusiness.getById(id);
                                                dataset.ifPresent(value -> selectedDates.add(String.valueOf(value.getStartDate())));
                                            }
                                        }

                                        UUID datasetDefinitionId = orderItem.getDatasetDefinitionId();
                                        String url = DOMAIN + "/api/dataset/" + datasetDefinitionId;
                                        Optional<DatasetDefinition> dOpt = datasetDefinitionBusiness.getById(datasetDefinitionId);
                                        if (dOpt.isPresent()) {
                                            DatasetDefinition datasetDefinition = dOpt.get();
                                            String status = datasetActivation.isActive() && (d.getLicenseEndDate().after(new Date(System.currentTimeMillis())) || d.getLicenseEndDate().toLocalDate().isEqual(new Date(System.currentTimeMillis()).toLocalDate())) ? "ACTIVE" : "INACTIVE";

                                            return new PurchaseDTO(d.getId(), datasetDefinitionId, datasetDefinition.getName(), datasetDefinition.getDescription(),
                                                    datasetDefinition.getOrganization().getName(), datasetDefinition.getOrganization().getEmail(),
                                                    orderItem.isSelectedDate(), datasetIds, selectedDates, orderItem.isPastDate(), orderItem.isFutureDate(),
                                                    status, d.getStatus(), datasetActivation.getApiKey(), d.getLicenseStartDate(), d.getLicenseEndDate(), url,
                                                    d.getLicenseStartDate(), d.getAmount(), d.getCurrency(), d.getCreatedAt(), d.getModifiedAt());
                                        }
                                    }

                                }
                            }

                            return null;

                        }).collect(Collectors.toList());

                if (!data.isEmpty()) {
                    response.put("data", data);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    @GetMapping("/purchase/{id}")
    public ResponseEntity<Map<String, Object>> purchaseDetail(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        Map<String, Object> response = new HashMap<>();
        if (userOpt.isPresent()) {
            User userData = userOpt.get();

            Optional<PaymentDetail> p = orderBusiness.getPaymentById(id);

            if (p.isPresent()) {
                PaymentDetail payment = p.get();
                Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(payment.getId());
                if (datasetActivationOpt.isPresent()) {
                    DatasetActivation datasetActivation = datasetActivationOpt.get();
                    if (datasetActivation.getUserId().equals(userData.getId())) {
                        Optional<OrderItem> orderItemOpt = orderBusiness.getOrderItemById(datasetActivation.getOrderItemId());
                        if (orderItemOpt.isPresent()) {
                            OrderItem orderItem = orderItemOpt.get();
                            List<UUID> datasetIds = null;
                            List<String> selectedDates = new ArrayList<>();
                            if (orderItem.isSelectedDate()){
                                datasetIds = orderBusiness.getAllByOrderItemId(datasetActivation
                                        .getOrderItemId()).stream().map(OrderItemDataset::getDatasetId).collect(Collectors.toList());
                                for(UUID dId : datasetIds){
                                    Optional<Dataset> dataset = datasetBusiness.getById(dId);
                                    dataset.ifPresent(value -> selectedDates.add(String.valueOf(value.getStartDate())));
                                }
                            }

                            UUID datasetDefinitionId = orderItem.getDatasetDefinitionId();
                            String url = DOMAIN + "/api/dataset/" + datasetDefinitionId;
                            Optional<DatasetDefinition> dOpt = datasetDefinitionBusiness.getById(datasetDefinitionId);
                            if (dOpt.isPresent()) {
                                DatasetDefinition datasetDefinition = dOpt.get();
                                String status = datasetActivation.isActive() && (payment.getLicenseEndDate().after(new Date(System.currentTimeMillis())) || payment.getLicenseEndDate().toLocalDate().isEqual(new Date(System.currentTimeMillis()).toLocalDate())) ?  "ACTIVE" : "INACTIVE";

                                PurchaseDTO data = new PurchaseDTO(payment.getId(), datasetDefinitionId, datasetDefinition.getName(), datasetDefinition.getDescription(),
                                        datasetDefinition.getOrganization().getName(), datasetDefinition.getOrganization().getEmail(),
                                        orderItem.isSelectedDate(), datasetIds, selectedDates, orderItem.isPastDate(), orderItem.isFutureDate(), status, payment.getStatus(), datasetActivation.getApiKey(),
                                        payment.getLicenseStartDate(), payment.getLicenseEndDate(), url,
                                        payment.getLicenseStartDate(), payment.getAmount(), payment.getCurrency(), payment.getCreatedAt(), payment.getModifiedAt());
                                response.put("data", data);
                                return new ResponseEntity<>(response, HttpStatus.OK);

                            }
                        }
                    }
                }
            }
        }

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/license")
    public ResponseEntity<Map<String, Object>> createLicense(@RequestBody LicenseDTO license) {
        Map<String, Object> response = new HashMap<>();
        //owner
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            Optional<DatasetDefinition> datasetOpt = datasetDefinitionBusiness.getById(license.getDatasetDefinitionId());
            if (datasetOpt.isPresent()) {
                //if user owns the dataset in the license
                if (datasetOpt.get().getUser().getId().equals(userOpt.get().getId())) {
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

                        Optional<DatasetDefinition> datasetDefinitionOpt = datasetDefinitionBusiness.getById(license.getDatasetDefinitionId());
                        DatasetDefinition datasetDefinition;
                        if (datasetDefinitionOpt.isPresent()) {
                            datasetDefinition = datasetDefinitionOpt.get();
                        } else {
                            response.put("error", "Dataset definition not found");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }

                        OrderDetail orderDetail = new OrderDetail(recipientUser.getId(), null, null);
                        OrderDetail orderDetailSave = orderBusiness.saveOrderDetail(orderDetail);

                        OrderItem orderItem = new OrderItem(license.getDatasetDefinitionId(), orderDetail.getId(),
                                false, license.isPastDate(), license.isFutureDate(), license.getMonthLicense());
                        OrderItem orderItemSave = orderBusiness.saveOrderItem(orderItem);


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


                        } else {
                            response.put("error", "Dataset not found");
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }

                        PaymentDetail paymentDetail = new PaymentDetail(orderDetail.getId(), 0D, license.getDescription(),
                                currency, "LICENSE_ONLY", null,
                                null, "provided");

                        paymentDetail.setLicenseStartDate(licenseStartDate);
                        paymentDetail.setLicenseEndDate(licenseEndDate);
                        PaymentDetail paymentDetailSave = orderBusiness.savePaymentDetail(paymentDetail);


                        DatasetActivation datasetActivation = createApiKeyRequest(paymentDetailSave.getId(), orderItemSave.getId(),
                                orderDetailSave.getUserId(), licenseEndDate,
                                true, datasetDefinition.getUser().getId());
                        DatasetActivation datasetActivationSave = orderBusiness.saveDatasetActivation(datasetActivation);

                        //Updating order detail
                        Optional<OrderDetail> orderDetailUpdate = orderBusiness.getOrderDetailById(orderDetailSave.getId());
                        if (orderDetailUpdate.isPresent()) {
                            OrderDetail od = orderDetailUpdate.get();
                            od.setTotal(0D);
                            od.setPaymentId(paymentDetailSave.getId());

                            od.update(od);
                            orderBusiness.saveOrderDetail(od);
                        }

                        response.put("data", datasetActivationSave);

                        return new ResponseEntity<>(response, HttpStatus.OK);
                    } else {
                        response.put("error", "Recipient email does not exist. User not found");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }


                } else {
                    response.put("error", "Only dataset owners are allowed to create new license.");
                    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
                }
            } else {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        } else {
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
            List<PaymentDetail> p = orderBusiness.getAllPurchasesByUserIdAndIsActive(userData.getId(), active);
            if (p == null) {
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            } else {
                if (active != null) {
                    if (active) {
                        p = p.stream().filter(item ->
                                        item.getStatus().equals(PaymentDetail.PaymentStatus.COMPLETED.name()))
                                .collect(Collectors.toList());
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
    public ResponseEntity<?> activateLicense(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<PaymentDetail> paymentOpt = orderBusiness.getPaymentById(id);
            if (paymentOpt.isPresent()) {
                PaymentDetail payment = paymentOpt.get();
                //if user owns the dataset in the license
                Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(id);
                if (datasetActivationOpt.isPresent()) {
                    DatasetActivation datasetActivation = datasetActivationOpt.get();

                    if (datasetActivation.getDatasetOwnerId().equals(user.getId())) {

                        payment.setModifiedAt(new Timestamp(System.currentTimeMillis()));
                        orderBusiness.savePaymentDetail(payment);
                        orderBusiness.activateLicense(id, true);
                        response.put("status", "Deleted");
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/invoice/licenseDeactivation/{id}")
    public ResponseEntity<?> deactivateLicense(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<PaymentDetail> paymentOpt = orderBusiness.getPaymentById(id);
            if (paymentOpt.isPresent()) {
                PaymentDetail payment = paymentOpt.get();
                //if user owns the dataset in the license
                Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(id);
                if (datasetActivationOpt.isPresent()) {
                    DatasetActivation datasetActivation = datasetActivationOpt.get();

                    if (datasetActivation.getDatasetOwnerId().equals(user.getId())) {

                        payment.setModifiedAt(new Timestamp(System.currentTimeMillis()));
                        orderBusiness.savePaymentDetail(payment);
                        orderBusiness.activateLicense(id, false);
                        response.put("status", "Deleted");
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/license/{id}")
    public ResponseEntity<Map<String, Object>> modifyLicense(@PathVariable Integer id,
                                                             @RequestBody LicenseDTO license) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            //if user owns the dataset in the license
            Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(id);
            if (datasetActivationOpt.isPresent()) {
                DatasetActivation datasetActivation = datasetActivationOpt.get();
                Optional<OrderItem> orderItemOpt = orderBusiness.getOrderItemById(datasetActivation.getOrderItemId());
                if (orderItemOpt.isPresent()) {
                    OrderItem orderItem = orderItemOpt.get();
                    if (orderItem.isFutureDate()) {
                        if (datasetActivation.getDatasetOwnerId().equals(user.getId())) {
                            orderItem.setMonthLicense(license.getMonthLicense());
                            orderItem.setModifiedAt(new Timestamp(System.currentTimeMillis()));
                            response.put("data", orderBusiness.saveOrderItem(orderItem));
                            return new ResponseEntity<>(response, HttpStatus.OK);
                        } else {
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                        }
                    }

                    Optional<PaymentDetail> payOpt = orderBusiness.getPaymentById(id);
                    if (payOpt.isPresent()){
                        PaymentDetail pay = payOpt.get();

                        if (license.getLicenseStartDate() != null && license.getLicenseEndDate() != null){
                            Date start = Date.valueOf(license.getLicenseStartDate());
                            Date end = Date.valueOf(license.getLicenseEndDate());
                            if (start.before(end) || start.equals(end)){
                                pay.setLicenseStartDate(start);
                                pay.setLicenseEndDate(end);
                                orderBusiness.savePaymentDetail(pay);
                            }
                        }
                    }
                }
            }
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/license/{id}")
    public ResponseEntity<?> deleteLicense(@PathVariable Integer id) {
        Optional<User> userOpt = userRepository.findByUsername(AuthTokenFilter.usernameLoggedIn);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<PaymentDetail> paymentOpt = orderBusiness.getPaymentById(id);
            if (paymentOpt.isPresent()) {
                PaymentDetail payment = paymentOpt.get();
                //if user owns the dataset in the license
                Optional<DatasetActivation> datasetActivationOpt = orderBusiness.getByPaymentDetailId(id);
                if (datasetActivationOpt.isPresent()) {
                    DatasetActivation datasetActivation = datasetActivationOpt.get();

                    if (datasetActivation.getDatasetOwnerId().equals(user.getId())) {
                        if (payment.getStatus().equalsIgnoreCase("LICENSE_ONLY")) {
                            orderBusiness.deleteActivation(id);
                            orderBusiness.deleteInvoice(id);
                        }
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                }
            }
        }
        return ResponseEntity.notFound().build();
    }
}