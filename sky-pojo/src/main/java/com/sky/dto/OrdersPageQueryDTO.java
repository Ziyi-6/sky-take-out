package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersPageQueryDTO implements Serializable {
    private int page; //页码
    private int pageSize; //每页显示记录数
    private Integer status; //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
    private Long userId; //用户ID
}