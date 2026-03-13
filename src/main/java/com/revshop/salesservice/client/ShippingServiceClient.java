package com.revshop.salesservice.client;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.ShipperDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "shipping-service")
public interface ShippingServiceClient {

    @GetMapping("/api/shippers/order/{orderId}")
    ApiResponse<ShipperDTO> getShipperByOrder(@PathVariable("orderId") Long orderId);

    @GetMapping("/api/tracking/order/{orderId}")
    ApiResponse<java.util.List<java.util.Map<String, Object>>> getTrackingByOrderId(@PathVariable("orderId") Long orderId);
}
