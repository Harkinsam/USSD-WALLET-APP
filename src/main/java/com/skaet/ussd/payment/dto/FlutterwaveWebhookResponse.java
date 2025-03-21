package com.skaet.ussd.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlutterwaveWebhookResponse {
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
        
        @JsonProperty("app_fee")
        private BigDecimal appFee;
        
        private String status;
        
        @JsonProperty("payment_type")
        private String paymentType;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        
        @JsonProperty("processor_response")
        private String processorResponse;
        
        @JsonProperty("auth_model")
        private String authModel;
        
        private CustomerData customer;
    }

    @Data
    public static class CustomerData {
        private Long id;
        private String name;
        
        @JsonProperty("phone_number")
        private String phoneNumber;
        
        private String email;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
    }
}