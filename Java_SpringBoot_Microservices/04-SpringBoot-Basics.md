# Spring Boot: What It Is and Why We Use It

## Quick Notes
- Spring Boot is a framework built **on top of Spring Framework** for creating **production-ready, stand-alone** applications with minimal setup.
- Spring is the toolbox (DI, MVC, data access, security). Spring Boot is the pre-assembled workshop: sensible defaults, auto-wiring, run immediately.
- It does **not replace** Spring. It uses Spring underneath.

## Why Use It
1. **Auto-configuration**: inspects the classpath and configures beans for you (e.g. `DataSource`, `EntityManager`, transaction manager when JPA + a DB driver are present). You override only what you need.
2. **Starter dependencies**: one dependency pulls in a compatible set of libraries.
3. **Embedded server**: Tomcat/Jetty/Undertow packaged in the JAR. Run with `java -jar app.jar`, no external server or WAR deployment.
4. **No XML**: annotations, Java config, and `application.properties` / `application.yml`.
5. **Actuator**: health, metrics, and info endpoints out of the box (`/actuator/health`), great for Kubernetes and monitoring.
6. **Managed dependency versions** via the Spring Boot BOM, so fewer version conflicts.
7. **Great for microservices and cloud**: small, self-contained, fast-starting, easy to containerize, integrates with Spring Cloud.
8. **Faster development**: Spring Initializr, DevTools hot reload, test slices (`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`).

## Starter Dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>  <!-- Spring MVC + Jackson + embedded Tomcat -->
</dependency>
```
Common starters: `starter-web`, `starter-data-jpa`, `starter-security`, `starter-test`, `starter-actuator`, `starter-validation`.

## Minimal Example
```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```
Run it, then open `http://localhost:8080/hello`.

## What `@SpringBootApplication` Is
| Annotation | Purpose |
|---|---|
| `@Configuration` | Marks the class as a source of bean definitions |
| `@EnableAutoConfiguration` | Turns on auto-configuration |
| `@ComponentScan` | Scans the package and sub-packages for `@Component`, `@Service`, `@Repository`, `@RestController` |

## Spring vs Spring Boot
| | Spring Framework | Spring Boot |
|---|---|---|
| Configuration | Manual (XML or Java config) | Auto-configured |
| Server | Deploy WAR to external server | Embedded server, run JAR |
| Dependencies | Choose and align versions yourself | Starters + managed versions |
| Setup time | Longer | Minutes |
| Production metrics | Set up yourself | Actuator included |

## Configuration Example
```yaml
# application.yml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: secret
  jpa:
    hibernate:
      ddl-auto: update
```

## Profiles (Environments)
```yaml
# application-dev.yml, application-prod.yml
# activate with:  spring.profiles.active=prod
```

## Interview Q&A
**Q: What is Spring Boot?**
An opinionated layer on top of Spring that provides auto-configuration, starters, and an embedded server so you can build and run production-ready apps quickly with minimal configuration.

**Q: How does auto-configuration work?**
`@EnableAutoConfiguration` loads candidate configuration classes listed in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Each is guarded by conditions like `@ConditionalOnClass` and `@ConditionalOnMissingBean`, so it applies only when the library is present and you haven't defined your own bean.

**Q: How do you disable a specific auto-configuration?**
`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)` or `spring.autoconfigure.exclude` in properties.

**Q: How do you handle different environments?**
Profiles: `application-dev.yml`, `application-prod.yml`, activated with `spring.profiles.active=prod`.

**Q: What is Actuator?**
A module exposing operational endpoints (health, metrics, info) for monitoring and management.

**Q: `@Component` vs `@Service` vs `@Repository` vs `@Controller`?**
All are stereotype annotations that register beans. `@Service` marks business logic, `@Repository` marks data access (and adds exception translation), and `@Controller`/`@RestController` marks web-layer classes.

**Q: What are starters?**
Curated dependency descriptors that bring in a compatible set of libraries for a feature, so you don't pick individual versions.

**Q: How do you run a Spring Boot app?**
`mvn spring-boot:run`, or build the fat JAR and run `java -jar app.jar`.

## Short Interview Answer
"Spring Boot is an opinionated layer on top of Spring that gives auto-configuration, starter dependencies, and an embedded server, so we can build and run production-ready applications quickly with minimal configuration. It's especially well suited to microservices."
