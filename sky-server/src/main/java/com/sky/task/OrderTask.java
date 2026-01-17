package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单
     * 每分钟触发一次
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void processTimeoutOrder() {
        log.info("开始处理超时订单：{}", LocalDateTime.now());
        
        // 查询状态为待付款且下单时间在15分钟之前的订单
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);//当前时间-15分钟，计算出阈值时间，在此之前的都要被取消
        List<Orders> timeoutOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);//筛选出超时的订单

        // 1.检查timeoutOrders这个集合对象本身是否存在2.检查集合内部是否有元素（不是 “空集合”）
        if (timeoutOrders != null && !timeoutOrders.isEmpty()) {
            for (Orders order : timeoutOrders) {
                // 将订单状态修改为已取消，并记录取消原因和时间
                Orders updateOrder = Orders.builder()
                        .id(order.getId())
                        .status(Orders.CANCELLED)
                        .cancelReason("订单超时，自动取消")
                        .cancelTime(LocalDateTime.now())
                        .build();
                
                orderMapper.update(updateOrder);
                log.info("超时订单已取消，订单ID：{}", order.getId());
            }
        }
        
        log.info("超时订单处理完成，共处理{}个订单", timeoutOrders != null ? timeoutOrders.size() : 0);
    }

    /**
     * 处理一直派送中的订单
     * 每天凌晨1点触发一次
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processDeliveryOrder() {
        log.info("开始处理派送中的订单：{}", LocalDateTime.now());
        
        // 获取1小时前的时间点
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        
        // 查询：状态为派送中且下单时间小于1小时前的订单
        List<Orders> deliveryOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        
        if (deliveryOrders != null && !deliveryOrders.isEmpty()) {
            for (Orders order : deliveryOrders) {
                // 将订单状态修改为已完成
                Orders updateOrder = Orders.builder()
                        .id(order.getId())
                        .status(Orders.COMPLETED)
                        .build();
                
                orderMapper.update(updateOrder);
                log.info("派送中订单已完成，订单ID：{}，下单时间：{}", order.getId(), order.getOrderTime());
            }
        }
        
        log.info("派送中订单处理完成，共处理{}个订单", deliveryOrders != null ? deliveryOrders.size() : 0);
    }
}