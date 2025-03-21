package com.skaet.ussd.payment.dto;

import lombok.Data;

@Data
public class FlutterwaveResponse {
    private String status;
    private String message;
    private FlutterwaveData data;
    private FlutterwaveMeta meta;
}
