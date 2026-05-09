package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.enums.ProductStatus;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // =========================
    // VALIDATION
    // =========================
    private void validateProduct(Product product) {

        if (product == null) {
            throw new RuntimeException("Dữ liệu sản phẩm không hợp lệ");
        }

        // NAME
        if (product.getName() == null
                || product.getName().trim().isEmpty()) {

            throw new RuntimeException("Tên sản phẩm không được để trống");
        }

        // PRICE
        if (product.getPrice() == null
                || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException("Giá sản phẩm phải lớn hơn 0");
        }

        // STOCK
        if (product.getStock() == null
                || product.getStock() < 0) {

            throw new RuntimeException("Số lượng tồn kho không được âm");
        }

        // CATEGORY
        if (product.getCategory() == null
                || product.getCategory().getCategoryId() == null) {

            throw new RuntimeException("Danh mục không hợp lệ");
        }
    }

    // =========================
    // GET ALL
    // =========================
    @Override
    public List<Product> getAll() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    // =========================
    // GET BY ID
    // =========================
    @Override
    public Product getById(Integer id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product không tồn tại"));
    }

    // =========================
    // CREATE
    // =========================
    @Override
    public Product create(Product product) {

        // VALIDATE trước
        validateProduct(product);

        // CHECK CATEGORY
        Integer categoryId = product.getCategory().getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new RuntimeException("Category không tồn tại"));

        product.setCategory(category);

        // mặc định ACTIVE
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        return productRepository.save(product);
    }

    // =========================
    // UPDATE
    // =========================
    @Override
    public Product update(Integer id, Product product) {

        validateProduct(product);

        Product old = getById(id);

        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setStock(product.getStock());
        old.setDescription(product.getDescription());
        old.setImage(product.getImage());

        // CATEGORY
        Integer categoryId = product.getCategory().getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new RuntimeException("Category không tồn tại"));

        old.setCategory(category);

        return productRepository.save(old);
    }

    // =========================
    // DELETE (SOFT DELETE)
    // =========================
    @Override
    public void delete(Integer id) {

        Product product = getById(id);

        product.setStatus(ProductStatus.INACTIVE);

        productRepository.save(product);
    }

    // =========================
    // SEARCH
    // =========================
    @Override
    public List<Product> search(
            String name,
            BigDecimal min,
            BigDecimal max,
            Integer categoryId
    ) {

        List<Product> products =
                productRepository.findByStatus(ProductStatus.ACTIVE);

        // FILTER NAME
        if (name != null && !name.trim().isEmpty()) {

            products = products.stream()
                    .filter(p -> p.getName()
                            .toLowerCase()
                            .contains(name.toLowerCase()))
                    .toList();
        }

        // FILTER CATEGORY
        if (categoryId != null) {

            products = products.stream()
                    .filter(p -> p.getCategory()
                            .getCategoryId()
                            .equals(categoryId))
                    .toList();
        }

        // FILTER PRICE
        if (min != null && max != null) {

            products = products.stream()
                    .filter(p ->
                            p.getPrice().compareTo(min) >= 0
                                    && p.getPrice().compareTo(max) <= 0
                    )
                    .toList();
        }

        return products;
    }

    // =========================
    // SEARCH ADVANCED
    // =========================
    @Override
    public List<Product> searchAdvanced(
            String name,
            Integer categoryId,
            Double minPrice,
            Double maxPrice
    ) {

        List<Product> products =
                productRepository.findByStatus(ProductStatus.ACTIVE);
         
        // KEY RỖNG
        if (name != null && name.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập từ khóa");
        }

        // NAME
        if (name != null && !name.trim().isEmpty()) {

            products = products.stream()
                    .filter(p -> p.getName()
                            .toLowerCase()
                            .contains(name.toLowerCase()))
                    .toList();
        }

        // CATEGORY
        if (categoryId != null) {

            products = products.stream()
                    .filter(p -> p.getCategory()
                            .getCategoryId()
                            .equals(categoryId))
                    .toList();
        }

        // PRICE
        if (minPrice != null && maxPrice != null) {

            BigDecimal min = BigDecimal.valueOf(minPrice);
            BigDecimal max = BigDecimal.valueOf(maxPrice);

            products = products.stream()
                    .filter(p ->
                            p.getPrice().compareTo(min) >= 0
                                    && p.getPrice().compareTo(max) <= 0
                    )
                    .toList();
        }

        return products;
    }
}