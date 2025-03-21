package com.skaet.ussd.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FlutterwaveData {
    private Long id;
    @JsonProperty("tx_ref")
    private String txRef;
    @JsonProperty("flw_ref")
    private String flwRef;
    private String status;
    @JsonProperty("payment_code")
    private String paymentCode;
    private BigDecimal amount;
    private String currency;
}

