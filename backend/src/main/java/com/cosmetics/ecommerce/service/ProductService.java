package com.cosmetics.ecommerce.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;

import com.cosmetics.ecommerce.dto.ProductRequest;
import com.cosmetics.ecommerce.dto.ProductResponse;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.enums.ProductStatus;

public interface ProductService {

    Page<ProductResponse> getPublicProducts(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    );

    ProductResponse getPublicProductDetail(Integer id);

    Page<ProductResponse> getAdminProducts(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    );

    Product getById(Integer id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Integer id, ProductRequest request);

    void delete(Integer id);
}