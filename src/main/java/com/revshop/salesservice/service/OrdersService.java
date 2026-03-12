package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.client.InventoryClient;
import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.*;
import com.revshop.salesservice.model.OrderItems;
import com.revshop.salesservice.model.Orders;
import com.revshop.salesservice.repository.OrdersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrdersService {

    private final OrdersRepository ordersRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final CouponService couponService;

    public OrdersService(OrdersRepository ordersRepository, CatalogClient catalogClient,
            InventoryClient inventoryClient, CouponService couponService) {
        this.ordersRepository = ordersRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.couponService = couponService;
    }

    public OrderResponseDTO placeOrder(OrderRequestDTO request) {
        Orders order = new Orders();
        order.setUserId(request.getUserId());
        order.setShippingAddressId(request.getShippingAddressId());
        order.setBillingAddressId(request.getBillingAddressId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(Orders.OrderStatus.PENDING);
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setPaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItems> orderItemsList = new ArrayList<>();

        for (OrderItemRequestDTO itemReq : request.getItems()) {
            // 1. Fetch Price from Catalog Service
            ApiResponse<ProductDTO> productResponse = catalogClient.getProductById(itemReq.getProductId());
            if (productResponse == null || productResponse.getData() == null)
                throw new RuntimeException("Product not found: " + itemReq.getProductId());

            ProductDTO product = productResponse.getData();

            // 2. Reserve Stock in Inventory Service
            inventoryClient.reserveStock(itemReq.getProductId(), itemReq.getQuantity());

            BigDecimal price = product.getSellingPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItems orderItem = new OrderItems(order, itemReq.getProductId(), itemReq.getQuantity(), price);
            orderItemsList.add(orderItem);
        }

        order.setOrderItems(orderItemsList);

        // 3. Apply Coupon if present
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            try {
                BigDecimal discount = couponService.validateAndCalculateDiscount(request.getCouponCode(), totalAmount);
                order.setDiscountAmount(discount);
                totalAmount = totalAmount.subtract(discount);
                couponService.applyCoupon(request.getCouponCode());
            } catch (Exception e) {
                // For simplicity, log and skip coupon if invalid during placement
                order.setDiscountAmount(BigDecimal.ZERO);
            }
        } else {
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        order.setTotalAmount(totalAmount);
        Orders savedOrder = ordersRepository.save(order);

        return convertToResponseDTO(savedOrder);
    }

    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        return ordersRepository.findByUserId(userId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public OrderResponseDTO updateOrderStatus(Long orderId, String status) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(Orders.OrderStatus.valueOf(status.toUpperCase()));
        return convertToResponseDTO(ordersRepository.save(order));
    }

    private OrderResponseDTO convertToResponseDTO(Orders order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setStatus(order.getStatus().name());
        dto.setOrderDate(order.getOrderDate());
        dto.setPaymentMethod(order.getPaymentMethod());

        List<OrderItemResponseDTO> itemDTOs = order.getOrderItems().stream().map(item -> {
            OrderItemResponseDTO itemDTO = new OrderItemResponseDTO();
            itemDTO.setProductId(item.getProductId());
            itemDTO.setQuantity(item.getQuantity());
            itemDTO.setPriceAtPurchase(item.getPriceAtPurchase());
            itemDTO.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));

            // Optionally fetch current name from Catalog if needed, or rely on snapshot
            // strategy
            try {
                ApiResponse<ProductDTO> productResponse = catalogClient.getProductById(item.getProductId());
                if (productResponse != null && productResponse.getData() != null) {
                    ProductDTO p = productResponse.getData();
                    itemDTO.setProductName(p.getName());
                }
            } catch (Exception e) {
                itemDTO.setProductName("Product #" + item.getProductId());
            }

            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        return dto;
    }
}
