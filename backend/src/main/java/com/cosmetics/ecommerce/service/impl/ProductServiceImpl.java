package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.dto.ProductRequest;
import com.cosmetics.ecommerce.dto.ProductResponse;
import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.enums.ProductStatus;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new BadRequestException("Số trang không hợp lệ");
        }

        if (size <= 0) {
            throw new BadRequestException("Kích thước trang không hợp lệ");
        }

        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "productId";
        }

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageRequest.of(page, size, sort);
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá thấp nhất không được âm");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá cao nhất không được âm");
        }

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Khoảng giá không hợp lệ");
        }
    }

    private void validateCategoryExists(Integer categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Danh mục không tồn tại");
        }
    }

    private void validateProductRequest(ProductRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu sản phẩm không hợp lệ");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Tên sản phẩm không được để trống");
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Giá sản phẩm phải lớn hơn 0");
        }

        if (request.getStock() == null || request.getStock() < 0) {
            throw new BadRequestException("Số lượng tồn kho không được âm");
        }

        if (request.getCategoryId() == null) {
            throw new BadRequestException("Danh mục sản phẩm không được để trống");
        }
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = new ProductResponse();

        response.setProductId(product.getProductId());
        response.setName(product.getName());
        response.setPrice(product.getPrice());
        response.setStock(product.getStock());
        response.setDescription(product.getDescription());
        response.setImage(product.getImage());

        if (product.getCategory() != null) {
            response.setCategoryName(product.getCategory().getName());
        }

        if (product.getStatus() != null) {
            response.setStatus(product.getStatus().name());
        }

        return response;
    }

    private ProductStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ProductStatus.ACTIVE;
        }

        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái sản phẩm không hợp lệ");
        }
    }

    @Override
    public Page<ProductResponse> getPublicProducts(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        validatePriceRange(minPrice, maxPrice);
        validateCategoryExists(categoryId);

        Pageable pageable = buildPageable(page, size, sortBy, direction);

        return productRepository.searchPublicProducts(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                pageable
        ).map(this::mapToResponse);
    }

    @Override
    public ProductResponse getPublicProductDetail(Integer id) {
        return mapToResponse(getById(id));
    }

    @Override
    public Page<ProductResponse> getAdminProducts(
            String keyword,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        validatePriceRange(minPrice, maxPrice);
        validateCategoryExists(categoryId);

        Pageable pageable = buildPageable(page, size, sortBy, direction);

        return productRepository.searchAdminProducts(
                keyword,
                categoryId,
                minPrice,
                maxPrice,
                status,
                pageable
        ).map(this::mapToResponse);
    }

    @Override
    public Product getById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
    }

    @Override
    public ProductResponse create(ProductRequest request) {
        validateProductRequest(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setDescription(request.getDescription());
        product.setImage(request.getImage());
        product.setCategory(category);
        product.setStatus(parseStatus(request.getStatus()));

        Product saved = productRepository.save(product);

        return mapToResponse(saved);
    }

    @Override
    public ProductResponse update(Integer id, ProductRequest request) {
        validateProductRequest(request);

        Product old = getById(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        old.setName(request.getName().trim());
        old.setPrice(request.getPrice());
        old.setStock(request.getStock());
        old.setDescription(request.getDescription());
        old.setImage(request.getImage());
        old.setCategory(category);

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            old.setStatus(parseStatus(request.getStatus()));
        }

        Product updated = productRepository.save(old);

        return mapToResponse(updated);
    }

    @Override
    public void delete(Integer id) {
        Product product = getById(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }
}