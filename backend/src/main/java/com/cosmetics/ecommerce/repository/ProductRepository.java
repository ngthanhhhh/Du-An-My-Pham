package com.cosmetics.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cosmetics.ecommerce.entity.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // search theo tên
    List<Product> findByNameContainingIgnoreCase(String name);

    // lọc theo giá
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // lọc theo category
    List<Product> findByCategory_CategoryId(Integer categoryId);

    // kết hợp nhiều điều kiện (QUAN TRỌNG)
    @Query("""
        SELECT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
        AND (:min IS NULL OR p.price >= :min)
        AND (:max IS NULL OR p.price <= :max)
    """)
    List<Product> searchAdvanced(String name, Integer categoryId, BigDecimal min, BigDecimal max);

    //  pagination + filter
    @Query("""
        SELECT p FROM Product p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
    """)
    Page<Product> searchWithPaging(String name, Integer categoryId, Pageable pageable);

    //  kiểm tra tồn tại
    boolean existsByName(String name);

    //Tìm và khóa sản phẩm để trừ kho an toàn, tránh bị âm kho
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p Where p.productId = :id")
    Optional<Product> findByIdWithLock(Integer id);
}