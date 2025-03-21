package com.skaet.ussd.currency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${currency.api.key}")
    private String apiKey;

    @Value("${currency.api.base-url}")
    private String baseUrl;

    private static final BigDecimal DEFAULT_RATE = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);

    public BigDecimal getExchangeRate(String baseCurrency, String targetCurrency) {

        String url = String.format("%s/latest?apikey=%s&base_currency=%s&currencies=%s",
                baseUrl, apiKey, baseCurrency, targetCurrency);

        try {
            log.info("Fetching exchange rate: {} -> {}", baseCurrency, targetCurrency);
            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            
            if (data != null && data.has(targetCurrency)) {
                return BigDecimal.valueOf(data.get(targetCurrency).asDouble())
                        .setScale(4, RoundingMode.HALF_UP);
            }
            
            log.error("Failed to get exchange rate from response: {}", response);
            return DEFAULT_RATE;
        } catch (Exception e) {
            log.error("Failed to fetch exchange rate: {}", e.getMessage());
            return DEFAULT_RATE;
        }
    }

    public BigDecimal convert(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
