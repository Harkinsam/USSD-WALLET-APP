package com.skaet.ussd.controller;

import com.skaet.ussd.account.service.AccountService;
import com.skaet.ussd.currency.CurrencyService;
import com.skaet.ussd.session.UssdSession;
import com.skaet.ussd.session.UssdSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ussd")
@RequiredArgsConstructor
@Slf4j
public class UssdController {
    private final AccountService accountService;
    private final UssdSessionService sessionService;
    private final CurrencyService  currencyService;

    @PostMapping
    public String handleUssd(@RequestParam String phoneNumber,
                           @RequestParam String text,
                           @RequestParam String sessionId) {
        String formattedPhone = formatPhoneNumber(phoneNumber);
        
        if (text.isEmpty()) {
            return handleInitialRequest(sessionId, formattedPhone);
        }
        
        return handleMenuNavigation(sessionId, formattedPhone, text);
    }

    private String formatPhoneNumber(String phoneNumber) {
        return phoneNumber.startsWith("234") ? "0" + phoneNumber.substring(3) : phoneNumber;
    }

    private String handleInitialRequest(String sessionId, String phoneNumber) {
        // Create new session with MTN-provided sessionId
        UssdSession session = new UssdSession();
        session.setSessionId(sessionId);
        session.setPhoneNumber(phoneNumber);
        session.setData(new HashMap<>());
        session.getData().put("menuLevel", "1");
        
        // Save the new session
        sessionService.saveSession(session);
        return getMainMenu();
    }

    private String handleMenuNavigation(String sessionId, String phoneNumber, String text) {
        if (text == null || sessionId == null) {
            return "END Invalid session";
        }

        // Retrieve existing session using MTN sessionId
        UssdSession session = sessionService.getSession(sessionId, phoneNumber);
        if (session == null) {
            return "END Session expired. Please start again.";
        }

        String menuLevel = session.getData().getOrDefault("menuLevel", "1");
        String[] inputs = text.split("\\*");

        String response = processMenuLevel(session, menuLevel, inputs);
        handleSessionState(sessionId, session, response);
        
        return response;
    }

    private String processMenuLevel(UssdSession session, String menuLevel, String[] inputs) {
        return switch (menuLevel) {
            case "1" -> handleMainMenu(session, inputs);
            case "2" -> handleSubMenu(session, inputs);
            case "3" -> handleFinalStep(session, inputs);
            default -> "END Invalid session state";
        };
    }

    private void handleSessionState(String sessionId, UssdSession session, String response) {
        if (response.startsWith("END")) {
            sessionService.clearSession(sessionId);
        } else {
            sessionService.saveSession(session);
        }
    }

    private String getMainMenu() {
        return """
       CON Welcome to SKAET USSD Banking
       1. Create Account
       2. Check Balance
       3. Deposit
       4. Withdraw
       5. Currency Converter
       6. Exit""";
    }


    private String handleMainMenu(UssdSession session, String[] inputs) {
        Map<String, String> data = session.getData();
        data.put("menuLevel", "2");

        return switch (inputs[0]) {
            case "1" -> handleCreateAccountOption(data);
            case "2" -> handleBalanceCheckOption(data);
            case "3" -> handleDepositOption(data);
            case "4" -> handleWithdrawOption(data);
            case "5" -> handleCurrencyConverterOption(data);
            case "6" -> "END Thank you for using our service";
            default -> "END Invalid option selected";
        };
    }


    private String handleDepositOption(Map<String, String> data) {
        data.put("currentFlow", "deposit");
        return """
               CON Select deposit method:
               1. Flutterwave USSD
               0. Back to Main Menu""";
    }

    private String handleWithdrawOption(Map<String, String> data) {
        data.put("currentFlow", "withdraw");
        return """
           CON Select withdrawal method:
           1. Flutterwave
           0. Back to Main Menu""";
    }


