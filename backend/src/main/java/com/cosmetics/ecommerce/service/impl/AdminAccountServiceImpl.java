package com.cosmetics.ecommerce.service.impl;

import com.cosmetics.ecommerce.dto.AdminAccountRequest;
import com.cosmetics.ecommerce.dto.AdminAccountResponse;
import com.cosmetics.ecommerce.dto.ChangePasswordRequest;
import com.cosmetics.ecommerce.entity.Role;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.RoleRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.AdminAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor

public class AdminAccountServiceImpl implements AdminAccountService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    //lấy danh sách tất cả tài khoản admin
    @Override
    public Page<AdminAccountResponse> getAllAccounts(String keyword, Boolean isActive, Pageable pageable){

        keyword = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();

        //Lọc chỉ lấy user có role ADMIN, chuyển sang DTO rồi trả về
        return userRepository.findAdmins(keyword, isActive, pageable)
                .map(this::toResponse);

    }

    //Tạo tài khoản admin mới
    @Override
    @Transactional
    public AdminAccountResponse createAccount(AdminAccountRequest request){

        validateAdminAccountRequest(request, true);

        String email = request.getEmail().trim().toLowerCase();
        String password = request.getPassword().trim();

        //Kiểm tra email đã tồn tại chưa
        if(userRepository.existsByEmail(email)){
            throw new BadRequestException("Email đã tồn tại");
        }


        //Lấy role ADMIN từ database
        Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ROLE_ADMIN"));


        //Tạo user mới với role ADMIN
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        //mã hóa mật khẩu trước khi lưu
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(adminRole);
        user.setIsActive(true);

        //lưu vào database và trả về DTO
        return toResponse(userRepository.save(user));
    }

    //Cập nhật thông tin tài khoản (name, email, isActive)
    //Không sửa password ở đây
    @Override
    @Transactional
    public AdminAccountResponse updateAccount(Integer id, AdminAccountRequest request){

        validateAdminAccountRequest(request, false);

        String email = request.getEmail().trim().toLowerCase();

        //Tìm user theo id, nếu không có thì báo lỗi
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if(!user.getRole().getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Chỉ áp dụng cho tài khoản ADMIN");
        }

        // Kiểm tra email trùng (trừ chính nó)
        if(userRepository.existsByEmailAndUserIdNot(email,id)){
            throw new BadRequestException("Email đã tồn tại");
        }

        //Cập nhật thông tin
        user.setName(request.getName().trim());
        user.setEmail(email);

        //Tránh lỗi null khi frontend không gửi isActive
        if(request.getIsActive() != null){
            user.setIsActive(request.getIsActive());
        }

        return toResponse(userRepository.save(user));
    }

    //Xóa mềm tài khoản (set isActive = false, không xóa khỏi database)
    @Override
    @Transactional

    public void deleteAccount(Integer id, String currentUserEmail){
        //Tìm user theo id, nếu không có thì báo lỗi
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if(!user.getRole().getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Chỉ áp dụng cho tài khoản ADMIN");
        }

        //Không cho xóa tài khoản đang đăng nhập
        if(user.getEmail().equalsIgnoreCase(currentUserEmail)){
            throw new BadRequestException("Không thể xóa tài khoản đang đăng nhập");
        }

        // Xóa mềm: chỉ set isActive = false
        user.setIsActive(false);
        userRepository.save(user);
    }

    //Đổi mật khẩu tài khoản Admin
    @Override
    @Transactional
    public void changePassword(Integer id, ChangePasswordRequest request){

        validateChangePassword(request);

        String newPassword = request.getNewPassword().trim();

        // Tìm user theo id, nếu không có thì báo lỗi
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if(!user.getRole().getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Chỉ áp dụng cho tài khoản ADMIN");
        }

        // Không trùng password cũ
        if(passwordEncoder.matches(newPassword, user.getPassword())){
            throw new BadRequestException("Mật khẩu mới không được trùng mật khẩu cũ");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    // Chuyển đổi User entity sang AdminAccountResponse DTO
    // Không trả password về frontend
    private AdminAccountResponse toResponse(User user){
        return AdminAccountResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .isActive(user.getIsActive())
                .createAt(user.getCreatedAt())
                .build();
    }

    // Mở / khóa tài khoản admin
    @Override
    @Transactional
    public void updateStatus(Integer id, Boolean isActive){

        if(isActive == null){
            throw new BadRequestException("Trạng thái không hợp lệ");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        //Check role trước
        if(!user.getRole().getRoleName().equals("ROLE_ADMIN")){
            throw new BadRequestException("Chỉ áp dụng cho tài khoản ADMIN");
        }

        // Lấy email hiện tại
        String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        if(user.getEmail().equalsIgnoreCase(currentUserEmail) && !isActive){
            throw new BadRequestException("Không thể tự vô hiệu hóa tài khoản của mình");
        }

        user.setIsActive(isActive);
        userRepository.save(user);
    }

    //Xác thực đổi mật khẩu admin
    private void validateChangePassword(ChangePasswordRequest request){
        if(request == null ){
            throw new BadRequestException("Dữ liệu không hợp lệ");
        }

        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : null;
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : null;

        if(newPassword == null || confirmPassword == null ||
           newPassword.isBlank() || confirmPassword.isBlank()){
            throw new BadRequestException("Mật khẩu không được để trống");
        }

        if(newPassword.length() < 6){
            throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if(!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")){
            throw new BadRequestException("Mật khẩu phải chứa chữ và số");
        }

        if(!newPassword.equals(confirmPassword)){
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
    }

    //Xác thực dữ liệu admin (dùng chung cho create/update)
    private void validateAdminAccountRequest(AdminAccountRequest request, boolean isCreate){

        if(request == null){
            throw new BadRequestException("Dữ liệu không hợp lệ");
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String name = request.getName() != null ? request.getName().trim() : null;
        String password = request.getPassword() != null ? request.getPassword().trim() : null;

        if(name == null || name.isBlank()){
            throw new BadRequestException("Họ tên không được để trống");
        }

        if(email == null || email.isBlank()){
            throw new BadRequestException("Email không được để trống");
        }

        if(!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")){
            throw new BadRequestException("Email không hợp lệ");
        }

        //chỉ check password khi tạo mới
        if(isCreate){
            if(password == null || password.length() < 6){
                throw new BadRequestException("Mật khẩu phải có ít nhất 6 ký tự");
            }

            //Bắt buộc có chữ + số
            if(!password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")){
                throw new BadRequestException("Mật khẩu phải chứa chữ và số");
            }
        }
    }


}
