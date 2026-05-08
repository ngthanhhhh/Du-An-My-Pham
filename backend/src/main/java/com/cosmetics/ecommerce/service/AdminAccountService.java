package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.AdminAccountRequest;
import com.cosmetics.ecommerce.dto.AdminAccountResponse;
import com.cosmetics.ecommerce.dto.ChangePasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminAccountService {

    //Lấy danh sách tất cả tài khoản Admin
    Page<AdminAccountResponse> getAllAccounts(String keyword, Boolean isActive, Pageable pageable);

    //Thêm tài khoản admin mới
    //Kiểm tra email trùng trước khi tạo
    AdminAccountResponse createAccount(AdminAccountRequest request);

    //Sửa thông tin tài khoản (name, email, isActive)
    //Không sửa pasword ở đây
    AdminAccountResponse updateAccount(Integer id, AdminAccountRequest request);

    //xóa mềm tài khoản (set_is_active = false)
    //không cho xóa tài khoản đang đăng nhập
    //currentUserEmail: email của admin đang đăng nhập
    void deleteAccount(Integer id, String currentUserEmail);

    //đổi mật khẩu tài khoản
    //mã hóa BCrypt trước khi lưu
    void changePassword(Integer id, ChangePasswordRequest request);

    // mở/ khóa tài khoản admin
    void updateStatus(Integer id, Boolean isActive);

}
