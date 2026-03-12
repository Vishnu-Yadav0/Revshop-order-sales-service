package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.client.InventoryClient;
import com.revshop.salesservice.dto.*;
import com.revshop.salesservice.model.Orders;
import com.revshop.salesservice.repository.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrdersServiceTest {

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrdersService ordersService;

    private OrderRequestDTO orderRequest;
    private ProductDTO product;

    @BeforeEach
    void setUp() {
        orderRequest = new OrderRequestDTO();
        orderRequest.setUserId(1L);
        orderRequest.setShippingAddressId(1L);
        orderRequest.setBillingAddressId(1L);
        orderRequest.setPaymentMethod("CARD");
        
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(101L);
        item.setQuantity(2);
        orderRequest.setItems(Collections.singletonList(item));

        product = new ProductDTO();
        product.setProductId(101L);
        product.setName("Test Product");
        product.setSellingPrice(new BigDecimal("100.00"));
    }

    @Test
    void placeOrder_Success() {
        ApiResponse<ProductDTO> apiResponse = new ApiResponse<>();
        apiResponse.setData(product);

        when(catalogClient.getProductById(101L)).thenReturn(apiResponse);
        when(ordersRepository.save(any(Orders.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO result = ordersService.placeOrder(orderRequest);

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getTotalAmount());
        verify(inventoryClient, times(1)).reserveStock(101L, 2);
        verify(ordersRepository, times(1)).save(any(Orders.class));
    }

    @Test
    void getOrdersByUserId_Success() {
        Orders order = new Orders();
        order.setUserId(1L);
        order.setOrderItems(Collections.emptyList());
        order.setStatus(Orders.OrderStatus.PENDING);
        
        when(ordersRepository.findByUserId(1L)).thenReturn(Collections.singletonList(order));

        List<OrderResponseDTO> result = ordersService.getOrdersByUserId(1L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void updateOrderStatus_Success() {
        Orders order = new Orders();
        order.setOrderId(1L);
        order.setStatus(Orders.OrderStatus.PENDING);
        order.setOrderItems(Collections.emptyList());

        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));
        when(ordersRepository.save(any(Orders.class))).thenReturn(order);

        OrderResponseDTO result = ordersService.updateOrderStatus(1L, "SHIPPED");

        assertNotNull(result);
        assertEquals("SHIPPED", result.getStatus());
        verify(ordersRepository, times(1)).save(order);
    }
}
