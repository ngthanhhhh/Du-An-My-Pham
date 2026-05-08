package com.cosmetics.ecommerce.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cosmetics.ecommerce.entity.Product;
import com.cosmetics.ecommerce.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 1. GET ALL
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    // 2. GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // 3. CREATE
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        return new ResponseEntity<>(productService.create(product), HttpStatus.CREATED);
    }

    // 4. UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Integer id,
            @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    // 5. DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.ok("Xóa thành công");
    }

    // 6. SEARCH + FILTER
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice
    ) {

        // Ưu tiên kết hợp nhiều điều kiện
        return ResponseEntity.ok(
                productService.searchAdvanced(name, categoryId, minPrice, maxPrice)
        );
    }
}