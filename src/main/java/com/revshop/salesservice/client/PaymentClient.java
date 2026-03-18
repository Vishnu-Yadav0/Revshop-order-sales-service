package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "payment-service", url = "${payment-service.url:http://localhost:8085}")
public interface PaymentClient {

    @PostMapping("/api/wallets/deduct")
    ApiResponse<Boolean> deductFromWallet(@RequestHeader("X-User-Id") Long userId, @RequestBody Map<String, Object> request);
}
