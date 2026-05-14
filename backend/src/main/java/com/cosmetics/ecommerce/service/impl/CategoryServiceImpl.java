package com.cosmetics.ecommerce.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.cosmetics.ecommerce.entity.Category;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.exception.ResourceNotFoundException;
import com.cosmetics.ecommerce.repository.CategoryRepository;
import com.cosmetics.ecommerce.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public Page<Category> getAll(int page, int size, String sortBy, String direction) {

        if (page < 0) {
            throw new BadRequestException("Số trang không hợp lệ");
        }

        if (size <= 0) {
            throw new BadRequestException("Kích thước trang không hợp lệ");
        }

        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "categoryId";
        }

        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return categoryRepository.findAll(pageable);
    }

    @Override
    public Category getById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Danh mục không tồn tại"));
    }

    @Override
    public Category create(Category category) {

        validateCategory(category);

        String name = category.getName().trim();

        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        category.setName(name);

        return categoryRepository.save(category);
    }

    @Override
    public Category update(Integer id, Category category) {

        validateCategory(category);

        Category old = getById(id);

        String newName = category.getName().trim();

        if (!old.getName().equalsIgnoreCase(newName)
                && categoryRepository.existsByName(newName)) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        old.setName(newName);
        old.setDescription(category.getDescription());

        return categoryRepository.save(old);
    }

    @Override
    public void delete(Integer id) {

        Category category = getById(id);

        try {
            categoryRepository.delete(category);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Không thể xóa danh mục đang có sản phẩm");
        }
    }

    private void validateCategory(Category category) {

        if (category == null) {
            throw new BadRequestException("Dữ liệu danh mục không hợp lệ");
        }

        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
    }
}