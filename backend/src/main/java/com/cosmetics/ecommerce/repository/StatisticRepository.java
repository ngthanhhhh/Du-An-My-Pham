package com.cosmetics.ecommerce.repository;

import com.cosmetics.ecommerce.dto.RevenueChartDTO;
import com.cosmetics.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticRepository extends JpaRepository<Order, Integer>{

    //1. DAY - Xem doanh thu chi tiết từng ngày trong một khoảng thời gian
    @Query("""
    SELECT new com.cosmetics.ecommerce.dto.RevenueChartDTO(
        DATE(o.createdAt),
        SUM(o.totalPrice)
    )
    FROM Order o
    WHERE o.status = com.cosmetics.ecommerce.enums.OrderStatus.COMPLETED
    AND o.createdAt BETWEEN :from AND :to
    GROUP BY DATE(o.createdAt)
    ORDER BY DATE(o.createdAt)
""")
    List<RevenueChartDTO> getRevenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    //2. WEEK - Xem xu hướng doanh thu theo từng tuần trong tháng.
    @Query("""
    SELECT new com.cosmetics.ecommerce.dto.RevenueChartDTO(
        FUNCTION("WEEK", o.createdAt),
        CAST(SUM(o.totalPrice) AS double)
    )
    FROM Order o
        WHERE o.status = com.cosmetics.ecommerce.enums.OrderStatus.COMPLETED
        AND o.createdAt BETWEEN :from AND :to
        GROUP BY FUNCTION('WEEK', o.createdAt)
        ORDER BY FUNCTION('WEEK', o.createdAt)
""")
    List<RevenueChartDTO> getRevenueByWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    //2. MONTH - Xem tổng doanh thu theo từng tháng trong năm
    @Query("""
    SELECT new com.cosmetics.ecommerce.dto.RevenueChartDTO(
        FUNCTION('MONTH', o.createdAt),
        SUM(o.totalPrice)
    )
    FROM Order o
    WHERE o.status = com.cosmetics.ecommerce.enums.OrderStatus.COMPLETED
    AND o.createdAt BETWEEN :from AND :to
    GROUP BY FUNCTION('MONTH', o.createdAt)
    ORDER BY FUNCTION('MONTH', o.createdAt)
""")
    List<RevenueChartDTO> getRevenueByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}
