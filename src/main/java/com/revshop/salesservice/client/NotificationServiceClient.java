package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications")
    ApiResponse<Object> createNotification(@RequestBody Map<String, Object> request);

    @PostMapping("/api/notifications/email")
    ApiResponse<Void> sendEmail(@RequestBody Map<String, String> request);
}
