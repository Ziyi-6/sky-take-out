package com.sky.mapper;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    @Insert("insert into orders (number, status, user_id, address_book_id, order_time, checkout_time, " +
            "pay_method, pay_status, amount, remark, phone, address, consignee, estimated_delivery_time, " +
            "delivery_status, tableware_number, tableware_status, pack_amount) " +
            "values (#{number}, #{status}, #{userId}, #{addressBookId}, #{orderTime}, #{checkoutTime}, " +
            "#{payMethod}, #{payStatus}, #{amount}, #{remark}, #{phone}, #{address}, #{consignee}, " +
            "#{estimatedDeliveryTime}, #{deliveryStatus}, #{tablewareNumber}, #{tablewareStatus}, #{packAmount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Orders orders);

    /**
     * 根据订单号和用户ID查询订单
     * @param number
     * @param userId
     * @return
     */
    @Select("select * from orders where number = #{number} and user_id = #{userId}")
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
    @Select("select count(id) from orders where user_id = #{userId} and status = #{status}")
    Integer countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 根据状态统计订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
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
}