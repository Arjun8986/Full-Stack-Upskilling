# OpenFeign (Spring Cloud OpenFeign)

## Quick Notes
- **Declarative** HTTP client: define an **interface + annotations**, Spring generates the implementation at runtime.
- Synchronous and blocking (reactive support is limited and not the recommended path).
- Integrates with **Eureka / service discovery**, **Spring Cloud LoadBalancer**, **Resilience4j** circuit breaker.
- Best for: **service-to-service** calls in microservices with minimal boilerplate.
- Uses Spring MVC annotations (`@GetMapping`, `@PathVariable`, ...).

## Dependency
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- Spring Cloud BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Enable Feign
```java
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

## Feign Client Interface
```java
// With a fixed URL
@FeignClient(name = "user-client", url = "https://jsonplaceholder.typicode.com")
public interface UserFeignClient {

    @GetMapping("/users/{id}")
    User getUser(@PathVariable("id") Long id);      // name in @PathVariable is required

    @GetMapping("/users")
    List<User> getAllUsers();

    @GetMapping("/users")
    List<User> searchUsers(@RequestParam("name") String name);

    @PostMapping("/users")
    User createUser(@RequestBody User user);

    @PutMapping("/users/{id}")
    User updateUser(@PathVariable("id") Long id, @RequestBody User user);

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

    @GetMapping("/secure/data")
    String secure(@RequestHeader("Authorization") String token);
}
```

## Service Discovery (Eureka) Style
```java
// No URL: "user-service" is resolved through Eureka + Spring Cloud LoadBalancer
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable("id") Long id);
}
```

## Usage (just inject it)
```java
@Service
public class OrderService {

    private final UserFeignClient userClient;

    public OrderService(UserFeignClient userClient) {
        this.userClient = userClient;
    }

    public OrderDetails getOrderDetails(Long orderId, Long userId) {
        User user = userClient.getUser(userId);       // looks like a local method call
        return new OrderDetails(orderId, user);
    }
}
```

## Configuration (application.yml)
```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                      # applies to all clients
            connectTimeout: 3000
            readTimeout: 5000
            loggerLevel: full
          user-client:                  # applies to one client only (by name)
            readTimeout: 2000

logging:
  level:
    com.example.client.UserFeignClient: DEBUG   # Feign logging needs DEBUG
```

## Custom Configuration Class
```java
public class FeignConfig {

    // Add Authorization header to every request
    @Bean
    public RequestInterceptor authInterceptor() {
        return template -> template.header("Authorization", "Bearer " + getToken());
    }

    // Convert HTTP errors into domain exceptions
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> switch (response.status()) {
            case 404 -> new UserNotFoundException("User not found");
            case 500 -> new RuntimeException("Remote server error");
            default  -> new ErrorDecoder.Default().decode(methodKey, response);
        };
    }

    // Retry policy: (period, maxPeriod, maxAttempts)
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

@FeignClient(name = "user-client", url = "...", configuration = FeignConfig.class)
public interface UserFeignClient { ... }
```

## Fallback with Circuit Breaker (Resilience4j)
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```
```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    User getUser(@PathVariable("id") Long id);
}

@Component
public class UserClientFallback implements UserServiceClient {
    @Override
    public User getUser(Long id) {
        return new User(id, "Unknown", "unavailable@example.com");   // default response
    }
}
```
> Use `fallbackFactory` instead of `fallback` if you need access to the exception that caused the failure.

## Interview Q&A
**Q: What is OpenFeign?**
A declarative HTTP client. You write an annotated interface and Spring Cloud generates a proxy that performs the REST call, so there is almost no boilerplate.

**Q: Why use Feign in microservices?**
Clean code, integrates with Eureka and load balancing, easy interceptors, error decoders, retries, and circuit breakers.

**Q: How does Feign do load balancing?**
With no `url` set, `name` is a logical service ID. Spring Cloud LoadBalancer picks an instance from the discovery registry (older versions used Ribbon).

**Q: Why do I get an error about `@PathVariable` in Feign?**
The name must be explicit: `@PathVariable("id")`. Feign can't rely on parameter names being available at runtime.

**Q: How do you handle errors?**
A custom `ErrorDecoder` maps HTTP status codes to exceptions. `fallback`/`fallbackFactory` with a circuit breaker provides degraded responses.

**Q: How do you propagate an auth token to downstream services?**
A `RequestInterceptor` that copies the header from the current request (or a `SecurityContext`) onto the Feign request template.

**Q: How do you set timeouts?**
`spring.cloud.openfeign.client.config.<name>.connectTimeout/readTimeout` in properties.

**Q: Does Feign support reactive?**
Not natively in Spring Cloud OpenFeign. For reactive apps prefer WebClient.

**Q: Is Feign in maintenance mode?**
The original Netflix Feign moved to OpenFeign, which is actively maintained. Spring Cloud OpenFeign is still the standard for declarative clients in Spring Cloud, though Spring's own HTTP Interface (`@HttpExchange`) is an alternative that needs no extra library.
