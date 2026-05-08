package com.cosmetics.ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    // CREATE CATEGORY
    @PostMapping
    public ResponseEntity<Category> create(
            @Valid @RequestBody Category category) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(category));
    }

    // UPDATE CATEGORY
    @PutMapping("/{id}")
    public ResponseEntity<Category> update(
            @PathVariable Integer id,
            @Valid @RequestBody Category category) {

        return ResponseEntity.ok(
                categoryService.update(id, category)
        );
    }

    // DELETE CATEGORY
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {

        categoryService.delete(id);

        return ResponseEntity.ok("Xóa danh mục thành công");
    }
}