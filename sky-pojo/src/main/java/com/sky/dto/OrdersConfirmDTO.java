package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrdersConfirmDTO implements Serializable {
    private Long id; // 订单ID
}