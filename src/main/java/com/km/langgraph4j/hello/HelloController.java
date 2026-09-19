package com.km.langgraph4j.hello;

import com.km.langgraph4j.hello.langraph.HelloState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * curl -X POST localhost:8080/api/hello -H "Content-Type: application/json" \
 *      -d '{"message":"Lets begin!"}'
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    private final ChatClient chatClient;

    /*
    """
                        You are a bot who handle time related queries to people. Reply with exactly one sentence.
                        If the question is about time, ask the user about the timezone where he want to convert the time to.
                        if the time is not there then ask the user to provide time first
                        """
     */
    public HelloController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a bot who greets people. Reply with exactly one sentence.
                        """)
                .build();
    }


    public record HelloRequest(String message) {}

    @PostMapping
    public Map<String, Object> run(@RequestBody(required = false) HelloRequest request) throws Exception {
        String verdict = chatClient.prompt()
                .user(request.message)
                .call()
                .content()
                .trim();
        return Map.of("messages",Collections.singletonList(verdict));
    }
}
