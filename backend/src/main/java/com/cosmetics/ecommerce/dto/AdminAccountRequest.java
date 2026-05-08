package com.cosmetics.ecommerce.dto;

import lombok.Data;

@Data
public class AdminAccountRequest {
    private String name;
    private String email;
    private String password;
    private Boolean isActive;
}
