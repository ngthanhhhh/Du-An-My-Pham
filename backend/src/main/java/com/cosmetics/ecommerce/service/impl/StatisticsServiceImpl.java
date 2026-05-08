package com.cosmetics.ecommerce.service.impl;

import com.cosmetics.ecommerce.dto.RevenueChartDTO;
import com.cosmetics.ecommerce.dto.StatisticsResponse;
import com.cosmetics.ecommerce.enums.OrderStatus;
import com.cosmetics.ecommerce.enums.StatisticType;
import com.cosmetics.ecommerce.exception.BadRequestException;
import com.cosmetics.ecommerce.repository.OrderRepository;
import com.cosmetics.ecommerce.repository.StatisticRepository;
import com.cosmetics.ecommerce.repository.UserRepository;
import com.cosmetics.ecommerce.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class StatisticsServiceImpl implements StatisticsService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StatisticRepository statisticRepository;

    @Override
    //Lấy tổng hợp các thông số thống kê chính cho Dashboard của Admin
    public StatisticsResponse getAdminDashboard(){

        //Lấy doanh thu (Chỉ tính đơn COMPLETED)
        Double totalRevenue = orderRepository.calculateTotalRevenue();

        return StatisticsResponse.builder()
                .totalRevenue( totalRevenue == null ? 0.0 : totalRevenue )
                .totalOrders( orderRepository.countTotalOrders())
                .totalUsers( userRepository.count())
                .pendingOrders( orderRepository.countByStatus(OrderStatus.PENDING))
                .shippingOrders( orderRepository.countByStatus(OrderStatus.SHIPPING))
                .completedOrders(orderRepository.countByStatus(OrderStatus.COMPLETED))
                .cancelledOrders( orderRepository.countByStatus(OrderStatus.CANCELLED))
                .build();

    }

    @Override
    public List<RevenueChartDTO> getRevenueChartData(StatisticType type, LocalDate fromDate, LocalDate toDate){

        if(type == null){
            throw new BadRequestException("Vui lòng chọn loại thống kê");
        }

        LocalDate start = (fromDate != null) ? fromDate:LocalDate.now().minusDays(30);
        LocalDate end = (toDate != null) ? toDate:LocalDate.now();

        if(start.isAfter(end)){
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }
        LocalDateTime fromDateTime = start.atStartOfDay();
        LocalDateTime toDateTime = end.atTime(23, 59, 59);

        switch (type) {
            case DAY:
                return statisticRepository.getRevenueByDay(fromDateTime, toDateTime);

            case WEEK:
                return statisticRepository.getRevenueByWeek(fromDateTime, toDateTime);

            case MONTH:
                return statisticRepository.getRevenueByMonth(fromDateTime, toDateTime);

            default:
                throw new BadRequestException("Loại thống kê không hợp lệ");
        }
    }
}
