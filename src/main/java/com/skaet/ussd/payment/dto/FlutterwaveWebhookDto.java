package com.skaet.ussd.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlutterwaveWebhookDto {
    private String event;
    private TransactionData data;

    @Data
    public static class TransactionData {
        private Long id;
        
        @JsonProperty("tx_ref")
        private String txRef;
        
        @JsonProperty("flw_ref")
        private String flwRef;
        
        private BigDecimal amount;
        private String currency;
        
        @JsonProperty("charged_amount")
        private BigDecimal chargedAmount;
        
        private String status;
        
        @JsonProperty("payment_type")
        private String paymentType;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        
        private Customer customer;
    }

    @Data
    public static class Customer {
        private Long id;
        private String name;
        
        @JsonProperty("phone_number")
        private String phoneNumber;
        
        private String email;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
    }
}