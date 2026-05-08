package com.cosmetics.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import com.cosmetics.ecommerce.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import com.cosmetics.ecommerce.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    //tim user theo email, dung khi dang nhap
    Optional<User> findByEmail(String email);

    //kiem tra email da ton tai chua, dung khi dang ky
    boolean existsByEmail(String email);

    //lấy danh sách user theo role (dùng cho quản lý tài khoản admin)
    List<User> findByRole(Role role);

    //Lấy danh sách user theo role và chỉ lấy tài khoản còn hoạt động (không bị xóa mềm)
    List<User> findByRoleAndIsActiveTrue(Role role);

    //Kiểm tra email tồn tại nhưng khác id hiện tại
    boolean existsByEmailAndUserIdNot(String email, Integer userId);

    @Query("""
    SELECT u FROM User u
    WHERE u.role.roleName = 'ROLE_ADMIN'
    AND (:keyword IS NULL 
        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:isActive IS NULL OR u.isActive = :isActive)
""")

    Page<User> findAdmins(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
