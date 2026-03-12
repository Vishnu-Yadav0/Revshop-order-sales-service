package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.client.InventoryClient;
import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.*;
import com.revshop.salesservice.exception.ResourceNotFoundException;
import com.revshop.salesservice.model.OrderItems;
import com.revshop.salesservice.model.Orders;
import com.revshop.salesservice.repository.OrdersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrdersService {

    private static final Logger log = LoggerFactory.getLogger(OrdersService.class);

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
        log.info("Placing order for user {}", request.getUserId());
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
            ApiResponse<ProductDTO> productResponse = catalogClient.getProductById(itemReq.getProductId());
            if (productResponse == null || productResponse.getData() == null)
                throw new ResourceNotFoundException("Product not found: " + itemReq.getProductId());

            ProductDTO product = productResponse.getData();

            inventoryClient.reserveStock(itemReq.getProductId(), itemReq.getQuantity());

            BigDecimal price = product.getSellingPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            // Fetch sellerId from product catalog
            Long sellerId = product.getSellerId();
            if (sellerId == null) throw new RuntimeException("Seller not identified for product " + itemReq.getProductId());

            OrderItems orderItem = new OrderItems(order, itemReq.getProductId(), sellerId, itemReq.getQuantity(), price);
            orderItemsList.add(orderItem);
        }

        order.setOrderItems(orderItemsList);

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            try {
                BigDecimal discount = couponService.validateAndCalculateDiscount(request.getCouponCode(), totalAmount);
                order.setDiscountAmount(discount);
                totalAmount = totalAmount.subtract(discount);
                couponService.applyCoupon(request.getCouponCode());
            } catch (Exception e) {
                log.warn("Coupon apply failed: {}", e.getMessage());
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

    public OrderResponseDTO getOrderById(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return convertToResponseDTO(order);
    }

    public OrderResponseDTO updateOrderStatus(Long orderId, String status) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(Orders.OrderStatus.valueOf(status.toUpperCase()));
        return convertToResponseDTO(ordersRepository.save(order));
    }

    public OrderResponseDTO cancelOrder(Long orderId, Long userId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        if (order.getStatus() == Orders.OrderStatus.DELIVERED || order.getStatus() == Orders.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(Orders.OrderStatus.CANCELLED);
        return convertToResponseDTO(ordersRepository.save(order));
    }

    public List<OrderResponseDTO> getOrdersBySeller(Long sellerId) {
        return ordersRepository.findDistinctByOrderItems_SellerId(sellerId).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSellerStats(Long sellerId) {
        List<Orders> orders = ordersRepository.findDistinctByOrderItems_SellerId(sellerId);
        
        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() == Orders.OrderStatus.DELIVERED)
                .count();
        
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == Orders.OrderStatus.DELIVERED)
                .flatMap(o -> o.getOrderItems().stream())
                .filter(oi -> oi.getSellerId().equals(sellerId))
                .map(oi -> oi.getPriceAtPurchase().multiply(BigDecimal.valueOf(oi.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("totalRevenue", totalRevenue);
        return stats;
    }

    public OrderResponseDTO requestReturn(Long orderId, Long userId, String reason) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (order.getStatus() != Orders.OrderStatus.DELIVERED) {
            throw new RuntimeException("Can only return delivered orders");
        }

        order.setStatus(Orders.OrderStatus.RETURN_REQUESTED);
        // In real app, save reason to a ReturnRequest table
        return convertToResponseDTO(ordersRepository.save(order));
    }

    public List<Map<String, Object>> getOrderTracking(Long orderId) {
        // Placeholder linking to monolith concept. 
        // In microservices, this might call shipping-service.
        List<Map<String, Object>> tracking = new ArrayList<>();
        Map<String, Object> step = new HashMap<>();
        step.put("status", "Order Placed");
        step.put("timestamp", LocalDateTime.now());
        tracking.add(step);
        return tracking;
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
