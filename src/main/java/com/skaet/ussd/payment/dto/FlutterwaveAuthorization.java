package com.skaet.ussd.payment.dto;

import lombok.Data;

@Data
public class FlutterwaveAuthorization {
    private String mode;
    private String note;
}
