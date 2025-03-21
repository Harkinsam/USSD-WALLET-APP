package com.skaet.ussd.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {
    private String to;
    private String from;
    private String sms;
    private String type;
    private String channel;
    private String api_key;
}