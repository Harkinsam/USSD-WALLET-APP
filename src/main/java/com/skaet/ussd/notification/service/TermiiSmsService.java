package com.skaet.ussd.notification.service;

import com.skaet.ussd.notification.dto.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class TermiiSmsService implements SmsNotificationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${sms.api.baseUrl}")
    private String baseUrl;
    
    @Value("${sms.api.key}")
    private String apiKey;
    
    @Value("${sms.sender}")
    private String sender;

    @Override
    public void sendSms(String phoneNumber, String message) {
        try {
            String formatPhoneNumber = formatPhoneNumber(phoneNumber);
            
            SmsRequest request = new SmsRequest();
            request.setTo(formatPhoneNumber);
            request.setFrom("+2348168864996");
            request.setSms(message);
            request.setApi_key(apiKey);
            request.setChannel("sandbox");
            request.setType("plain");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/sms/send",
                request, 
                String.class
            );

            boolean isSuccess = response.getStatusCode().is2xxSuccessful();
            if (isSuccess) {
                log.info("SMS sent successfully to {}", phoneNumber);
            } else {
                log.error("Failed to send SMS. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replace("+", "");
        
        if (!phoneNumber.startsWith("234") && phoneNumber.startsWith("0")) {
            phoneNumber = "234" + phoneNumber.substring(1);
        }
        
        return phoneNumber;
    }
}