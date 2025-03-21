package com.skaet.ussd.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UssdSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private String phoneNumber;
    private String currentMenu;
    private Map<String, String> data = new HashMap<>();
    private int level;
}