    private String handleSubMenu(UssdSession session, String[] inputs) {
        String currentFlow = session.getData().get("currentFlow");

        return switch (currentFlow) {
            case "account_creation" -> handleAccountCreation(session, inputs);
            case "balance" -> accountService.checkBalance(session.getPhoneNumber(), inputs[1]);
            case "deposit" -> handleDepositFlow(session, inputs);
            case "withdraw" -> handleWithdrawFlow(session, inputs);
            case "currency_converter" -> handleCurrencyConversionFlow(session, inputs);
            default -> "END Invalid menu option";
        };
    }

    private String handleCurrencyConverterOption(Map<String, String> data) {
        data.put("currentFlow", "currency_converter");
        return """
               CON Select base currency:
               1. NGN
               2. USD
               3. EUR
               4. GBP
               0. Back to Main Menu""";
    }

    private String handleCurrencyConversionFlow(UssdSession session, String[] inputs) {
        Map<String, String> data = session.getData();
        
        if (inputs.length == 2) {
            String selection = inputs[1];
            String currency = switch (selection) {
                case "1" -> "NGN";
                case "2" -> "USD";
                case "3" -> "EUR";
                case "4" -> "GBP";
                case "0" -> { yield getMainMenu(); }
                default -> null;
            };
            
            if (currency == null) {
                return "END Invalid currency selected";
            }
            
            data.put("baseCurrency", currency);
            return """
                   CON Select target currency:
                   1. NGN
                   2. USD
                   3. EUR
                   4. GBP""";
        }

        if (inputs.length == 3) {
            String targetCurrency = switch (inputs[2]) {
                case "1" -> "NGN";
                case "2" -> "USD";
                case "3" -> "EUR";
                case "4" -> "GBP";
                default -> null;
            };
            
            if (targetCurrency == null) {
                return "END Invalid target currency";
            }
            
            data.put("targetCurrency", targetCurrency);
            return "CON Enter amount to convert:";
        }

        if (inputs.length == 4) {
            try {
                BigDecimal amount = new BigDecimal(inputs[3]);
                String baseCurrency = data.get("baseCurrency");
                String targetCurrency = data.get("targetCurrency");
                
                BigDecimal convertedAmount = currencyService.convert(baseCurrency, targetCurrency, amount);
                
                return String.format("END %s %s = %s %s", 
                    amount, baseCurrency, 
                    convertedAmount, targetCurrency);
            } catch (NumberFormatException e) {
                return "END Invalid amount entered";
            } catch (Exception e) {
                log.error("Currency conversion failed: {}", e.getMessage());
                return "END Currency conversion failed. Please try again.";
            }
        }

        return "END Invalid input sequence";
    }

    private String handleWithdrawFlow(UssdSession session, String[] inputs) {
        if (inputs.length < 2) {
            return "END Invalid input format";
        }

        String userSelection = inputs[1];

        if (inputs.length == 2) {
            switch (userSelection) {
                case "1":
                    session.getData().put("withdrawMethod", "flutterwave");
                    return "CON Enter amount to withdraw:";
                case "0":
                    return getMainMenu();
                default:
                    return "END Invalid option selected";
            }
        }

        if (inputs.length == 3) {
            try {
                double amount = Double.parseDouble(inputs[2]);
                if (amount <= 0) {
                    return "END Invalid amount. Please enter a positive number.";
                }
                session.getData().put("amount", String.valueOf(amount));
                return "CON Enter your PIN:";
            } catch (NumberFormatException e) {
                return "END Invalid amount entered. Please enter a numeric value.";
            }
        }

        if (inputs.length == 4) {
            return handleFinalStep(session, inputs);
        }

        return "END Invalid input sequence";
    }



    private String handleDepositFlow(UssdSession session, String[] inputs) {
        if (inputs.length < 2) {
            return "END Invalid input format";
        }

        String userSelection = inputs[1];

        if (inputs.length == 2) {
            switch (userSelection) {
                case "1":
                    session.getData().put("depositMethod", "flutterwave");
                    return "CON Enter amount to deposit:";
                case "0":
                    return getMainMenu();
                default:
                    return "END Invalid option selected";
            }
        }

        if (inputs.length == 3) {
            try {
                double amount = Double.parseDouble(inputs[2]);
                if (amount <= 0) {
                    return "END Invalid amount. Please enter a positive number.";
                }

                // Process the deposit immediately after the amount is entered
                return accountService.handleDeposit(session.getPhoneNumber(), String.valueOf(amount));
            } catch (NumberFormatException e) {
                return "END Invalid amount entered. Please enter a numeric value.";
            }
        }

        return "END Invalid input sequence";
    }



