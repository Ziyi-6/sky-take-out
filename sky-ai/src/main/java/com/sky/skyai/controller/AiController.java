package com.sky.skyai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final ChatClient chatClient;

    // Spring AI 自动注入 ChatClient，无需我们手动配置
    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "msg") String msg) {
        // 1. 设定人设（System Prompt）- 这里还是先写死，作为 MVP 版本
        String systemPrompt = """
                你是一个苍穹外卖的智能营养师助手。
                你的语气要亲切、幽默。
                我们的菜单只有以下几个（必须严格基于此回答，不要编造）：
                1. 宫保鸡丁（28元，微辣，热量中等）
                2. 清炒西兰花（18元，清淡，低热量，适合减肥）
                3. 滋补乌鸡汤（38元，大补，适合身体虚弱）
                4. 米饭（2元）
                如果用户问其他菜，请委婉告知本店暂时没有。
                """;

        // 2. 调用 DeepSeek 大模型
        String response = chatClient.prompt()
                .system(systemPrompt)  // 告诉 AI 它是谁
                .user(msg)             // 告诉 AI 用户问了什么
                .call()
                .content();            // 获取回答

        return response;
    }
}