package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 查询今日运营数据
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData() {
        // 获取今天的开始时间和结束时间
        LocalDateTime beginTime = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.now().with(LocalTime.MAX);
        
        return getBusinessData(beginTime, endTime);
    }

    /**
     * 查询今日运营数据（指定时间）
     * @param beginTime
     * @param endTime
     * @return
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime beginTime, LocalDateTime endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);

        // 查询营业额
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        // 查询有效订单数
        map.put("status", Orders.COMPLETED);
        Integer validOrderCount = orderMapper.countByMap(map);
        validOrderCount = validOrderCount == null ? 0 : validOrderCount;

        // 查询订单总数
        map.remove("status");
        Integer totalOrderCount = orderMapper.countByMap(map);
        totalOrderCount = totalOrderCount == null ? 0 : totalOrderCount;

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }

        // 计算平均客单价
        Double unitPrice = 0.0;
        if (validOrderCount != 0) {
            unitPrice = turnover / validOrderCount;
        }

        // 查询新增用户数
        Integer newUsers = userMapper.countByMap(map);
        newUsers = newUsers == null ? 0 : newUsers;

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        // 查询待接单数量（状态为2）
        Integer waitingOrders = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        waitingOrders = waitingOrders == null ? 0 : waitingOrders;

        // 查询待派送数量（状态为3）
        Integer deliveredOrders = orderMapper.countByStatus(Orders.CONFIRMED);
        deliveredOrders = deliveredOrders == null ? 0 : deliveredOrders;

        // 查询已完成数量（状态为5）
        Integer completedOrders = orderMapper.countByStatus(Orders.COMPLETED);
        completedOrders = completedOrders == null ? 0 : completedOrders;

        // 查询已取消数量（状态为6）
        Integer cancelledOrders = orderMapper.countByStatus(Orders.CANCELLED);
        cancelledOrders = cancelledOrders == null ? 0 : cancelledOrders;

        // 查询全部订单数量
        Integer allOrders = orderMapper.countByMap(new HashMap<>());
        allOrders = allOrders == null ? 0 : allOrders;

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     * @return
     */
    @Override
    public DishOverViewVO getDishOverView() {
        // 查询已启售菜品数量（状态为1）
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        Integer sold = dishMapper.countByMap(map);
        sold = sold == null ? 0 : sold;

        // 查询已停售菜品数量（状态为0）
        map.put("status", 0);
        Integer discontinued = dishMapper.countByMap(map);
        discontinued = discontinued == null ? 0 : discontinued;

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     * @return
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        // 查询已启售套餐数量（状态为1）
        Map<String, Object> map = new HashMap<>();
        map.put("status", 1);
        Integer sold = setmealMapper.countByMap(map);
        sold = sold == null ? 0 : sold;

        // 查询已停售套餐数量（状态为0）
        map.put("status", 0);
        Integer discontinued = setmealMapper.countByMap(map);
        discontinued = discontinued == null ? 0 : discontinued;

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}