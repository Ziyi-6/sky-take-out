package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 获取日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        
        // 遍历日期列表，查询每天的营业额
        for (LocalDate date : dateList) {
            // 查询指定日期完成的订单的总金额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            
            // 构建查询条件
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        // 封装结果数据
        return TurnoverReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(LocalDate::toString).toArray(String[]::new)))
                .turnoverList(String.join(",", turnoverList.stream().map(String::valueOf).toArray(String[]::new)))
                .build();
    }

    /**
     * 用户统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 获取日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的新增用户数
        List<Integer> newUserList = new ArrayList<>();
        // 存放截止到每天的总用户数
        List<Integer> totalUserList = new ArrayList<>();
        
        // 遍历日期列表，查询每天的用户数据
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            
            // 查询新增用户数（当天创建的用户）
            Map<String, Object> newUserMap = new HashMap<>();
            newUserMap.put("begin", beginTime);
            newUserMap.put("end", endTime);
            Integer newUserCount = userMapper.countByMap(newUserMap);
            newUserList.add(newUserCount == null ? 0 : newUserCount);
            
            // 查询总用户数（截止到当天，不限制开始时间）
            Map<String, Object> totalUserMap = new HashMap<>();
            totalUserMap.put("end", endTime);
            Integer totalUserCount = userMapper.countByMap(totalUserMap);
            totalUserList.add(totalUserCount == null ? 0 : totalUserCount);
        }

        // 封装结果数据
        return UserReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(LocalDate::toString).toArray(String[]::new)))
                .newUserList(String.join(",", newUserList.stream().map(String::valueOf).toArray(String[]::new)))
                .totalUserList(String.join(",", totalUserList.stream().map(String::valueOf).toArray(String[]::new)))
                .build();
    }

    /**
     * 订单统计
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 获取日期范围内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 存放每天的有效订单数（状态为已完成）
        List<Integer> validOrderCountList = new ArrayList<>();
        
        // 遍历日期列表，查询每天的订单数据
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            
            // 查询当天订单总数（不限制状态）
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("begin", beginTime);
            orderMap.put("end", endTime);
            Integer orderCount = orderMapper.countByMap(orderMap);
            orderCountList.add(orderCount == null ? 0 : orderCount);
            
            // 查询当天有效订单数（状态为已完成）
            Map<String, Object> validOrderMap = new HashMap<>();
            validOrderMap.put("begin", beginTime);
            validOrderMap.put("end", endTime);
            validOrderMap.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(validOrderMap);
            validOrderCountList.add(validOrderCount == null ? 0 : validOrderCount);
        }

        // 计算总订单数和有效订单数
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        Integer validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        
        // 计算订单完成率（避免除零）
        Double orderCompletionRate = 0.0;
        if (totalOrderCount > 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }

        // 封装结果数据
        return OrderReportVO.builder()
                .dateList(String.join(",", dateList.stream().map(LocalDate::toString).toArray(String[]::new)))
                .orderCountList(String.join(",", orderCountList.stream().map(String::valueOf).toArray(String[]::new)))
                .validOrderCountList(String.join(",", validOrderCountList.stream().map(String::valueOf).toArray(String[]::new)))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名Top10
     * @param begin 开始日期
     * @param end 结束日期
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        
        // 查询销量排名Top10
        List<Map<String, Object>> top10List = orderMapper.getSalesTop10(map);
        
        // 处理结果数据
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        
        for (Map<String, Object> item : top10List) {
            nameList.add(item.get("name").toString());
            numberList.add(Integer.valueOf(item.get("number").toString()));
        }

        // 封装结果数据
        return SalesTop10ReportVO.builder()
                .nameList(String.join(",", nameList))
                .numberList(String.join(",", numberList.stream().map(String::valueOf).toArray(String[]::new)))
                .build();
    }
}