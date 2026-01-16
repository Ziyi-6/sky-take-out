package com.sky.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrdersPageQueryDTO implements Serializable {
    private int page; //页码
    private int pageSize; //每页显示记录数
    private Integer status; //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
    private Long userId; //用户ID
    private String number; //订单号
    private String phone; //手机号
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime; //开始时间
    
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime; //结束时间
}