package com.cosmetics.ecommerce.service;
import com.cosmetics.ecommerce.dto.ChangePasswordRequest;
import com.cosmetics.ecommerce.dto.UpdateProfileRequest;
import com.cosmetics.ecommerce.dto.UserProfileResponse;

public interface UserService {

    //UC3.7 - Đổi mật khẩu cho customer đang đăng nhập
    void changePassword(String email, ChangePasswordRequest request);

    // Xem profile cá nhân
    UserProfileResponse getProfileByEmail(String email);

    // Cập nhật profile cá nhân
    void updateProfile(String email, UpdateProfileRequest request);

}
