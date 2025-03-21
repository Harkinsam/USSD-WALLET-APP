package com.skaet.ussd.payment.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {
    String initiateDeposit(String phoneNumber, BigDecimal amount);
    String initiateWithdrawal(String phoneNumber, BigDecimal amount, String bankCode, String accountNumber);
    String verifyTransaction(String reference);
    boolean isAvailable();
    String getGatewayName();
}