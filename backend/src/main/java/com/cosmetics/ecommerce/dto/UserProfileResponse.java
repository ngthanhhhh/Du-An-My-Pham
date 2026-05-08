package com.cosmetics.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String name;
    private String email;
    private String phone;
    private String address;
}
