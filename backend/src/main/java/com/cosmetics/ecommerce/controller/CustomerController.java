package com.cosmetics.ecommerce.controller;

import com.cosmetics.ecommerce.dto.CustomerDetailResponse;
import com.cosmetics.ecommerce.dto.CustomerResponse;
import com.cosmetics.ecommerce.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    //Xem danh sách khách hàng
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort)
    {

        // parse sort
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction,sortParams[0]));
        return ResponseEntity.ok(customerService.getAllCustomers(keyword, isActive, pageable));
    }

    // GET /api/v1/admin/customers/{id}
    // Xem chi tiết khách hàng
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable Integer id){
        return ResponseEntity.ok(customerService.getCustomerDetail(id));
    }

    ///{id}/status
    // Mở / Khóa tài khoản khách hàng
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> request
    ){
        Boolean isActive = request.get("isActive");

        customerService.updateStatus(id, isActive);

        return ResponseEntity.ok(
                Map.of("message", "Cập nhật trạng thái khách hàng thành công"));

    }


}
