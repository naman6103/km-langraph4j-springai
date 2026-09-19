package com.km.langgraph4j.support;

import com.km.langgraph4j.exception.TicketNotFoundException;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * curl -X POST localhost:8080/api/tickets -H "Content-Type: application/json" \
 *      -d '{"text":"My laptop wont boot after last nights update, I have lost a days work"}'
 * -> {"ticketId": "...", "status": "AWAITING_REVIEW", ...}
 *
 * curl -X POST localhost:8080/api/tickets/{ticketId}/decision \
 *      -H "Content-Type: application/json" -d '{"approved": true}'
 * -> {"ticketId": "...", "status": "RESOLVED", ...}
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final CompiledGraph<TicketState> ticketGraph;

    public TicketController(CompiledGraph<TicketState> ticketGraph) {
        this.ticketGraph = ticketGraph;
    }

    public record NewTicketRequest(String text) {}
    public record DecisionRequest(boolean approved) {}

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> create(@RequestBody NewTicketRequest request) {
        String ticketId = UUID.randomUUID().toString();

        // threadId is what ties this HTTP call to a checkpointed conversation.
        // The same threadId lets us resume this exact ticket later.
        var config = RunnableConfig.builder().threadId(ticketId).build();

        TicketState last = null;
        for (var output : ticketGraph.stream(
                Map.<String, Object>of(TicketState.TICKET_TEXT, request.text()), config)) {
            last = output.state();
        }

        return summarize(ticketId, last);
    }

    @PostMapping("/{ticketId}/decision")
    public Map<String, Object> decide(@PathVariable("ticketId") String ticketId,
                                       @RequestBody DecisionRequest request) throws Exception {
        var config = RunnableConfig.builder().threadId(ticketId).build();

        // Inject the human's decision into the checkpointed state, attributed
        // to the node that was waiting on it. updateState() returns a fresh
        // RunnableConfig pinned to the checkpoint it just wrote - use THAT to resume.
        RunnableConfig resumedConfig;
        try {
            resumedConfig = ticketGraph.updateState(config,
                    Map.<String, Object>of(TicketState.HUMAN_DECISION, request.approved() ? "APPROVED" : "REJECTED"),
                    "human_review");
        } catch (IllegalStateException e) {
            // Thrown by CompiledGraph.updateState() as "Missing Checkpoint!" when
            // ticketId doesn't match any saved thread - bad id, not a server bug.
            throw new TicketNotFoundException(ticketId, e);
        }

        // GraphInput.resume() - NOT a plain Map - tells the engine to continue
        // from resumedConfig's checkpoint. An empty Map here is indistinguishable
        // from "start a brand-new run with no args", which just replays the
        // thread from START and re-hits interruptBefore("human_review").
        TicketState last = null;
        for (var output : ticketGraph.stream(GraphInput.resume(), resumedConfig)) {
            last = output.state();
        }

        return summarize(ticketId, last);
    }

    private Map<String, Object> summarize(String ticketId, TicketState state) {
        if (state == null) {
            return Map.of("ticketId", ticketId, "status", "UNKNOWN");
        }
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("ticketId", ticketId);
        result.put("status", state.status());
        result.put("category", state.category());
        result.put("resolution", state.resolution().orElse(null));
        result.put("transcript", state.messages());
        return result;
    }
}
