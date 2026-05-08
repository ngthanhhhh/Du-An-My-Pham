package com.cosmetics.ecommerce.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.repository.ProductRepository;
import com.cosmetics.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // 1. GET ALL
    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    // 2. GET BY ID
    @Override
    public Product getById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));
    }

    // 3. CREATE
    @Override
    public Product create(Product product) {

        // check category tồn tại
        Integer categoryId = product.getCategory().getCategoryId();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category không tồn tại"));

        product.setCategory(category);

        return productRepository.save(product);
    }

    // 4. UPDATE
    @Override
    public Product update(Integer id, Product product) {

        Product old = getById(id);

        old.setName(product.getName());
        old.setPrice(product.getPrice());
        old.setStock(product.getStock());
        old.setDescription(product.getDescription());
        old.setImage(product.getImage());

        // xử lý category 
        if (product.getCategory() != null) {
            Integer categoryId = product.getCategory().getCategoryId();

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category không tồn tại"));

            old.setCategory(category);
        }

        return productRepository.save(old);
    }

    // 5. DELETE
    @Override
    public void delete(Integer id) {

        Product product = getById(id);
        productRepository.delete(product);
    }

    // 6. SEARCH 
    @Override
    public List<Product> search(String name, BigDecimal min, BigDecimal max, Integer categoryId) {

        List<Product> products = productRepository.findAll();

        if (name != null) {
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
        }

        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory().getCategoryId().equals(categoryId))
                    .toList();
        }

        if (min != null && max != null) {
            products = products.stream()
                    .filter(p -> p.getPrice().compareTo(min) >= 0
                            && p.getPrice().compareTo(max) <= 0)
                    .toList();
        }

        return products;
    }

    @Override
    public List<Product> searchAdvanced(String name, Integer categoryId, Double minPrice, Double maxPrice) {

        // ưu tiên filter kết hợp
        if (name != null && categoryId != null) {
            return productRepository.findByNameContainingIgnoreCase(name)
                    .stream()
                    .filter(p -> p.getCategory().getCategoryId().equals(categoryId))
                    .toList();
        }

        if (name != null) {
            return productRepository.findByNameContainingIgnoreCase(name);
        }

        if (categoryId != null) {
            return productRepository.findByCategory_CategoryId(categoryId);
        }

        if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetween(
                    BigDecimal.valueOf(minPrice),
                    BigDecimal.valueOf(maxPrice)
            );
        }

        return productRepository.findAll();
    }
}