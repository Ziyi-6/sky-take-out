package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    // 批量插入口味
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 批量删除菜品对应的味道
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);
}