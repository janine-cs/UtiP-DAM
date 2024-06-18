package com.utipdam.internal.model;

import lombok.Data;

@Data
public class Vendor {
    private Integer id;
    private String accountNo;
    private String accountName;
    private String bankName;
    private String companyName;
    private String companyVatNo;
    private String companyRegNo;
    private String companyAddress;
    private String country;
    private String contactName;
    private String contactEmail;
    private String swiftCode;


    public Vendor() {
    }

}
