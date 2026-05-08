package com.cosmetics.ecommerce.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceLegacy {

    private final UserRepository userRepository;

    //Hàm tìm user bằng email (dùng cho logic đăng nhập)
    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    //Lấy thông tin chi tiết user (dùng cho chức năng xem Profile)
    public User getUserById(Integer id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung voi ID: " + id));

    }

    //Lấy danh sách khách hàng (dùng cho chức năng Admin quản lý khách hàng)
    public List<User> getAllCustomers(){
        return userRepository.findAll();
    }

    @Transactional
    //Hàm cập nhật trạng thái tài khoản (Dùng cho admin khi muốn mở/khóa tài khoản)
    public void toggleUserStatus(Integer id){
        User user = getUserById(id);
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }
}
