package com.revshop.salesservice.controller;

import com.revshop.salesservice.dto.CartDTO;
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
    public ResponseEntity<CartDTO> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/user/{userId}/add")
    public ResponseEntity<Void> addItem(@PathVariable Long userId, @RequestParam Long productId,
            @RequestParam Integer quantity) {
        cartService.addItemToCart(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/remove/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long userId, @PathVariable Long productId) {
        cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }
}
