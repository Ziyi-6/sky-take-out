package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersCancelDTO;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    
    @Autowired
    private AddressBookMapper addressBookMapper;
    
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 校验地址ID是否真实存在
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException("地址不存在");
        }

        // 2. 校验当前用户购物车是否为空
        Long userId = getCurrentUserId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException("购物车为空");
        }

        // 3. 向orders表插入1条记录
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setPhone(addressBook.getPhone());
        // 完善地址拼接：省+市+区+详细地址
        String fullAddress = addressBook.getProvinceName() + addressBook.getCityName() + 
                           addressBook.getDistrictName() + addressBook.getDetail();
        order.setAddress(fullAddress);
        order.setConsignee(addressBook.getConsignee());
        order.setUserId(userId);

        orderMapper.insert(order);

        // 4. 向order_detail表批量插入购物车中的商品明细
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(order.getId());
            orderDetail.setName(cart.getName());
            orderDetail.setImage(cart.getImage());
            orderDetail.setDishId(cart.getDishId());
            orderDetail.setSetmealId(cart.getSetmealId());
            orderDetail.setDishFlavor(cart.getDishFlavor());
            orderDetail.setNumber(cart.getNumber());
            orderDetail.setAmount(cart.getAmount());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 5. 下单成功后，清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 6. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 模拟支付成功
     * @param orderNumber
     */
