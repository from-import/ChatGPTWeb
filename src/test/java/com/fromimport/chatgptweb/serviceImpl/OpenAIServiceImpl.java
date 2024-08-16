package com.fromimport.chatgptweb.serviceImpl;import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;import com.fasterxml.jackson.databind.node.ArrayNode;import com.fasterxml.jackson.databind.node.ObjectNode;import com.fromimport.chatgptweb.service.OpenAIService;import lombok.extern.slf4j.Slf4j;import org.springframework.beans.factory.annotation.Value;import org.springframework.http.HttpEntity;import org.springframework.http.HttpHeaders;import org.springframework.http.HttpMethod;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.stereotype.Service;import org.springframework.web.client.RestTemplate;import reactor.core.publisher.Mono;@Service@Slf4jpublic class OpenAIServiceImpl implements OpenAIService {    @Value("${openai.api.key}")    private String apiKey; // OpenAI API的密钥    @Value("${openai.api.base}")    private String apiBase; // OpenAI API的基本URL    private final RestTemplate restTemplate; // 用于发送HTTP请求的RestTemplate    private final ObjectMapper objectMapper; // 用于处理JSON的ObjectMapper    public OpenAIServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {        this.restTemplate = restTemplate;        this.objectMapper = objectMapper;    }    @Override    public Mono<String> chatgpt(String message) {        int maxRetries = 3; // 最大重试次数        int retries = 0;        String url = apiBase + "/chat/completions"; // API的完整URL        while (retries < maxRetries) {            try {                // 设置HTTP头信息                HttpHeaders headers = new HttpHeaders();                headers.set("Authorization", "Bearer " + apiKey);                headers.set("Content-Type", "application/json");                // 使用 ObjectMapper 构建请求 JSON                ObjectNode requestJson = objectMapper.createObjectNode();                requestJson.put("model", "gpt-3.5-turbo");                ArrayNode messagesNode = requestJson.putArray("messages");                ObjectNode messageNode = messagesNode.addObject();                messageNode.put("role", "user");                messageNode.put("content", message);                requestJson.put("temperature", 0.7);                String jsonString = requestJson.toString();                log.info("请求 JSON: {}", jsonString);                HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, headers);                // 发送 POST 请求并接收响应                log.info("正在向 OpenAI API 发送请求，消息内容为: {}", message);                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);                if (response.getStatusCode() == HttpStatus.OK) {                    // 解析响应 JSON 以提取生成的内容                    JsonNode rootNode = objectMapper.readTree(response.getBody());                    JsonNode choicesNode = rootNode.path("choices");                    if (choicesNode.isArray() && choicesNode.size() > 0) {                        JsonNode firstChoiceNode = choicesNode.get(0);                        JsonNode responseMessageNode = firstChoiceNode.path("message");                        String content = responseMessageNode.path("content").asText();                        log.info("收到 OpenAI API 的响应: {}", content);                        return Mono.just(content);                    }                    log.info("响应中没有找到内容。");                    return Mono.just("响应中没有找到内容。");                } else {                    // 处理意外的 HTTP 状态码                    log.error("收到意外的 HTTP 状态码: {}", response.getStatusCode());                }            } catch (Exception e) {                // 处理错误                log.error("调用 API 过程中发生错误: {}", e.getMessage());            }            retries++;        }        log.error("经过 {} 次重试后仍未获得有效响应。", maxRetries);        return Mono.just("请求失败");    }}