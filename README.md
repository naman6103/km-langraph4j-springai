# LangGraph4j + Spring AI — KM Session Companion Code

Two graphs live in this project:

| Package | What it is | Covers |
|---|---|---|
| `com.km.langgraph4j.hello` | The **exercise starter**: a 2-node "hello world" graph | State, Nodes, Edges, running a graph |
| `com.km.langgraph4j.support` | The **live demo**: an IT-support ticket triage agent | Conditional edges, checkpointing, human-in-the-loop, multi-graph, observability |

Both graphs are verified to compile against the *real* `langgraph4j-core:1.8.24`
and `spring-ai:1.0.6` jars (not just the docs — see "A note on accuracy" below).

## Run it

### Option A — Docker (recommended for the workshop)

```bash
docker compose up --build
```

This starts an `ollama` container (pulls `qwen2.5:7b` on first boot — a few
minutes and a few GB the first time only) and the Spring Boot `app` container.
The app waits for Ollama to report healthy before starting.

### Option B — locally, against your own Ollama

```bash
ollama pull qwen2.5:7b
ollama serve   # if not already running

mvn spring-boot:run
```

### Swapping to OpenAI instead of Ollama

Nodes only ever talk to Spring AI's `ChatClient` abstraction, so the swap is
config-only:

1. Uncomment the `spring-ai-starter-model-openai` dependency in `pom.xml`.
2. Uncomment the `spring.ai.openai` block in `application.yml`.
3. `export OPENAI_API_KEY=sk-...`

No node, edge, or graph-wiring code changes.

## Useful commands

```bash
# See running containers
docker ps

# Stop a container (by name or ID from docker ps)
docker stop <container>

# Stop your host's own Ollama (in case it's holding port 11434)
sudo systemctl stop ollama

# Stop and remove this project's containers
docker compose down

# Check the Ollama model is up and serving
curl --location 'http://localhost:11434/api/tags'
```

## Try it

```bash
# Hello world — the exercise starter
curl -X POST localhost:8080/api/hello -H "Content-Type: application/json" \
     -d '{"message":"Lets begin!"}'

# Hello world — LangGraph4j-flavored variant
curl -X POST localhost:8080/api/langGraph/hello -H "Content-Type: application/json" \
     -d '{"message":"Lets begin!"}'

# A routine ticket — auto-resolves, no pause
curl -X POST localhost:8080/api/tickets -H "Content-Type: application/json" \
     -d '{"text":"I forgot my VPN password, can you reset it?"}'

# A ticket that pauses for a human (note the "AWAITING_REVIEW" status + ticketId)
curl -X POST localhost:8080/api/tickets -H "Content-Type: application/json" \
     -d '{"text":"Production database is down and we are losing customer data"}'

# Resume it — approve or reject
curl -X POST localhost:8080/api/tickets/<ticketId>/decision \
     -H "Content-Type: application/json" -d '{"approved": true}'

# List available graph inspection URLs
curl localhost:8080/api/graphs

# See both compiled graphs as Mermaid diagrams (paste into mermaid.live)
curl localhost:8080/api/graphs/hello/mermaid
curl localhost:8080/api/graphs/ticket/mermaid
```

## Project layout

```
src/main/java/com/km/langgraph4j/
├── KmApplication.java
├── hello/                      # exercise starter
│   ├── HelloState.java
│   ├── GreeterNode.java
│   ├── ResponderNode.java
│   ├── HelloGraphConfig.java   # graph wiring -> CompiledGraph<HelloState> bean
│   └── HelloController.java
├── support/                    # live demo
│   ├── TicketState.java
│   ├── ClassifyNode.java       # LLM call: SIMPLE vs COMPLEX
│   ├── AutoResolveNode.java    # LLM call: draft a resolution
│   ├── HumanReviewNode.java    # the interrupt point
│   ├── EscalateNode.java
│   ├── FinalizeNode.java
│   ├── TicketGraphConfig.java  # graph wiring + checkpointing + interruptBefore
│   └── TicketController.java   # create / decision REST endpoints
└── config/
    └── GraphInspectionController.java  # multi-graph + Mermaid export

src/test/java/com/km/langgraph4j/
├── hello/HelloGraphTest.java   # whole graph, no Spring, no LLM
└── support/RoutingTest.java    # conditional-edge logic in isolation
```

## Testing

```bash
mvn test
```

Nodes are plain functions of state and edges are plain functions returning a
routing key, so most of the graph's *logic* — including the human-in-the-loop
branch — is testable with zero Spring context and zero LLM calls. Only the
two LLM-calling nodes (`ClassifyNode`, `AutoResolveNode`) need a real or
mocked `ChatClient`.

## A note on accuracy

Several LangGraph4j blog posts and even parts of the project's own README
reference an `execute()` method and a `CompletableFuture`-returning
`invoke()` that **do not exist** in `langgraph4j-core:1.8.24`. This codebase
was compiled and unit-tested against the real jar (`javap`-verified) rather
than copied from docs, so:

- Use `invoke(Map<String,Object>, RunnableConfig)` → `Optional<State>`, or
  `stream(Map<String,Object>, RunnableConfig)` → iterable of
  `NodeOutput<State>` (`.node()` / `.state()`).
- `updateState(RunnableConfig, Map<String,Object>, String asNode)` returns a
  **new** `RunnableConfig` — use the returned one, not the original, for the
  resume call.
- `Map.of(k, v)` infers `Map<String,String>`, which will **not** satisfy a
  `Map<String,Object>` parameter — use `Map.<String,Object>of(...)`.

If you're on a different LangGraph4j version, run
`javap -p -classpath <jar> org.bsc.langgraph4j.CompiledGraph` yourself before
trusting a blog post (including this one).

## See also

- [LangGraph4j](https://github.com/langgraph4j/langgraph4j)
- [Spring AI](https://docs.spring.io/spring-ai/reference/)