//    @Override
//    public void paySuccess(String orderNumber) {
//        // 1. 根据订单号查询订单
//        Orders ordersDB = orderMapper.getByNumberAndUserId(orderNumber, BaseContext.getCurrentId());
//
//        // 2. 构造新对象，只修改需要更新的字段（符合开发规范）
//        Orders orders = Orders.builder()
//                .id(ordersDB.getId())
//                .status(Orders.TO_BE_CONFIRMED) // 状态改为 2（待接单）
//                .payStatus(Orders.PAID)         // 支付状态改为 1（已支付）
//                .checkoutTime(LocalDateTime.now()) // 记录结账时间
//                .build();
//
//        // 3. 执行更新
//        orderMapper.update(orders);
//    }

    /**
     * 查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 设置当前用户ID
        Long userId = BaseContext.getCurrentId();
        
        // 开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        
        // 设置用户ID到DTO中
        ordersPageQueryDTO.setUserId(userId);
        
        // 分页查询 - 这里返回的是List<Orders>，需要用PageInfo包装
        List<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);
        PageInfo<Orders> pageInfo = new PageInfo<>(ordersList);
        
        // 遍历订单列表，为每个订单查询订单明细
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders order : ordersList) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            
            // 查询订单明细
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());
            orderVO.setOrderDetailList(orderDetailList);
            
            // 构建订单菜品信息字符串
            StringBuilder orderDishes = new StringBuilder();
            for (OrderDetail orderDetail : orderDetailList) {
                orderDishes.append(orderDetail.getName()).append("*").append(orderDetail.getNumber()).append(";");
            }
            orderVO.setOrderDishes(orderDishes.toString());
            
            orderVOList.add(orderVO);
        }
        
        return new PageResult(pageInfo.getTotal(), orderVOList);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        // 查询订单基本信息
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单是否属于当前用户
        Long currentUserId = BaseContext.getCurrentId();
        if (!order.getUserId().equals(currentUserId)) {
            throw new OrderBusinessException("无权查看该订单");
        }
        
        // 查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        
        // 构建OrderVO对象
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        
        // 构建订单菜品信息字符串
        StringBuilder orderDishes = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDishes.append(orderDetail.getName()).append("*").append(orderDetail.getNumber()).append(";");
        }
        orderVO.setOrderDishes(orderDishes.toString());
        
        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    @Transactional
    public void cancelOrder(Long id) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单是否属于当前用户
        Long currentUserId = BaseContext.getCurrentId();
        if (!ordersDB.getUserId().equals(currentUserId)) {
            throw new OrderBusinessException("无权取消该订单");
        }
        
        // 校验订单状态：只有状态为待付款(1)或待接单(2)时才允许取消
        if (ordersDB.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException("当前订单状态不允许取消");
        }
        
        // 构造更新对象
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED) // 状态改为6（已取消）
                .cancelReason("用户主动取消")
                .cancelTime(LocalDateTime.now())
                .build();
        
        // 如果是已支付（待接单）状态取消，将支付状态设置为退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND); // 支付状态改为退款
            log.info("订单{}已支付，模拟退款处理，支付状态设置为退款", id);
        }
        
        // 执行更新
        orderMapper.update(orders);
    }

    /**
     * 根据条件分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        
        // 分页查询 - 管理端查询不需要设置userId，可以查询所有订单
        List<Orders> ordersList = orderMapper.pageQuery(ordersPageQueryDTO);
        PageInfo<Orders> pageInfo = new PageInfo<>(ordersList);
        
        // 遍历订单列表，为每个订单查询订单明细
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders order : ordersList) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            
            // 查询订单明细
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());
            orderVO.setOrderDetailList(orderDetailList);
            
            // 构建订单菜品信息字符串
            StringBuilder orderDishes = new StringBuilder();
            for (OrderDetail orderDetail : orderDetailList) {
                orderDishes.append(orderDetail.getName()).append("*").append(orderDetail.getNumber()).append(";");
            }
            orderVO.setOrderDishes(orderDishes.toString());
            
            orderVOList.add(orderVO);
        }
        
        return new PageResult(pageInfo.getTotal(), orderVOList);
    }

    /**
     * 获取当前登录用户ID
     * @return
     */
    private Long getCurrentUserId() {
        return BaseContext.getCurrentId();
    }

    /**
     * 各个状态订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 分别查询状态为2（待接单）、3（已接单）、4（派送中）的订单数量
        Integer toBeConfirmedCount = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED); // 2
        Integer confirmedCount = orderMapper.countByStatus(Orders.CONFIRMED); // 3
        Integer deliveryInProgressCount = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS); // 4
        
        // 构建OrderStatisticsVO对象
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmedCount);
        orderStatisticsVO.setConfirmed(confirmedCount);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgressCount);
        
        return orderStatisticsVO;
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    @Override
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单状态：只有待接单状态才能接单
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException("当前订单状态不允许接单");
        }
        
        // 构造更新对象，将订单状态修改为已接单
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CONFIRMED) // 状态改为3（已接单）
                .build();
        
        // 执行更新
        orderMapper.update(orders);
        
        log.info("商家接单成功，订单ID：{}", ordersConfirmDTO.getId());
    }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单状态：只有待接单状态才能拒单
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException("当前订单状态不允许拒单");
        }
        
        // 如果订单已支付，需要退款
        if (ordersDB.getPayStatus() != null && ordersDB.getPayStatus().equals(Orders.PAID)) {
            // 模拟退款逻辑：修改支付状态为退款
            Orders refundOrder = Orders.builder()
                    .id(ordersDB.getId())
                    .payStatus(Orders.REFUND) // 支付状态改为退款
                    .build();
            orderMapper.update(refundOrder);
            log.info("订单已支付，执行退款操作，订单ID：{}", ordersRejectionDTO.getId());
        }
        
        // 构造更新对象，将订单状态修改为已取消，并记录拒单原因和时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED) // 状态改为6（已取消）
                .rejectionReason(ordersRejectionDTO.getRejectionReason()) // 记录拒单原因
                .cancelTime(LocalDateTime.now()) // 记录取消时间
                .build();
        
        // 执行更新
        orderMapper.update(orders);
        
        log.info("商家拒单成功，订单ID：{}，拒单原因：{}", ordersRejectionDTO.getId(), ordersRejectionDTO.getRejectionReason());
    }

    /**
     * 派送订单
     * @param id 订单ID
     */
    @Override
    @Transactional
    public void delivery(Long id) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单状态：只有已接单状态才能派送
        if (!ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException("当前订单状态不允许派送");
        }
        
        // 构造更新对象，将订单状态修改为派送中
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS) // 状态改为4（派送中）
                .build();
        
        // 执行更新
        orderMapper.update(orders);
        
        log.info("订单派送成功，订单ID：{}", id);
    }

    /**
     * 完成订单
     * @param id 订单ID
     */
    @Override
    @Transactional
    public void complete(Long id) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 校验订单状态：只有派送中状态才能完成
        if (!ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException("当前订单状态不允许完成");
        }
        
        // 构造更新对象，将订单状态修改为已完成，并记录送达时间
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED) // 状态改为5（已完成）
                .deliveryTime(LocalDateTime.now()) // 记录送达时间
                .build();
        
        // 执行更新
        orderMapper.update(orders);
        
        log.info("订单完成成功，订单ID：{}", id);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    @Transactional
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        // 查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        
        // 如果订单已支付，需要退款
        if (ordersDB.getPayStatus() != null && ordersDB.getPayStatus().equals(Orders.PAID)) {
            // 模拟退款逻辑：修改支付状态为退款
            Orders refundOrder = Orders.builder()
                    .id(ordersCancelDTO.getId())
                    .payStatus(Orders.REFUND) // 支付状态改为退款
                    .build();
            orderMapper.update(refundOrder);
            log.info("订单已支付，执行退款操作，订单ID：{}", ordersCancelDTO.getId());
        }
        
        // 构造更新对象，将订单状态修改为已取消，并记录取消原因和时间
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED) // 状态改为6（已取消）
                .cancelReason(ordersCancelDTO.getCancelReason()) // 记录取消原因
                .cancelTime(LocalDateTime.now()) // 记录取消时间
                .build();
        
        // 执行更新
        orderMapper.update(orders);
        
        log.info("商家取消订单成功，订单ID：{}，取消原因：{}", ordersCancelDTO.getId(), ordersCancelDTO.getCancelReason());
    }

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, null);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        
        // 通过WebSocket向客户端推送消息 type=1表示来单提醒
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1表示来单提醒
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);
        
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        
        log.info("支付成功，订单号：{}，已向客户端推送来单提醒", outTradeNo);
    }
}