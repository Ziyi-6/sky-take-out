package com.sky.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待接单数量（status = 2）
    private Integer toBeConfirmed;

    //已接单数量（status = 3）
    private Integer confirmed;

    //派送中数量（status = 4）
    private Integer deliveryInProgress;
}