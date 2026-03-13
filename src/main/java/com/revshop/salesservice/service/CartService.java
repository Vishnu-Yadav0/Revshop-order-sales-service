package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.CartDTO;
import com.revshop.salesservice.dto.CartItemDTO;
import com.revshop.salesservice.dto.ProductDTO;
import com.revshop.salesservice.model.Cart;
import com.revshop.salesservice.model.CartItem;
import com.revshop.salesservice.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CatalogClient catalogClient;

    public CartService(CartRepository cartRepository, CatalogClient catalogClient) {
        this.cartRepository = cartRepository;
        this.catalogClient = catalogClient;
    }

    private Cart getOrCreateCart(Long userId) {
        List<Cart> carts = cartRepository.findByUserId(userId);
        if (carts.isEmpty()) {
            return cartRepository.save(new Cart(userId));
        }
        // If multiple carts exist (legacy issue), return the first one
        return carts.get(0);
    }

    public CartDTO getCartByUserId(Long userId) {
        return convertToDTO(getOrCreateCart(userId));
    }

    public void addItemToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        if (cart.getItems() == null) {
            cart.setItems(new java.util.ArrayList<>());
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem(cart, productId, quantity);
            cart.getItems().add(newItem);
        }
        cartRepository.save(cart);
    }

    public void updateItemQuantity(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setQuantity(quantity);
                    cartRepository.save(cart);
                });
    }

    public void removeItemFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setCartId(cart.getCartId());
        dto.setUserId(cart.getUserId());

        List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
            CartItemDTO itemDTO = new CartItemDTO();
            itemDTO.setCartItemId(item.getCartItemId());
            itemDTO.setProductId(item.getProductId());
            itemDTO.setQuantity(item.getQuantity());

            try {
                ApiResponse<ProductDTO> productResponse = catalogClient.getProductById(item.getProductId());
                if (productResponse != null && productResponse.getData() != null) {
                    ProductDTO product = productResponse.getData();
                    itemDTO.setProductName(product.getName());
                    itemDTO.setPrice(product.getSellingPrice());
                    itemDTO.setImageUrl(product.getImageUrl());
                }
            } catch (Exception e) {
                itemDTO.setProductName("Product Details Unavailable");
            }
            return itemDTO;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);

        BigDecimal total = itemDTOs.stream()
                .filter(i -> i.getPrice() != null)
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setTotalPrice(total);
        return dto;
    }
}
