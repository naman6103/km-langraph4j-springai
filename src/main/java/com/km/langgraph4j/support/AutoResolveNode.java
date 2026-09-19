package com.km.langgraph4j.support;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AutoResolveNode implements NodeAction<TicketState> {

    private final ChatClient chatClient;

    public AutoResolveNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Draft a short, friendly resolution message for a routine
                        IT support ticket. Three sentences maximum.
                        """)
                .build();
    }

    @Override
    public Map<String, Object> apply(TicketState state) {
        String resolution = chatClient.prompt()
                .user(state.ticketText())
                .call()
                .content();

        return Map.of(
                TicketState.RESOLUTION, resolution,
                TicketState.MESSAGES, "auto_resolve -> drafted resolution"
        );
    }
}
