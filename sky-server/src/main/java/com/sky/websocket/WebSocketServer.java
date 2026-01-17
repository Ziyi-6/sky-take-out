package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WebSocket服务端
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    /**
     * 存储所有连接的客户端会话
     * key: sid (会话ID)
     * value: WebSocket会话
     */
    private static ConcurrentMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     * @param session 客户端会话
     * @param sid 客户端标识
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
        log.info("WebSocket连接建立成功，sid：{}，当前连接数：{}", sid, sessionMap.size());
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送的消息
     * @param sid 客户端标识
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("收到来自客户端{}的消息：{}", sid, message);
        // 可以在这里处理客户端发送的消息，目前主要是服务端向客户端发送消息
        // 例如：可以回复确认消息
        sendToClient(sid, "服务器已收到消息：" + message);
    }

    /**
     * 连接关闭调用的方法
     * @param sid 客户端标识
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
        log.info("WebSocket连接关闭，sid：{}，当前连接数：{}", sid, sessionMap.size());
    }

    /**
     * 发生错误时调用
     * @param session 客户端会话
     * @param error 错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误：", error);
    }

    /**
     * 向指定客户端发送消息
     * @param sid 客户端标识
     * @param message 消息内容
     */
    public void sendToClient(String sid, String message) {
        Session session = sessionMap.get(sid);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("向客户端{}发送消息：{}", sid, message);
            } catch (IOException e) {
                log.error("向客户端{}发送消息失败：", sid, e);
            }
        } else {
            log.warn("客户端{}未连接或连接已关闭", sid);
        }
    }

    /**
     * 向所有客户端群发消息
     * @param message 消息内容
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("向客户端发送消息失败：", e);
                }
            }
        }
        log.info("向所有客户端群发消息，消息内容：{}，当前连接数：{}", message, sessionMap.size());
    }

    /**
     * 获取当前连接数
     * @return 连接数
     */
    public int getConnectionCount() {
        return sessionMap.size();
    }

    /**
     * 检查指定客户端是否连接
     * @param sid 客户端标识
     * @return 是否连接
     */
    public boolean isConnected(String sid) {
        Session session = sessionMap.get(sid);
        return session != null && session.isOpen();
    }
}