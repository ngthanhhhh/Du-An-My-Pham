package com.cosmetics.ecommerce.service.impl;

import com.cosmetics.ecommerce.service.AuthService;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.dto.*;
import com.cosmetics.ecommerce.entity.Cart;
import com.cosmetics.ecommerce.entity.Role;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.repository.RoleRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.cosmetics.ecommerce.repository.CartRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final CartRepository cartRepository;
    private final AuthenticationManager authenticationManager;

    // UC3.5 - Dang ky tai khoan (cho khach hang)
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request){
        validateRegisterRequest(request);

        String email = request.getEmail().trim().toLowerCase();

        //Kiểm tra email đã tồn tại
        if(userRepository.findByEmail(email).isPresent()){
            throw new BadRequestException("Email này đã được đăng ký");
        }

        //Lấy role CUSTOMER
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy ROLE_CUSTOMER"));

        //Tạo user
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setPhone(request.getPhone().trim());
        user.setRole(customerRole);

        user.setIsActive(true);

        //Lưu vào Database
        User savedUser = userRepository.save(user);

        //Tạo giỏ hàng trống cho User mới
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        return new RegisterResponse("Đăng ký tài khoản thành công");
    }

    @Override
    // dang nhap Admin - dang nhap Customer
    public LoginResponse login(LoginRequest request){

        validateLoginRequest(request);

        String email = request.getEmail().trim().toLowerCase();

        //b1: xác thực email + password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );
        //b2: lấy user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy user"));

        //b3: kiểm tra tài khoản có bị khóa không
        if(!user.getIsActive()){
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        // b4. Tạo token
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getRoleName());

        // b5. Trả về thông tin cho Frontend
        return new LoginResponse(
                token,
                user.getRole().getRoleName(),
                user.getName());
    }

    //Validate cho đăng ký
    private void validateRegisterRequest(RegisterRequest request){
        if (request == null){
            throw new BadRequestException("Dữ liệu đăng ký không hợp lệ");
        }

        String name = request.getName() != null ? request.getName().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        String password = request.getPassword();
        String confirm = request.getConfirmPassword();

        if (name == null || name.isBlank()){
            throw new BadRequestException("Họ tên không được để trống");
        }

        if (email == null || email.isBlank()){
            throw new BadRequestException("Email không được để trống");
        }

        if(!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")){
            throw new BadRequestException("Email không đúng định dạng");
        }

        if(phone == null ||!phone.matches("^\\d{10}$")){
            throw new BadRequestException("Số điện thoại phải gồm 10 chữ số");
        }

        if(password == null || password.length() < 6){
            throw new BadRequestException(("Mật khẩu phải có ít nhất 6 kí tự"));
        }

        //Mật khẩu mạnh
        if(!password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")){
            throw new BadRequestException("Mật khẩu phải chứa chữ và số");
        }

        if(confirm == null ||
                !password.equals(confirm)){
            throw new BadRequestException(("Mật khẩu xác nhận không khớp"));
        }

    }

    private void validateLoginRequest(LoginRequest request){

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        if (request == null ||
                email == null || email.isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()){
            throw new BadRequestException("Vui lòng nhập đầy đủ thông tin đăng nhập");
        }
    }

}
