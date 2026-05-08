package com.cosmetics.ecommerce.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder

public class AdminAccountResponse {
    private Integer userId;
    private String name;
    private String email;
    private Boolean isActive;
    private LocalDateTime createAt;
}
