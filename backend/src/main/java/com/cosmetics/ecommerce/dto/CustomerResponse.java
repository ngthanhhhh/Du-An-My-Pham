package com.cosmetics.ecommerce.dto;

import java.time.LocalDateTime;

public record CustomerResponse (
    Integer id,
    String name,
    String email,
    String phone,
    LocalDateTime createdAt,
    Boolean isActive
){}
