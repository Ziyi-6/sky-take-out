package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器 (用户端/C端)
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法,直接放行
            return true;
        }

        // 1、从请求头中获取令牌 (注意：这里使用的是 getUserTokenName)
        String token = request.getHeader(jwtProperties.getUserTokenName());

        // 2、校验令牌
        try {
                log.info("jwt校验:{}", token);
            // 使用用户端专属秘钥进行解析
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);

            // 从载荷中获取用户ID并转为 Long 类型
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);

            // 存入 ThreadLocal，方便后续 Service 获取当前登录用户
            BaseContext.setCurrentId(userId);

            // 3、校验通过,放行
            return true;
        } catch (Exception ex) {
            // 4、不通过,响应 401 状态码 (未授权)
            response.setStatus(401);
            return false;
        }
    }
}