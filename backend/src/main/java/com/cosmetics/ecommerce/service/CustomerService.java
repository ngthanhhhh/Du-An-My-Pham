package com.cosmetics.ecommerce.service;

import com.cosmetics.ecommerce.dto.CustomerResponse;
import com.cosmetics.ecommerce.dto.CustomerDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {
    Page<CustomerResponse> getAllCustomers(String keyword,  Boolean isActive, Pageable pageable);
    CustomerDetailResponse getCustomerDetail(Integer id);
    // mở/ khóa tài khoản khách hàng
    void updateStatus(Integer id, Boolean isActive);
}
