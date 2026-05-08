package com.cosmetics.ecommerce.repository;

import com.cosmetics.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // check trùng tên
    boolean existsByName(String name);

    // tìm theo tên chính xác
    Optional<Category> findByName(String name);


    boolean existsByNameIgnoreCase(String name);
}