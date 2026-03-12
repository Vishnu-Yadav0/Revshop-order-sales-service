package com.revshop.salesservice.service;

import com.revshop.salesservice.client.CatalogClient;
import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.CartDTO;
import com.revshop.salesservice.dto.CartItemDTO;
import com.revshop.salesservice.dto.ProductDTO;
import com.revshop.salesservice.model.Cart;
import com.revshop.salesservice.model.CartItem;
import com.revshop.salesservice.repository.CartItemRepository;
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
    private final CartItemRepository cartItemRepository;
    private final CatalogClient catalogClient;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
            CatalogClient catalogClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.catalogClient = catalogClient;
    }

    public CartDTO getCartByUserId(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
        return convertToDTO(cart);
    }

    public void addItemToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));

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

    public void removeItemFromCart(Long userId, Long productId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().removeIf(item -> item.getProductId().equals(productId));
            cartRepository.save(cart);
        });
    }

    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
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
