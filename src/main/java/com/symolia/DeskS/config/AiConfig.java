package com.symolia.DeskS.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }
}