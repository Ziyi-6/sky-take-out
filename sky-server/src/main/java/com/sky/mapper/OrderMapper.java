package com.sky.mapper;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号和用户ID查询订单
     * @param number
     * @param userId
     * @return
     */
    Orders getByNumberAndUserId(String number, Long userId);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    Orders getById(Long id);

    /**
     * 更新订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    List<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据用户ID和状态查询订单数量
     * @param userId
     * @param status
     * @return
     */
    Integer countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 根据状态统计订单数量
     * @param status
     * @return
     */
    Integer countByStatus(Integer status);

    /**
     * 根据状态和下单时间查询订单
     * @param status 订单状态
     * @param time 下单时间
     * @return
     */
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

    /**
     * 根据状态查询订单
     * @param status 订单状态
     * @return
     */
    List<Orders> getByStatus(Integer status);

    /**
     * 根据条件统计营业额
     * @param map 查询条件
     * @return
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 根据条件统计订单数量
     * @param map 查询条件
     * @return
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 查询销量排名Top10
     * @param map 查询条件
     * @return
     */
    List<Map<String, Object>> getSalesTop10(Map<String, Object> map);
}