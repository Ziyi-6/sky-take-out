package com.sky.controller.user;


import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("unserDishController")
@RequestMapping("/user/dish")
@Api(tags = "小程序端-菜品浏览接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        //1. 构造Redis 中的key (例如：dish_10)
        String key = "dish_" + categoryId;
        //2.查询redis中是否存在数据菜品
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //3.如果存在，直接返回无需查询数据库
        if(list != null){
            log.info("从 Redis 缓存中获取菜品数据，key: {}", key);
            return Result.success(list);
        }

        //4.如果不存在，查询数据库
        log.info("Redis 缓存未命中，开始查询数据库...");
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//仅查询起售中的菜品
        list = dishService.listWithFlavor(dish);
        // 5. 将查询到的数据放入 redis 中，方便下次使用
        redisTemplate.opsForValue().set(key, list);
        return Result.success(list);
    }
}
