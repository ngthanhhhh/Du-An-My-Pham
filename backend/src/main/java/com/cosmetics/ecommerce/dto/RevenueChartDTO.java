package com.cosmetics.ecommerce.dto;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import lombok.Data;

@Data
@NoArgsConstructor
public class RevenueChartDTO {
    private String label; //Nhãn (Ngày 1, 2,... hoặc Tháng 1, 2, ...)
    private Double revenue; //Doanh thu

    public RevenueChartDTO(Object label, Number revenue){
        this.label = (label != null) ? label.toString() : "";
        this.revenue = (revenue != null) ? revenue.doubleValue() : 0.0;
    }

    public RevenueChartDTO(Object label, BigDecimal revenue) {
        this.label = (label != null) ? label.toString() : "";
        this.revenue = (revenue != null) ? revenue.doubleValue() : 0.0;
    }    
}
