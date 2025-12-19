package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 处理SQL异常（例如：录入重复的用户名）
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        // 异常信息示例：Duplicate entry 'zhangsan' for key 'employee.idx_username'
        String message = ex.getMessage();

        // 如果包含 "Duplicate entry" 这个关键词，说明是重复录入
        if (message.contains("Duplicate entry")) {
            // 1. 按照空格切割字符串
            String[] split = message.split(" ");
            // 2. 取出第三个元素（就是重复的那个用户名，比如 'zhangsan'）
            String username = split[2];
            // 3. 拼接提示信息
            String msg = username + MessageConstant.ALREADY_EXISTS; // 需要去MessageConstant里确认一下常量
            return Result.error(msg);
        } else {
            // 其他未知错误
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }

}
