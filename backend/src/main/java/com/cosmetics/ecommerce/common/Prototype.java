package com.cosmetics.ecommerce.common;

/**
 * Interface chung cho các object có khả năng tự tạo bản sao.
 * @param <T> kiểu object được clone
 */
public interface Prototype<T> {
    /**
     * Tạo bản sao của object hiện tại.
     * 
     * @return object mới có dữ liệu được sao chép từ object gốc
     */
    T clone();
}
