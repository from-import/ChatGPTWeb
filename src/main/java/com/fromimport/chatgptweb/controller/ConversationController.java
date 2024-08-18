package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/history")
    public List<Map<String, Object>> getConversationHistory(@RequestParam Long userId) {
        return conversationService.getConversationHistoryWithFirstMessage(userId);
    }

    // 根据 conversationId 获取详细对话记录的实现
    // 返回详细对话记录的响应
    @GetMapping("/{conversationId}")
    public List<Map<String, Object>> getConversationDetail(@PathVariable String conversationId) {
        return conversationService.getConversationHistoryWithConversationId(conversationId);
    }


}
