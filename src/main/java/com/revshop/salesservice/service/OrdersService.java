package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.client.IdentityClient;
import com.revshop.salesservice.client.InventoryClient;
import com.revshop.salesservice.client.ShippingServiceClient;
import com.revshop.salesservice.client.NotificationServiceClient;
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
    private final IdentityClient identityClient;
    private final ShippingServiceClient shippingServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public OrdersService(OrdersRepository ordersRepository, CatalogClient catalogClient,
            InventoryClient inventoryClient, CouponService couponService, IdentityClient identityClient,
            ShippingServiceClient shippingServiceClient, NotificationServiceClient notificationServiceClient) {
        this.ordersRepository = ordersRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.couponService = couponService;
        this.identityClient = identityClient;
        this.shippingServiceClient = shippingServiceClient;
        this.notificationServiceClient = notificationServiceClient;
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
        
        // Notify buyer and seller about order placement
        sendOrderPlacedNotifications(savedOrder);

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
        
        Orders.OrderStatus oldStatus = order.getStatus();
        Orders.OrderStatus newStatus = Orders.OrderStatus.valueOf(status.toUpperCase());
        
        order.setStatus(newStatus);
        Orders savedOrder = ordersRepository.save(order);
        OrderResponseDTO response = convertToResponseDTO(savedOrder);
        
        // Send notification and email if status changed
        if (oldStatus != newStatus) {
            sendNotifications(savedOrder, newStatus);
        }
        
        return response;
    }

    private void sendNotifications(Orders order, Orders.OrderStatus status) {
        try {
            // Fetch buyer info
            ApiResponse<UserDTO> userResponse = identityClient.getUserById(order.getUserId());
            if (userResponse != null && userResponse.getData() != null) {
                UserDTO user = userResponse.getData();
                
                // 1. Create Dashboard Notification
                Map<String, Object> notifRequest = new HashMap<>();
                notifRequest.put("userId", order.getUserId());
                notifRequest.put("title", "Order Status Update: " + status);
                notifRequest.put("message", "Your order #" + order.getOrderNumber() + " is now " + status);
                notifRequest.put("type", "ORDER");
                notifRequest.put("targetId", order.getOrderId().toString());
                notificationServiceClient.createNotification(notifRequest);
                
                // 2. Send Email
                Map<String, String> emailRequest = new HashMap<>();
                emailRequest.put("to", user.getEmail());
                emailRequest.put("subject", "RevShop - Order Status Update #" + order.getOrderNumber());
                emailRequest.put("body", "Hello " + user.getName() + ",\n\nYour order #" + order.getOrderNumber() + 
                    " has been updated to: " + status + ".\n\nThank you for shopping with RevShop!");
                notificationServiceClient.sendEmail(emailRequest);
            }

            // ADD SELLER NOTIFICATION FOR DELIVERED
            if (status == Orders.OrderStatus.DELIVERED) {
                List<Long> sellerIds = order.getOrderItems().stream()
                        .map(OrderItems::getSellerId)
                        .distinct()
                        .toList();
                
                for (Long sellerId : sellerIds) {
                    ApiResponse<UserDTO> sellerResponse = identityClient.getUserById(sellerId);
                    if (sellerResponse != null && sellerResponse.getData() != null) {
                        UserDTO seller = sellerResponse.getData();
                        // 1. Notification
                        Map<String, Object> sellerNotif = new HashMap<>();
                        sellerNotif.put("userId", sellerId);
                        sellerNotif.put("title", "Order Delivered Successfully");
                        sellerNotif.put("message", "Order #" + order.getOrderNumber() + " containing your product(s) has been delivered to the customer.");
                        sellerNotif.put("type", "ORDER");
                        sellerNotif.put("targetId", order.getOrderId().toString());
                        notificationServiceClient.createNotification(sellerNotif);

                        // 2. Email
                        Map<String, String> sellerEmail = new HashMap<>();
                        sellerEmail.put("to", seller.getEmail());
                        sellerEmail.put("subject", "RevShop - Order Delivered #" + order.getOrderNumber());
                        sellerEmail.put("body", "Hello " + seller.getName() + ",\n\nOrder #" + order.getOrderNumber() + 
                            " containing your product(s) has been successfully delivered.\n\nThank you for selling on RevShop!");
                        notificationServiceClient.sendEmail(sellerEmail);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send notifications for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private void sendOrderPlacedNotifications(Orders order) {
        try {
            // Notify Buyer
            ApiResponse<UserDTO> buyerResponse = identityClient.getUserById(order.getUserId());
            if (buyerResponse != null && buyerResponse.getData() != null) {
                UserDTO buyer = buyerResponse.getData();
                
                // 1. Create Dashboard Notification
                Map<String, Object> notifRequest = new HashMap<>();
                notifRequest.put("userId", order.getUserId());
                notifRequest.put("title", "Order Placed Successfully");
                notifRequest.put("message", "Your order #" + order.getOrderNumber() + " has been placed successfully.");
                notifRequest.put("type", "ORDER");
                notifRequest.put("targetId", order.getOrderId().toString());
                notificationServiceClient.createNotification(notifRequest);
                
                // 2. Send Email
                Map<String, String> emailRequest = new HashMap<>();
                emailRequest.put("to", buyer.getEmail());
                emailRequest.put("subject", "RevShop - Order Confirmation #" + order.getOrderNumber());
                emailRequest.put("body", "Hello " + buyer.getName() + ",\n\nThank you for your purchase! Your order #" + order.getOrderNumber() + 
                    " has been successfully placed.\n\nThank you for shopping with RevShop!");
                notificationServiceClient.sendEmail(emailRequest);
            }

            // Notify Sellers
            List<Long> sellerIds = order.getOrderItems().stream()
                    .map(OrderItems::getSellerId)
                    .distinct()
                    .toList();
            
            for (Long sellerId : sellerIds) {
                ApiResponse<UserDTO> sellerResponse = identityClient.getUserById(sellerId);
                if (sellerResponse != null && sellerResponse.getData() != null) {
                    UserDTO seller = sellerResponse.getData();
                    
                    // 1. Notification
                    Map<String, Object> sellerNotif = new HashMap<>();
                    sellerNotif.put("userId", sellerId);
                    sellerNotif.put("title", "New Order Received");
                    sellerNotif.put("message", "You have received a new order #" + order.getOrderNumber() + " for your product(s).");
                    sellerNotif.put("type", "ORDER");
                    sellerNotif.put("targetId", order.getOrderId().toString());
                    notificationServiceClient.createNotification(sellerNotif);

                    // 2. Email
                    Map<String, String> sellerEmail = new HashMap<>();
                    sellerEmail.put("to", seller.getEmail());
                    sellerEmail.put("subject", "RevShop - New Order Received #" + order.getOrderNumber());
                    sellerEmail.put("body", "Hello " + seller.getName() + ",\n\nGreat news! You have received a new order #" + order.getOrderNumber() + 
                        " for your product(s).\n\nPlease check your dashboard for details.\n\nThank you for selling on RevShop!");
                    notificationServiceClient.sendEmail(sellerEmail);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send order placed notifications for order {}: {}", order.getOrderId(), e.getMessage());
        }
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
        try {
            ApiResponse<List<Map<String, Object>>> response = shippingServiceClient.getTrackingByOrderId(orderId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch tracking details from shipping service for order: {}", orderId);
        }
        
        // Fallback to initial order placed status if no tracking data yet
        List<Map<String, Object>> tracking = new ArrayList<>();
        Map<String, Object> step = new HashMap<>();
        step.put("status", "ORDER PLACED");
        step.put("createdAt", LocalDateTime.now());
        step.put("description", "Order has been placed successfully.");
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

        // Fetch User info for shipper dashboard (name, phone)
        try {
            ApiResponse<UserDTO> userResponse = identityClient.getUserById(order.getUserId());
            if (userResponse != null && userResponse.getData() != null) {
                UserDTO u = userResponse.getData();
                dto.setCustomerName(u.getName());
                dto.setCustomerPhone(u.getPhone());
                dto.setBuyerName(u.getName()); // For seller dashboard
            }
        } catch (Exception e) {
            log.warn("Identity service error for order mapping: {}", e.getMessage());
        }

        // Fetch Shipper info
        try {
            ApiResponse<ShipperDTO> shipperResponse = shippingServiceClient.getShipperByOrder(order.getOrderId());
            if (shipperResponse != null && shipperResponse.getData() != null) {
                dto.setShipperName(shipperResponse.getData().getName());
            }
        } catch (Exception e) {
            log.warn("Shipping service error for order mapping: {}", e.getMessage());
        }

        // Fetch Address info for shipper dashboard
        if (order.getShippingAddressId() != null) {
            try {
                ApiResponse<AddressDTO> addrResponse = identityClient.getAddressById(order.getShippingAddressId());
                if (addrResponse != null && addrResponse.getData() != null) {
                    dto.setShippingAddress(addrResponse.getData());
                }
            } catch (Exception e) {
                log.warn("Address service error for order mapping: {}", e.getMessage());
            }
        }

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
