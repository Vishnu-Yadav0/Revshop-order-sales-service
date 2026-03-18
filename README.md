![Banner](https://raw.githubusercontent.com/Vishnu-Yadav0/Revshop-order-sales-service/main/banner.png)

# 📋 RevShop — Order & Sales Service

Managing the transactional heart of RevShop: from checkout and order placement to history tracking, status updates, and return management.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=flat-square&logo=docker)](https://www.docker.com/)

---

## Domain Logic

- **Checkout Flow:** Validating cart items, calculating totals, and initiating payment.
- **Order Lifecycle:** Transitions from `PENDING` to `SHIPPED`, `DELIVERED`, etc.
- **Seller Controls:** Dashboard to view sales, update order statuses, and manage returns.
- **Buyer Views:** Order history and detailed status tracking.

## Service Integration

- **User Service:** Validate buyer/seller identity.
- **Product & Inventory:** Verify availability and deduct stock.
- **Payment Service:** Process transaction status.
- **Notification Service:** Send receipts and tracking updates.
- **Shipping Service:** Assign deliveries to shippers.

## Architecture

| Tech | Usage |
|---|---|
| Java 17 | Core Logic |
| MySQL | Relational Data storage |
| OpenFeign | Resilience-focused inter-service calls |
| resilience4j | Fault tolerance patterns |

---

## Explore the Project

- [Revshop-frontend](https://github.com/Vishnu-Yadav0/Revshop-frontend)
- [Revshop-api-gateway](https://github.com/Vishnu-Yadav0/Revshop-api-gateway)
- [Revshop-user-service](https://github.com/Vishnu-Yadav0/Revshop-user-service)

