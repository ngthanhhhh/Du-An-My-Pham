package com.cosmetics.ecommerce.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosmetics.ecommerce.dto.OrderDetailResponseDTO;
import com.cosmetics.ecommerce.dto.OrderRequestDTO;
import com.cosmetics.ecommerce.dto.OrderResponseDTO;
import com.cosmetics.ecommerce.security.CurrentUserProvider;
import com.cosmetics.ecommerce.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    //Khach dat hang
    @PostMapping
    public ResponseEntity<OrderResponseDTO> placeOrder(
        Authentication authentication,
        @Valid @RequestBody OrderRequestDTO request) 
    {
        Integer currentUserId = currentUserProvider.getCurrentUserId(authentication);
        OrderResponseDTO response = orderService.placeOrder(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(Authentication authentication) {
        Integer currentUserId = currentUserProvider.getCurrentUserId(authentication);
        List<OrderResponseDTO> history = orderService.getMyOrders(currentUserId);
        return ResponseEntity.ok(history);
        
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponseDTO> getMyOrderDetail(
        Authentication authentication,
        @PathVariable Integer id) 
    {
        Integer currentUserId = currentUserProvider.getCurrentUserId(authentication);
        OrderDetailResponseDTO detail = orderService.getOrderDetailForCustomer(currentUserId, id);
        return ResponseEntity.ok(detail);
    }
}
