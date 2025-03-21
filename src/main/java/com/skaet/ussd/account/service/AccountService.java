package com.skaet.ussd.account.service;

import com.skaet.ussd.account.entity.Account;
import com.skaet.ussd.account.repository.AccountRepository;

import com.skaet.ussd.notification.service.SmsNotificationService;
import com.skaet.ussd.payment.gateway.PaymentGateway;
import com.skaet.ussd.wallet.model.Wallet;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentGateway paymentService;
    private final SmsNotificationService termiiService;



    public String createAccount(String phoneNumber, String firstName, String lastName, String pin) {
        try {
            if (accountRepository.existsByPhoneNumber(phoneNumber)) {
                return "END Account already exists";
            }

            if (!isValidPin(pin)) {
                return "END PIN must be 4 digits and contain only numbers";
            }

            Account account = new Account();
            account.setPhoneNumber(phoneNumber);
            account.setFirstName(firstName);
            account.setLastName(lastName);
            account.setPin(passwordEncoder.encode(pin));

            Wallet wallet = new Wallet();
            wallet.setBalance(BigDecimal.ZERO);
            account.setWallet(wallet);

            accountRepository.save(account);

            // Send welcome SMS
            String message = String.format("Welcome to SKAET Banking, %s! Your account has been created successfully.", firstName);
            termiiService.sendSms(phoneNumber, message);

            return "END Account created successfully";
        } catch (Exception e) {
            log.error("Account creation failed: {}", e.getMessage());
            return "END Account creation failed. Please try again";
        }
    }

    @Transactional
    public String handleDeposit(String phoneNumber, String amountStr) {
        try {
            Account account = accountRepository.findByPhoneNumber(phoneNumber)
                    .orElse(null);
            if (account == null) {
                return "END Account not found";
            }


            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                return "END Invalid amount format";
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "END Invalid amount. Amount must be greater than 0";
            }

            String gateway = "flutterwave"; // Hardcoded for now, adjust if needed
            return processDeposit(phoneNumber, amount, gateway, account);

        } catch (Exception e) {
            log.error("Deposit error: {}", e.getMessage());
            return "END Transaction failed. Please try again";
        }
    }


    private String processDeposit(String phoneNumber, BigDecimal amount, String gateway, Account account) {
        return switch (gateway.toLowerCase()) {
            case "flutterwave" -> {
                String transactionRef = paymentService.initiateDeposit(phoneNumber, amount);
                if (transactionRef.equals("FAILED")) {
                    yield "END Deposit failed. Please try again later.";
                }
                
                yield String.format("""
                    END Deposit initiated via Flutterwave
                    Amount: NGN %s
                    Reference: %s
                    Check your SMS for payment instructions""", 
                    amount, transactionRef);
            }
            default -> "END Unsupported payment method";
        };
    }

    @Transactional
    public String handleWithdrawal(String phoneNumber, String amountStr, String bankName, String accountNumber, String gateway) {
        try {
            Optional<Account> optionalAccount = accountRepository.findByPhoneNumber(phoneNumber);

            if (optionalAccount.isEmpty()) {
                return "END Account not found";
            }

            Account account = optionalAccount.get();
            Wallet wallet = account.getWallet();

            if (wallet == null) {
                return "END Wallet not found";
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                return "END Invalid amount format";
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "END Invalid amount. Amount must be greater than 0";
            }

            if (wallet.getBalance().compareTo(amount) < 0) {
                return "END Insufficient balance.";
            }

            // Do not deduct balance here, wait for webhook
            return processWithdraw(phoneNumber, amount, bankName, accountNumber, gateway);

        } catch (Exception e) {
            log.error("Withdrawal error: {}", e.getMessage());
            return "END Transaction failed. Please try again";
        }
    }




    private String processWithdraw(String phoneNumber, BigDecimal amount, String bankName, String accountNumber, String gateway) {
        return switch (gateway.toLowerCase()) {
            case "flutterwave" -> {
                String transactionRef = paymentService.initiateWithdrawal(phoneNumber, amount, bankName, accountNumber);
                if ("FAILED".equals(transactionRef)) {
                    yield "END Withdrawal failed. Please try again later.";
                }

                // Do NOT deduct wallet balance here; webhook will handle it
                yield String.format("""
                END Withdrawal initiated via Flutterwave
                Amount: NGN %s
                Reference: %s
                You will receive a confirmation SMS once processed""",
                        amount, transactionRef);
            }
            default -> "END Unsupported payment method";
        };
    }



    private boolean isValidPin(String pin) {
        return pin != null && pin.length() == 4 && pin.matches("\\d+");
    }

    @Transactional
    public void creditAccount(String phoneNumber, BigDecimal amount) {
        Optional<Account> existingAccount = accountRepository.findByPhoneNumber(phoneNumber);

        if (existingAccount.isEmpty()) {
            log.warn("Account not found for phone number: {}", phoneNumber);
            return;
        }

        Account account = existingAccount.get();
        Wallet wallet = account.getWallet();
        wallet.setBalance(wallet.getBalance().add(amount));
        accountRepository.save(account);

        log.info("Account credited successfully: {} - Amount: {}", phoneNumber, amount);

    }

    @Transactional
    public void debitAccount(String phoneNumber, BigDecimal amount) {
        Optional<Account> existingAccount = accountRepository.findByPhoneNumber(phoneNumber);

        if (existingAccount.isEmpty()) {
            log.warn("Account not found for phone number: {}", phoneNumber);
            return;
        }

        Account account = existingAccount.get();
        Wallet wallet = account.getWallet();
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for user: {}", phoneNumber);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Account debited successfully: {} - Amount: {}", phoneNumber, amount);
    }



    public String checkBalance(String phoneNumber, String pin) {
        try {
            Account account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElse(null);
            if (account == null) {
                return "END Account not found";
            }

            if (!passwordEncoder.matches(pin, account.getPin())) {
                return "END Invalid PIN";
            }

            return String.format("END Your balance is NGN %s", 
                account.getWallet().getBalance().toString());
                
        } catch (Exception e) {
            log.error("Balance check failed: {}", e.getMessage());
            return "END Unable to check balance. Please try again";
        }
    }
}