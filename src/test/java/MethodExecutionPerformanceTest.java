import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromimport.chatgptweb.service.ChatMessageService;
import com.fromimport.chatgptweb.service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashMap;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MethodExecutionPerformanceTest {

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final int MESSAGE_COUNT = 100;

    @Test
    public void testAverageExecutionTime() throws Exception {
        AtomicLong totalExecutionTime = new AtomicLong(0);

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String payload = generateTestPayload(i);
            long startTime = System.currentTimeMillis();

            Future<?> future = taskExecutor.submit(() -> {
                try {
                    Map<String, Object> data = new ObjectMapper().readValue(payload, Map.class);
                    String userId = (String) data.get("userId");
                    String conversationId = (String) data.get("conversationId");
                    String message = (String) data.get("message");

                    openAIService.chatgpt(message)
                            .subscribe(response -> {
                                chatMessageService.saveChatMessage(Long.parseLong(userId), Long.parseLong(conversationId), response, "chatgpt");
                                redisTemplate.opsForValue().set("chat_response_" + conversationId, response);
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            future.get(30, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            totalExecutionTime.addAndGet(executionTime);
            System.out.println("单次执行时间: " + executionTime + " ms");
        }

        long averageExecutionTime = totalExecutionTime.get() / MESSAGE_COUNT;
        System.out.println("平均执行时间: " + averageExecutionTime + " ms");
    }

    private String generateTestPayload(int index) {
        return "{ \"userId\": \"" + index + "\", \"conversationId\": \"" + index + "\", \"message\": \"Test message " + index + "\" }";
    }
}