package serviceImpl;

import com.fromimport.chatgptweb.entity.ChatMessage;
import com.fromimport.chatgptweb.mapper.ChatMessageMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.serviceImpl.ChatMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChatMessageServiceImplTest {

    @InjectMocks
    private ChatMessageServiceImpl chatMessageServiceImpl;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveChatMessageSuccess() {
        Long userId = 1L;
        Long conversationId = 1L;
        String message = "Hello, World!";
        String sender = "user";

        // 设置 chatMessageMapper 的 insert 方法返回值
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1); // 模拟插入成功

        // 调用 saveChatMessage 方法
        Mono<Void> result = chatMessageServiceImpl.saveChatMessage(userId, conversationId, message, sender);

        // 验证 chatMessageMapper 的 insert 方法是否被调用
        verify(chatMessageMapper, times(1)).insert(any(ChatMessage.class));

        // 断言结果是否为空 (完成)
        assertDoesNotThrow(() -> result.block());
    }

    @Test
    void testSaveChatMessageException() {
        Long userId = 1L;
        Long conversationId = 1L;
        String message = "Hello, World!";
        String sender = "user";

        // 模拟 chatMessageMapper 的 insert 方法抛出异常
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenThrow(new RuntimeException("Database error"));

        // 调用 saveChatMessage 方法
        Mono<Void> result = chatMessageServiceImpl.saveChatMessage(userId, conversationId, message, sender);

        // 验证 chatMessageMapper 的 insert 方法是否被调用
        verify(chatMessageMapper, times(1)).insert(any(ChatMessage.class));

        // 断言结果是否为空 (完成)
        assertDoesNotThrow(() -> result.block());
    }
}