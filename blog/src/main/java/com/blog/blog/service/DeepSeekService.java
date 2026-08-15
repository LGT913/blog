package com.blog.blog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时:建立 TCP 握手的最大等待时间(防止目标服务器挂了)
        factory.setConnectTimeout(5000);     // 5 秒
        // 读取超时:发送请求后等待响应的最大时间(AI API 通常 2-30 秒)
        factory.setReadTimeout(30000);       // 30 秒
        this.restTemplate = new RestTemplate(factory);
    }

    public String generateSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "无内容可摘要";
        }

        // 清理内容中的控制字符（防止 JSON 解析失败）
        String cleanContent = content.replaceAll("[\\x00-\\x1F\\x7F]", "");

        try {
            // 1. 构建请求体（使用 ObjectMapper 安全构建 JSON）
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            // 构建 messages
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个文章摘要助手，请用100字以内简洁概括文章核心内容，只输出摘要，不要加任何解释或标题。");

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", cleanContent);

            requestBody.set("messages", objectMapper.createArrayNode().add(systemMsg).add(userMsg));

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            // 3. 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            // 4. 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String summary = root.path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText("摘要生成失败");

                // 清理摘要中的多余空格和换行
                return summary.trim().replaceAll("\\s+", " ");
            }

        } catch (Exception e) {
            System.err.println("调用 DeepSeek API 出错: " + e.getMessage());
            e.printStackTrace();
        }

        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
