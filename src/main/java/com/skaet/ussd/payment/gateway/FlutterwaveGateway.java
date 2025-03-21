package com.skaet.ussd.payment.gateway;

import com.skaet.ussd.notification.service.SmsNotificationService;
import com.skaet.ussd.payment.dto.FlutterwaveData;
import com.skaet.ussd.payment.dto.FlutterwaveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlutterwaveGateway implements PaymentGateway {
    @Value("${flutterwave.api.baseUrl}")
    private String baseUrl;
    @Value("${flutterwave.secretKey}")
    private String apiKey;
    private final RestTemplate restTemplate;
    private final SmsNotificationService smsService;

    @Override
    public String initiateDeposit(String phoneNumber, BigDecimal amount) {
        try {

            String reference = generateReference();
            String url = baseUrl + "/charges?type=ussd";

            log.info("Flutterwave deposit request URL: {}", url);

            Map<String, Object> requestBody = getStringObjectMap(phoneNumber, amount, reference);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<FlutterwaveResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                FlutterwaveResponse.class
            );

            log.info("Flutterwave response: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                FlutterwaveResponse responseBody = response.getBody();
                if ("success".equals(responseBody.getStatus())) {
                    FlutterwaveData data = responseBody.getData();
                    String ussdCode = responseBody.getMeta().getAuthorization().getNote();

                    // Send USSD code via SMS
                    String smsMessage = String.format("""
                                    Your deposit of NGN %s is initiated.
                                    Dial %s to complete payment.
                                    Reference: %s""",
                            amount, ussdCode, data.getTxRef());

                    smsService.sendSms(phoneNumber, smsMessage);
                    log.info(smsMessage);

                    return data.getTxRef();
                }
            }
            
            log.error("Invalid response from Flutterwave: {}", response.getBody());
            return "FAILED";
            
        } catch (Exception e) {
            log.error("Flutterwave deposit failed: {}", e.getMessage());
            return "FAILED";
        }
    }

    public String initiateWithdrawal(String phoneNumber, BigDecimal amount, String bankCode, String accountNumber) {
        try {
            String reference = generateReference();
            String url = baseUrl + "/transfers";

            log.info("Flutterwave withdrawal request URL: {}", url);

            // Prepare request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("account_bank", bankCode);  // Bank code (e.g., GTB = 058)
            requestBody.put("account_number", accountNumber);
            requestBody.put("amount", amount);
            requestBody.put("currency", "NGN");
            requestBody.put("reference", reference);
            requestBody.put("narration", "Withdrawal to bank account");
            requestBody.put("debit_currency", "NGN");


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<FlutterwaveResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    FlutterwaveResponse.class
            );

            log.info("Flutterwave response: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                FlutterwaveResponse responseBody = response.getBody();
                if ("success".equals(responseBody.getStatus())) {
                    FlutterwaveData data = responseBody.getData();

                    // Send withdrawal confirmation SMS
                    String smsMessage = String.format("""
                                Your withdrawal of NGN %s is being processed.
                                Reference: %s
                                You will receive the funds shortly.""",
                            amount, data.getTxRef());

                    smsService.sendSms(phoneNumber, smsMessage);
                    log.info(smsMessage);

                    return data.getTxRef();
                }
            }

            log.error("Invalid response from Flutterwave: {}", response.getBody());
            return "FAILED";

        } catch (Exception e) {
            log.error("Flutterwave withdrawal failed: {}", e.getMessage());
            return "FAILED";
        }
    }


    @Override
    public String verifyTransaction(String reference) {
        return null;
    }

    private static Map<String, Object> getStringObjectMap(String phoneNumber, BigDecimal amount, String reference) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("account_bank", "058");  // Default to GTBank
        requestBody.put("amount", amount.intValue());
        requestBody.put("currency", "NGN");
        requestBody.put("email", phoneNumber + "@skaet.com");  // Generate email from phone
        requestBody.put("tx_ref", reference);
        requestBody.put("phone_number", phoneNumber);
        requestBody.put("fullname", "SKAET Customer");  // Can be updated with actual name
        return requestBody;
    }



    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getGatewayName() {
        return "flutterwave";
    }

    private String generateReference() {
        return "FLW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}