    private String handleFinalStep(UssdSession session, String[] inputs) {
        if (inputs.length < 2) { // Updated condition to allow just amount input
            return "END Invalid input format";
        }

        String currentFlow = session.getData().get("currentFlow");

        return switch (currentFlow) {
            case "deposit" -> {
                try {
                    double amount = Double.parseDouble(inputs[1]); // Extract deposit amount
                    if (amount <= 0) {
                        yield "END Invalid amount. Please enter a positive number.";
                    }

                    // Process the deposit immediately without requiring a PIN
                    yield accountService.handleDeposit(session.getPhoneNumber(), String.valueOf(amount));
                } catch (NumberFormatException e) {
                    yield "END Invalid amount entered. Please enter a numeric value.";
                }
            }
            case "balance" -> {
                if (inputs.length < 3) {
                    yield "END Invalid input format for balance check.";
                }
                yield accountService.checkBalance(session.getPhoneNumber(), inputs[inputs.length - 1]);
            }
            default -> "END Invalid operation";
        };
    }



    private String handleAccountCreation(UssdSession session, String[] inputs) {
        Map<String, String> data = session.getData();
        String lastInput = inputs[inputs.length - 1];

        if (!data.containsKey("firstName")) {
            data.put("firstName", lastInput);
            return "CON Enter your last name:";
        }

        if (!data.containsKey("lastName")) {
            data.put("lastName", lastInput);
            return "CON Create your 4-digit PIN:";
        }

        return accountService.createAccount(
            session.getPhoneNumber(),
            data.get("firstName"),
            data.get("lastName"),
            lastInput
        );
    }



    private String handleCreateAccountOption(Map<String, String> data) {
        data.put("currentFlow", "account_creation");
        return "CON Enter your first name:";
    }

    private String handleBalanceCheckOption(Map<String, String> data) {
        data.put("currentFlow", "balance");
        return "CON Enter your PIN:";
    }

//    private String handleCurrencyConverterOption(Map<String, String> data) {
//        data.put("currentFlow", "currency_converter");
//        return "CON Enter the currency to convert from (e.g., USD):";
//    }
//
//    private String handleCurrencyConversionFlow(UssdSession session, String[] inputs) {
//        Map<String, String> data = session.getData();
//
//        if (inputs.length < 2) {
//            return "END Invalid input. Please follow the correct format.";
//        }
//
//        if (!data.containsKey("fromCurrency")) {
//            data.put("fromCurrency", inputs[1].toUpperCase()); // Store base currency
//            return "CON Enter the currency to convert to (e.g., NGN):";
//        }
//
//        if (!data.containsKey("toCurrency")) {
//            data.put("toCurrency", inputs[1].toUpperCase()); // Store target currency
//            return "CON Enter the amount to convert:";
//        }
//
//        if (!data.containsKey("amount")) {
//            try {
//                double amount = Double.parseDouble(inputs[1]);
//                if (amount <= 0) return "END Invalid amount. Please enter a positive number.";
//
//                data.put("amount", String.valueOf(amount));
//
//                // Convert currency
//                BigDecimal convertedAmount = currencyService.convert(
//                        data.get("fromCurrency"),
//                        data.get("toCurrency"),
//                        BigDecimal.valueOf(amount)
//                );
//
//                return "END " + amount + " " + data.get("fromCurrency") + " = " +
//                        convertedAmount + " " + data.get("toCurrency");
//            } catch (NumberFormatException e) {
//                return "END Invalid amount entered. Please enter a numeric value.";
//            }
//        }
//
//        return "END Invalid input sequence";
//    }


}
