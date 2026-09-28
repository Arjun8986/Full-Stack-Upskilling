# RestClient (Spring Framework 6.1+ / Spring Boot 3.2+)

## Quick Notes
- **Synchronous, blocking** HTTP client with a **fluent API** (like WebClient's style, but blocking).
- Introduced in Spring 6.1 as the modern replacement for `RestTemplate` (which is in maintenance mode).
- No reactive dependency needed: `spring-boot-starter-web` is enough.
- Works on top of pluggable HTTP libraries (JDK `HttpClient`, Apache HttpClient, Jetty, `SimpleClientHttpRequestFactory`).
- Best for: simple, straightforward blocking calls in Spring MVC apps.

## Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

## Model
```java
public record User(Long id, String name, String email) {}
```

## Configuration (Bean)
```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}
```

## CRUD Examples
```java
@Service
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // GET single object
    public User getUser(Long id) {
        return restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(User.class);
    }

    // GET list (generics -> ParameterizedTypeReference)
    public List<User> getAllUsers() {
        return restClient.get()
                .uri("/users")
                .retrieve()
                .body(new ParameterizedTypeReference<List<User>>() {});
    }

    // GET with query params
    public List<User> searchUsers(String name) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    // POST
    public User createUser(User user) {
        return restClient.post()
                .uri("/users")
                .body(user)
                .retrieve()
                .body(User.class);
    }

    // PUT
    public void updateUser(Long id, User user) {
        restClient.put()
                .uri("/users/{id}", id)
                .body(user)
                .retrieve()
                .toBodilessEntity();
    }

    // DELETE
    public void deleteUser(Long id) {
        restClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    // Get full response (status + headers + body)
    public ResponseEntity<User> getUserEntity(Long id) {
        return restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .toEntity(User.class);
    }
}
```

## Error Handling
```java
public User getUserSafe(Long id) {
    return restClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                throw new UserNotFoundException("User not found: " + id);
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new RuntimeException("Remote service failed: " + response.getStatusCode());
            })
            .body(User.class);
}
```
> By default, `retrieve()` throws `HttpClientErrorException` (4xx) / `HttpServerErrorException` (5xx).

## Adding Headers / Auth
```java
restClient.get()
        .uri("/secure/data")
        .header("Authorization", "Bearer " + token)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(String.class);
```

## Declarative Style with HTTP Interface (bonus)
```java
public interface UserApi {
    @GetExchange("/users/{id}")
    User getUser(@PathVariable Long id);

    @PostExchange("/users")
    User create(@RequestBody User user);
}

@Bean
UserApi userApi(RestClient restClient) {
    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter).build().createClient(UserApi.class);
}
```

## Interview Q&A
**Q: What is RestClient and why was it introduced?**
A synchronous fluent HTTP client added in Spring 6.1. It gives a modern, readable API (like WebClient) without needing reactive dependencies, and replaces the aging `RestTemplate`.

**Q: RestClient vs RestTemplate?**
Both are blocking. RestClient has a fluent API, better error handling via `onStatus`, and shares the message converters and infrastructure with WebClient. RestTemplate is in maintenance mode.

**Q: How do you handle errors?**
Use `.retrieve().onStatus(...)` for custom exceptions, or `.exchange()` for full control of the response.

**Q: `retrieve()` vs `exchange()`?**
`retrieve()` is the simple path: it auto-throws on 4xx/5xx and gives the body. `exchange()` gives full access to the request/response, but you must handle status codes and close resources yourself.

**Q: How do you set timeouts?**
On the underlying `ClientHttpRequestFactory` (connect and read timeouts). RestClient itself has no timeout setting.

**Q: When would you NOT use it?**
In reactive/WebFlux apps (it blocks the thread), or when you need streaming and non-blocking behavior.
