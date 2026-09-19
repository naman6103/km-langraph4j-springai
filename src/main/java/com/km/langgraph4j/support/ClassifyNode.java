package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Every node only ever talks to the Spring AI {@link ChatClient} abstraction,
 * never to OllamaChatModel/OpenAiChatModel directly — so swapping the backend
 * in application.yml (see pom.xml) never touches this class.
 */
@Component
public class ClassifyNode implements NodeAction<TicketState> {

    private final ChatClient chatClient;

    public ClassifyNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You triage IT support tickets. Reply with exactly one word:
                        SIMPLE  - a password reset, access request, or other routine issue
                                  a first-line agent could resolve from a knowledge base.
                        COMPLEX - anything involving data loss, security, billing disputes,
                                  or an angry or escalated customer.
                        No punctuation, no explanation - one word only.
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(TicketState state) {
        String verdict = chatClient.prompt()
                .user(state.ticketText())
                .call()
                .content()
                .trim()
                .toUpperCase();

        String category = verdict.contains("COMPLEX") ? "COMPLEX" : "SIMPLE";
        String status = category.equals("COMPLEX") ? "AWAITING_REVIEW" : "CLASSIFIED";

        return Map.of(
                TicketState.CATEGORY, category,
                TicketState.STATUS, status,
                TicketState.MESSAGES, "classify -> " + category
        );
    }
}
