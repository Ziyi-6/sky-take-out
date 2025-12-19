package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect // 表示我是个切面类
@Component // 交给 Spring 管理
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点：拦截 com.sky.mapper 包下，所有加了 @AutoFill 注解的方法
     */
    //@Pointcut（里面装的是拦截规则）
    //哪些会被拦截？
    // 1. 在com.sky.mapper包下
    // 2. 方法上有@AutoFill注解
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    //这个方法本身“什么都不干”，它就是个“变量名”或者“快捷方式”。
    //它存在的唯一意义，就是让 @Pointcut 注解有个地方挂着，方便后面引用。
    public void autoFillPointCut() {}

    /**
     * 前置通知：在方法执行前，进行自动填充
     */
    //如果没有public void autoFillPointCut() {}
    //在这里就需要写@Before("execution(* com.sky.mapper.*.*(..)) ...")太累了，而且万一你要改路线，得改好几个地方。
    @Before("autoFillPointCut()")
    //拦截后要做的事情执行autoFIll()这个方法
    //JoinPoint joinPoint  AOP 自动传入的参数，装着 “被拦截方法的所有信息”（比如方法名、参数、所属类）
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        // 1. 获取当前被拦截的方法上的数据库操作类型 (INSERT 还是 UPDATE?)

        //joinPoint.getSignature()从 “访客信息单” 里拿 “方法签名”（方法的唯一标识）
        //强制转成 MethodSignature（因为要获取方法对象）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 方法签名
        //signature.getMethod() 获取被拦截的方法对象（比如 insert (Employee)、update (Category)）
        //getAnnotation(AutoFill.class) 从方法对象上拿到贴的@AutoFill注解
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); // 获得注解
        //autoFill.value()从 @AutoFill 注解里拿到操作类型（OperationType.INSERT/UPDATE）
        OperationType operationType = autoFill.value(); // 获得操作类型

        // 2. 获取到当前被拦截的方法的参数--实体对象 (比如 Employee, Category)
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0]; // 约定：第一个参数必须是实体对象

        // 3. 准备赋值的数据：当前时间和当前用户ID
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        // 4. 根据不同操作类型，为对应的属性通过反射赋值
        if (operationType == OperationType.INSERT) {
            // 如果是新增，4个字段都要填
            try {

                // 利用反射获取 set 方法：第一个参数是方法名，第二个是方法的参数类型
                //getDeclaredMethod(方法名, 参数类型) 从类里找指定名称、指定参数类型的方法
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);


                // 执行 set 方法：第一个参数是“要赋值的对象”，第二个是“要赋的值”
                //method.invoke(对象, 值)   调用这个 set 方法，给对象的字段赋值
                //上面拿到的是大类的赋值方法，具体传给哪个具体的对象还要再传一遍 所以传了entity这个参数
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (operationType == OperationType.UPDATE) {
            // 如果是修改，只填2个字段
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}