package com.cosmetics.ecommerce.service;

import org.springframework.data.domain.Page;

import com.cosmetics.ecommerce.entity.Category;

public interface CategoryService {

    Page<Category> getAll(
            int page,
            int size,
            String sortBy,
            String direction
    );

    Category getById(Integer id);

    Category create(Category category);

    Category update(Integer id, Category category);

    void delete(Integer id);
}