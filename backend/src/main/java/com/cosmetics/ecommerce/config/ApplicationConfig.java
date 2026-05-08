package com.cosmetics.ecommerce.config;

import com.cosmetics.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@RequiredArgsConstructor

public class ApplicationConfig {

    private  final UserRepository userRepository;

    //Cung cấp cách thức tìm kiếm người dùng từ Database qua Email
    @Bean
    public UserDetailsService userDetailsService(){
        return username -> {
            com.cosmetics.ecommerce.entity.User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay nguoi dung: " + username));

            //Chuyển đổi sang đối tượng User của Spring Security
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities(user.getRole().getRoleName()) //roleName phải là ROLE_ADMIN hoặc ROLE_CUSTOMER
                    .build();
        };

    }

    //Định nghĩa bộ mã hóa mật khẩu sử dụng thuật toán BCrypt
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    //Cấu hình bộ máy xác thực: Kết hợp UserDetailService v PasswordEncoder

//    @Bean
//    public AuthenticationProvider authenticationProvider(){
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService());
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }

    //Quản lý xác thực chung cho toàn ứng dụng
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}
