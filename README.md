# SKAET USSD Banking System

A USSD-based banking system that enables users to perform banking operations through their mobile phones using USSD codes.

## Features

- Account Management
  - Create Account
  - Check Balance
  - View Transaction History

- Financial Transactions
  - Deposit via Flutterwave USSD
  - Withdraw via Flutterwave
  - Real-time Currency Conversion

- Security
  - PIN-based Authentication
  - Secure Session Management
  - Flutterwave Webhook Verification

## Technologies

- Java 17
- Spring Boot 3.x
- MySQL Database
- Redis for Session Management
- Flutterwave Payment Gateway
- Free Currency API

## Prerequisites

- JDK 17 or higher
- MySQL 8.0
- Redis Server
- Maven
- Flutterwave Account
- Free Currency API Key

## Configuration

### Database Setup
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/skaet_ussd
spring.datasource.username=your_username
spring.datasource.password=your_password