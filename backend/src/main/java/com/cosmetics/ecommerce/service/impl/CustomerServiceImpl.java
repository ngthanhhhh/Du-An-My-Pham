package com.cosmetics.ecommerce.service.impl;

import com.cosmetics.ecommerce.dto.CustomerResponse;
import com.cosmetics.ecommerce.dto.CustomerDetailResponse;
import com.cosmetics.ecommerce.entity.User;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.CustomerRepository;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements  CustomerService{

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    public Page<CustomerResponse> getAllCustomers(String keyword, Boolean isActive, Pageable pageable){

        keyword = (keyword == null || keyword.trim().isEmpty() ? null : keyword.trim());

        return customerRepository.findAllCustomers(keyword, isActive, pageable)
                .map(u -> new CustomerResponse(
                        u.getUserId(), u.getName(), u.getEmail(),
                        u.getPhone(), u.getCreatedAt(), u.getIsActive()
                ));
    }

    @Override
    public CustomerDetailResponse getCustomerDetail(Integer id){
        User user = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if(!user.getRole().getRoleName().equals("ROLE_CUSTOMER")){
            throw new BadRequestException("User không phải khách hàng");
        }

        // Lấy lịch sử đơn hàng từ OrderRepository
        List<CustomerDetailResponse.OrderHistoryDTO> history = orderRepository.findByUserUserId(id)
                .stream()
                .map(o -> new CustomerDetailResponse.OrderHistoryDTO(
                        o.getOrderId(),
                        o.getCreatedAt(),
                        o.getTotalPrice(),
                        o.getStatus().name()

                )).toList();

        return new CustomerDetailResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getCreatedAt(),
                user.getIsActive(),
                history
        );
    }

    // Mở / khóa tài khoản khách hàng
    @Override
    @Transactional
    public void updateStatus(Integer id, Boolean isActive){

        if(isActive == null){
            throw new BadRequestException("Trạng thái không hợp lệ");
        }

        User user = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if(!user.getRole().getRoleName().equals("ROLE_CUSTOMER")){
            throw new BadRequestException("Chỉ áp dụng cho khách hàng");
        }

        user.setIsActive(isActive);

        customerRepository.save(user);
    }
}
