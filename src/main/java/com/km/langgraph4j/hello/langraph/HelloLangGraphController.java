package com.km.langgraph4j.hello.langraph;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * curl -X POST localhost:8080/api/hello -H "Content-Type: application/json" \
 *      -d '{"message":"Lets begin!"}'
 */
@RestController
@RequestMapping("/api/langGraph/hello")
public class HelloLangGraphController {

    private final CompiledGraph<HelloState> helloGraph;

    public HelloLangGraphController(CompiledGraph<HelloState> helloGraph) {
        this.helloGraph = helloGraph;
    }

    public record HelloRequest(String message) {}

    @PostMapping
    public Map<String, Object> run(@RequestBody(required = false) HelloRequest request) throws Exception {
        String message = (request != null && request.message() != null)
                ? request.message()
                : "Let's begin!";

        var config = RunnableConfig.builder()
                .threadId(UUID.randomUUID().toString())
                .build();

        // invoke() runs the graph synchronously to completion and hands back
        // the final state (empty only if the graph produced no output at all).
        HelloState finalState = helloGraph
                .invoke(Map.<String, Object>of(HelloState.MESSAGES_KEY, message), config)
                .orElseThrow();

        List<String> messages = finalState.messages();
        return Map.of("messages", messages);
    }
}
