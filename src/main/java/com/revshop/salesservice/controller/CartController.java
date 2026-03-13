package com.revshop.salesservice.controller;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.CartDTO;
import com.revshop.salesservice.dto.OrderItemRequestDTO;
import com.revshop.salesservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CartDTO>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(new ApiResponse<CartDTO>("Cart fetched successfully", cartService.getCartByUserId(userId)));
    }

    @PostMapping("/user/{userId}/add")
    public ResponseEntity<ApiResponse<CartDTO>> addItem(@PathVariable Long userId, @RequestBody OrderItemRequestDTO request) {
        cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(new ApiResponse<CartDTO>("Item added to cart", cartService.getCartByUserId(userId)));
    }

    @DeleteMapping("/user/{userId}/remove/{productId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(new ApiResponse<CartDTO>("Item removed from cart", cartService.getCartByUserId(userId)));
    }

    @PutMapping("/user/{userId}/update")
    public ResponseEntity<ApiResponse<CartDTO>> updateQuantity(@PathVariable Long userId, @RequestBody OrderItemRequestDTO request) {
        cartService.updateItemQuantity(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(new ApiResponse<CartDTO>("Quantity updated", cartService.getCartByUserId(userId)));
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(new ApiResponse<Void>("Cart cleared successfully", null));
    }
}
