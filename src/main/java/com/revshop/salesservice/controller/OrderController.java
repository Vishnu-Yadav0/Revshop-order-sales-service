package com.revshop.salesservice.controller;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.OrderRequestDTO;
import com.revshop.salesservice.dto.OrderResponseDTO;
import com.revshop.salesservice.service.OrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrdersService ordersService;

    public OrderController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/place")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(@RequestBody OrderRequestDTO request) {
        log.info("POST /api/orders/place");
        return ResponseEntity.ok(new ApiResponse<OrderResponseDTO>("Order placed successfully", ordersService.placeOrder(request)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getUserOrders(@PathVariable Long userId) {
        log.info("GET /api/orders/user/{}", userId);
        return ResponseEntity.ok(new ApiResponse<List<OrderResponseDTO>>("Orders fetched successfully", ordersService.getOrdersByUserId(userId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/orders/{}", orderId);
        return ResponseEntity.ok(new ApiResponse<OrderResponseDTO>("Order fetched successfully", ordersService.getOrderById(orderId)));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(@PathVariable Long orderId, @RequestParam String status) {
        log.info("PUT /api/orders/{}/status", orderId);
        return ResponseEntity.ok(new ApiResponse<OrderResponseDTO>("Status updated successfully", ordersService.updateOrderStatus(orderId, status)));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        log.info("PUT /api/orders/{}/cancel", orderId);
        return ResponseEntity.ok(new ApiResponse<OrderResponseDTO>("Order cancelled successfully", ordersService.cancelOrder(orderId, userId)));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrdersBySeller(@PathVariable Long sellerId) {
        log.info("GET /api/orders/seller/{}", sellerId);
        return ResponseEntity.ok(new ApiResponse<List<OrderResponseDTO>>("Seller orders fetched", ordersService.getOrdersBySeller(sellerId)));
    }

    @GetMapping("/seller/{sellerId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSellerStats(@PathVariable Long sellerId) {
        log.info("GET /api/orders/seller/{}/stats", sellerId);
        return ResponseEntity.ok(new ApiResponse<Map<String, Object>>("Seller stats fetched", ordersService.getSellerStats(sellerId)));
    }

    @PutMapping("/{orderId}/return")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> requestReturn(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String reason = (String) payload.get("reason");
        log.info("PUT /api/orders/{}/return", orderId);
        return ResponseEntity.ok(new ApiResponse<OrderResponseDTO>("Return requested", ordersService.requestReturn(orderId, userId, reason)));
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrderTracking(@PathVariable Long orderId) {
        log.info("GET /api/orders/{}/tracking", orderId);
        return ResponseEntity.ok(new ApiResponse<List<Map<String, Object>>>("Tracking details fetched", ordersService.getOrderTracking(orderId)));
    }
}
