package com.revshop.salesservice.controller;

import com.revshop.salesservice.dto.OrderRequestDTO;
import com.revshop.salesservice.dto.OrderResponseDTO;
import com.revshop.salesservice.service.OrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrdersService ordersService;

    public OrderController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @PostMapping("/place")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody OrderRequestDTO request) {
        return ResponseEntity.ok(ordersService.placeOrder(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(ordersService.getOrdersByUserId(userId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(@PathVariable Long orderId, @RequestParam String status) {
        return ResponseEntity.ok(ordersService.updateOrderStatus(orderId, status));
    }
}
