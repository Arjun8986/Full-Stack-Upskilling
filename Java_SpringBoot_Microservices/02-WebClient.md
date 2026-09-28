# WebClient (Spring WebFlux)

## Quick Notes
- **Non-blocking, reactive** HTTP client built on **Project Reactor** (`Mono` / `Flux`).
- Part of `spring-boot-starter-webflux`. Default engine is **Reactor Netty**.
- Supports **sync (`block()`)** and **async** usage, and **streaming** responses (SSE, large payloads).
- Best for: WebFlux apps, high concurrency, parallel calls, streaming, backpressure.
- Calling `.block()` inside a reactive pipeline (event-loop thread) is an anti-pattern.

## Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```
> Can be used in a Spring MVC app too, alongside `spring-boot-starter-web`.

## Configuration (Bean with Timeouts)
```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
```

## CRUD Examples (Reactive)
```java
@Service
public class UserWebClient {

    private final WebClient webClient;

    public UserWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // GET single -> Mono
    public Mono<User> getUser(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(User.class);
    }

    // GET list -> Flux
    public Flux<User> getAllUsers() {
        return webClient.get()
                .uri("/users")
                .retrieve()
                .bodyToFlux(User.class);
    }

    // GET with query params
    public Flux<User> searchUsers(String name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .bodyToFlux(User.class);
    }

    // POST
    public Mono<User> createUser(User user) {
        return webClient.post()
                .uri("/users")
                .bodyValue(user)
                .retrieve()
                .bodyToMono(User.class);
    }

    // PUT
    public Mono<Void> updateUser(Long id, User user) {
        return webClient.put()
                .uri("/users/{id}", id)
                .bodyValue(user)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // DELETE
    public Mono<Void> deleteUser(Long id) {
        return webClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
```

## Using in a WebFlux Controller
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserWebClient client;

    public UserController(UserWebClient client) { this.client = client; }

    @GetMapping("/{id}")
    public Mono<User> get(@PathVariable Long id) {
        return client.getUser(id);
    }

    @GetMapping
    public Flux<User> all() {
        return client.getAllUsers();
    }
}
```

## Using Synchronously (in a Spring MVC app)
```java
User user = webClient.get()
        .uri("/users/{id}", 1)
        .retrieve()
        .bodyToMono(User.class)
        .block();   // blocks current thread; avoid inside reactive code
```

## Error Handling, Retry, Timeout
```java
public Mono<User> getUserResilient(Long id) {
    return webClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError,
                    resp -> Mono.error(new UserNotFoundException("User not found: " + id)))
            .onStatus(HttpStatusCode::is5xxServerError,
                    resp -> Mono.error(new RuntimeException("Server error")))
            .bodyToMono(User.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
            .onErrorResume(ex -> Mono.just(new User(0L, "fallback", "n/a")));
}
```

## Parallel Calls (a key WebClient strength)
```java
public Mono<Dashboard> loadDashboard(Long userId) {
    Mono<User> user = getUser(userId);
    Mono<List<Order>> orders = orderClient.getOrders(userId).collectList();

    return Mono.zip(user, orders)                    // runs both concurrently
               .map(t -> new Dashboard(t.getT1(), t.getT2()));
}
```

## Filter (logging / auth header on every request)
```java
WebClient client = WebClient.builder()
        .filter((request, next) -> {
            ClientRequest filtered = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + token)
                    .build();
            return next.exchange(filtered);
        })
        .build();
```

## Streaming (SSE / large data)
```java
public Flux<String> streamEvents() {
    return webClient.get()
            .uri("/events")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String.class);
}
```

## Interview Q&A
**Q: What is WebClient?**
A non-blocking reactive HTTP client from Spring WebFlux built on Reactor. It returns `Mono`/`Flux` and doesn't hold a thread while waiting for the response.

**Q: `Mono` vs `Flux`?**
`Mono` = 0..1 element, `Flux` = 0..N elements.

**Q: Nothing happens when I call WebClient. Why?**
Reactive streams are lazy. Nothing executes until someone **subscribes** (or the framework does, e.g. by returning it from a WebFlux controller, or you call `block()`/`subscribe()`).

**Q: Can WebClient be used in Spring MVC?**
Yes. Use `.block()` for sync calls, or return `Mono`/`Flux` from controllers (Spring MVC supports reactive return types). But if the app is purely synchronous, RestClient is simpler.

**Q: `retrieve()` vs `exchangeToMono()`?**
`retrieve()` is simpler and handles error status via `onStatus`. `exchangeToMono()` gives full control of the `ClientResponse` (you must consume the body).

**Q: How do you make parallel calls?**
`Mono.zip(...)` or `Flux.merge(...)`. Both calls run concurrently on the event loop.

**Q: How do you handle failures?**
`onStatus`, `onErrorResume`, `onErrorReturn`, `retryWhen(Retry.backoff(...))`, `timeout(...)`.

**Q: Why avoid `block()` in reactive code?**
It blocks the event-loop thread, which kills throughput and can cause deadlocks. Reactor even throws an `IllegalStateException` for `block()` on non-blocking threads.
