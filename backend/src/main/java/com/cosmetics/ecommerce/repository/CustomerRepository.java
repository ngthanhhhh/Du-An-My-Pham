package com.cosmetics.ecommerce.repository;

import com.cosmetics.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<User, Integer> {

    //Lọc user có role ROLE_CUSTOMER tìm kiếm theo tên/email, không phân biệt hoa thường
    @Query("""
    SELECT u FROM User u
    WHERE u.role.roleName = 'ROLE_CUSTOMER'
    AND (:keyword IS NULL 
        OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:isActive IS NULL OR u.isActive = :isActive)
""")
    Page<User> findAllCustomers(@Param("keyword")String keyword, @Param("isActive") Boolean isActive, Pageable pageable);

}
