# RestClient vs WebClient vs OpenFeign: Interview Cheat Sheet

## One-Liners
| Client | One-liner |
|---|---|
| **RestClient** | Modern **synchronous** fluent client (Spring 6.1+), the successor to RestTemplate. |
| **WebClient** | **Non-blocking reactive** client (`Mono`/`Flux`) from WebFlux. |
| **OpenFeign** | **Declarative** interface-based client, ideal for microservice-to-microservice calls. |

## Comparison Table
| Feature | RestClient | WebClient | OpenFeign |
|---|---|---|---|
| Programming model | Imperative, fluent | Reactive, fluent | Declarative (interface) |
| Blocking? | Yes | No (can `block()`) | Yes |
| Return types | Plain objects | `Mono<T>` / `Flux<T>` | Plain objects |
| Extra dependency | None (Spring Web) | `spring-boot-starter-webflux` | `spring-cloud-starter-openfeign` |
| Streaming / SSE | Limited | Excellent | Limited |
| Parallel calls | Manual (threads) | Easy (`Mono.zip`) | Manual |
| Service discovery / LB | Manual | Manual (`@LoadBalanced` builder) | Built-in |
| Circuit breaker | Manual (Resilience4j) | Manual (Resilience4j) | `fallback` + Resilience4j |
| Boilerplate | Low | Medium | Lowest |
| Best for | Simple sync calls | Reactive apps, high concurrency | Microservices |

## When to Use What
- **Spring MVC + simple calls to an external API** → `RestClient`
- **WebFlux app / streaming / many concurrent calls / backpressure** → `WebClient`
- **Many internal microservice calls, Eureka, circuit breakers** → `OpenFeign`
- Spring MVC app that also wants a declarative style without Spring Cloud → `@HttpExchange` on top of `RestClient`

## Evolution
`RestTemplate` (legacy, maintenance mode) → `WebClient` (Spring 5, reactive) → `RestClient` (Spring 6.1, sync fluent)

## Same Call in All Three
```java
// RestClient
User u = restClient.get().uri("/users/{id}", 1).retrieve().body(User.class);

// WebClient
Mono<User> u = webClient.get().uri("/users/{id}", 1).retrieve().bodyToMono(User.class);

// OpenFeign
@GetMapping("/users/{id}") User getUser(@PathVariable("id") Long id);   // then: userClient.getUser(1);
```

## Common Follow-up Questions
1. **Is RestTemplate deprecated?** Not formally deprecated, but in maintenance mode. New code should use RestClient (or WebClient).
2. **Can I use WebClient without WebFlux app?** Yes, add the webflux starter; use `block()` or return `Mono` from MVC controllers.
3. **Which handles 10,000 concurrent slow calls better?** WebClient, because it doesn't tie up a thread per request (with Java 21 virtual threads, RestClient/Feign also scale much better).
4. **How to secure calls between microservices?** Pass JWT via interceptor (Feign), `ExchangeFilterFunction` (WebClient), or `ClientHttpRequestInterceptor` (RestClient).
5. **Which handles retries?** All can, but: Feign has `Retryer`, WebClient has `retryWhen`, RestClient needs Resilience4j / Spring Retry.
6. **Timeouts?** RestClient → request factory; WebClient → Reactor Netty `HttpClient`; Feign → `spring.cloud.openfeign.client.config.*`.
