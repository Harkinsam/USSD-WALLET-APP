package com.skaet.ussd.notification.service;

public interface SmsNotificationService {
    void sendSms(String phoneNumber, String message);
}