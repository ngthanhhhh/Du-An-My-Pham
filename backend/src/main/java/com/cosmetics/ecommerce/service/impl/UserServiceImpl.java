package com.cosmetics.ecommerce.service.impl;

import com.cosmetics.ecommerce.dto.UserProfileResponse;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.dto.ChangePasswordRequest;
import com.cosmetics.ecommerce.dto.UpdateProfileRequest;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //UC3.7 - Customer tự đổi mật khẩu của mình
    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request){

        validateChangePassword(request);

        String oldPassword = request.getOldPassword().trim();
        String newPassword = request.getNewPassword().trim();

        //Tìm user theo email lấy từ SecurityContext
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if(!user.getIsActive()){
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        //Xác minh mật khẩu cũ
            if(!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        //Mật khẩu mới không được trùng mật khẩu cũ
        if(passwordEncoder.matches(newPassword, user.getPassword())){
            throw new BadRequestException("Mật khẩu mới không được trùng mật khẩu cũ");
        }

        //Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void validateChangePassword(ChangePasswordRequest request){

        //Validate input
        if(request == null){
            throw new BadRequestException("Dữ liệu đổi mật khẩu không hợp lệ");
        }

        String oldPassword = request.getOldPassword() != null ? request.getOldPassword().trim() : null;
        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : null;
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : null;

        if (oldPassword == null || oldPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            throw new BadRequestException("Vui lòng nhập đầy đủ thông tin");
        }

        //Check mật khẩu xác nhận
        if(!newPassword.equals(confirmPassword)){
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Check độ dài
        if (newPassword.length() < 6){
            throw new BadRequestException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // Check chữ + số
        if(!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")){
            throw new BadRequestException("Mật khẩu phải chứa chữ và số");
        }
    }

    @Override
    public UserProfileResponse getProfileByEmail(String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if(!user.getIsActive()){
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        return UserProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .build();
    }

    @Override
    @Transactional
    public void updateProfile(String email, UpdateProfileRequest request){

        if (request == null){
            throw new BadRequestException("Vui lòng nhập đầy đủ thông tin");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if(!user.getIsActive()){
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        // xác thực và cập nhật
        if(request.getName() != null && !request.getName().isBlank()){
            user.setName(request.getName().trim());
        }

        if(request.getPhone() != null){
            if(!request.getPhone().matches("^\\d{10}$")){
                throw new BadRequestException("SĐT phải là 10 số");
            }
            user.setPhone(request.getPhone());
        }

        if(request.getAddress() != null){
            user.setAddress(request.getAddress());
        }

        userRepository.save(user);
    }



}
