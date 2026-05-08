package com.cosmetics.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.dto.CartItemRequestDTO;
import com.cosmetics.ecommerce.dto.CartResponseDTO;
import com.cosmetics.ecommerce.security.CurrentUserProvider;
import com.cosmetics.ecommerce.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDTO> addToCart(
        Authentication authentication,
        @RequestBody @Valid CartItemRequestDTO request) {
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items")
    public ResponseEntity<CartResponseDTO> updateCartItem(
        Authentication authentication,
        @RequestBody @Valid CartItemRequestDTO request) {
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        return ResponseEntity.ok(cartService.updateCartItem(userId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeCartItem(
        Authentication authentication,
        @PathVariable Integer productId){
        Integer userId = currentUserProvider.getCurrentUserId(authentication);
        cartService.removeCartItem(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
