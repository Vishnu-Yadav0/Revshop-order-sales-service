package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.dto.CartDTO;
import com.revshop.salesservice.model.Cart;
import com.revshop.salesservice.model.CartItem;
import com.revshop.salesservice.repository.CartItemRepository;
import com.revshop.salesservice.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(new ArrayList<>());
    }

    @Test
    void getCartByUserId_ExistingCart_Success() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        CartDTO result = cartService.getCartByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(cartRepository, times(1)).findByUserId(userId);
    }

    @Test
    void addItemToCart_NewItem_Success() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        cartService.addItemToCart(userId, 101L, 2);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void removeItemFromCart_Success() {
        CartItem item = new CartItem(cart, 101L, 2);
        cart.getItems().add(item);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        cartService.removeItemFromCart(userId, 101L);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(cart);
    }

    @Test
    void clearCart_Success() {
        cart.getItems().add(new CartItem(cart, 101L, 2));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        cartService.clearCart(userId);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(cart);
    }
}
