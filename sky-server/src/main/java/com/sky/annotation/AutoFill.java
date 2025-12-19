package com.sky.annotation;

import com.sky.enumeration.OperationType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标识某个方法需要进行功能字段自动填充处理
 */
// 元注解1：限定注解只能贴在“方法”上
@Target(ElementType.METHOD)
// 元注解2：注解在程序运行时存在，AOP能读取到
@Retention(RetentionPolicy.RUNTIME)
// 定义自定义注解的核心语法
public @interface AutoFill {
    // 注解的属性：存储“数据库操作类型”（只能传INSERT/UPDATE）
    OperationType value();
}