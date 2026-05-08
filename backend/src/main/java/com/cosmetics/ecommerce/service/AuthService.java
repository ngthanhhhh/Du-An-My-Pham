package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.LoginRequest;
import com.cosmetics.ecommerce.dto.LoginResponse;
import com.cosmetics.ecommerce.dto.RegisterRequest;
import com.cosmetics.ecommerce.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);

}
