package com.fromimport.chatgptweb.common;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理来自客户端的消息
    }

    public void sendMessageToClient(WebSocketSession session, String message) throws Exception {
        session.sendMessage(new TextMessage(message));
    }
}
