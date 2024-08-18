package com.fromimport.chatgptweb.controller;

import com.fromimport.chatgptweb.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
