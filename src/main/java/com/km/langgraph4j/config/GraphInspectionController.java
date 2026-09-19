package com.km.langgraph4j.config;

import com.km.langgraph4j.hello.langraph.HelloState;
import com.km.langgraph4j.support.TicketState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Two things live in this one controller on purpose:
 *
 *  1. Multiple graphs, one Spring context: {@code helloGraph} and
 *     {@code ticketGraph} are both just CompiledGraph beans, injected like
 *     any other Spring bean and free to coexist, be tested, and be called
 *     independently.
 *
 *  2. Observability: CompiledGraph#getGraph(...) generates a Mermaid (or
 *     PlantUML) diagram straight from the compiled graph, so the diagram can
 *     never drift out of sync with the code. Paste the output into
 *     https://mermaid.live to render it, or view it live in LangGraph4j Studio.
 */
@RestController
@RequestMapping("/api/graphs")
public class GraphInspectionController {

    private final CompiledGraph<HelloState> helloGraph;
    private final CompiledGraph<TicketState> ticketGraph;

    public GraphInspectionController(CompiledGraph<HelloState> helloGraph,
                                      CompiledGraph<TicketState> ticketGraph) {
        this.helloGraph = helloGraph;
        this.ticketGraph = ticketGraph;
    }

    @GetMapping
    public Map<String, String> list() {
        return Map.of(
                "hello", "/api/graphs/langGraph/hello/mermaid",
                "ticket", "/api/graphs/ticket/mermaid"
        );
    }

    @GetMapping(value = "/hello/mermaid", produces = "text/plain")
    public String helloMermaid() {
        return helloGraph.getGraph(GraphRepresentation.Type.MERMAID, "Hello World Graph").content();
    }

    @GetMapping(value = "/ticket/mermaid", produces = "text/plain")
    public String ticketMermaid() {
        return ticketGraph.getGraph(GraphRepresentation.Type.MERMAID, "IT Support Ticket Triage").content();
    }
}
