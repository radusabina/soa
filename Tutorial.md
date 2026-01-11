# Securing a REST API with JWT in Java Spring Boot

## 1. Introduction

In modern web applications, exposing REST APIs is common. However, it is crucial to secure these endpoints to prevent unauthorized access and protect sensitive data.  

In this tutorial, we will show how to secure a REST API using **JWT (JSON Web Tokens)** in a microservices environment, using the `user-service` from our example project.

**Topics covered:**

- What JWT is and why it is used  
- Implementing JWT authentication in a microservice  
- Securing endpoints in a Spring Boot service  
- Testing secured endpoints through an API gateway  

**Repository:** [user-service on GitHub](https://github.com/radusabina/soa/tree/main/user-service)

---

## 2. JWT Overview

**JSON Web Tokens (JWT)** are a compact, URL-safe means of representing claims between two parties.  

**Key points:**

- JWT is composed of **header**, **payload**, and **signature**  
- It allows **stateless authentication** – no server-side session is needed  
- Typically, a token is issued after a user logs in and must be included in the `Authorization` header for protected endpoints

**Example token:**

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzYWJpbmEiLCJpYXQiOjE2NzM2NzI3MjMsImV4cCI6MTY3MzY3NjMyM30.d0U8R5pIYZT3hJ3zL3fZqA4H2X3-6OQwVcXzM7bY4pA
```

---

## 3. Implementing JWT in a Spring Boot Microservice

### Step 1: Add dependencies

In `build.gradle`:

```xml
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
```
### Step 2: Create JwtService

The `JwtService` class is responsible for generating and validating JWT tokens.

**Responsibilities:**

- Generate a JWT token for a given username after a successful login  
- Extract the username from an existing token to verify identity  
- Set token expiration (e.g., 1 hour)  

This service uses a secret key and the HS256 algorithm to sign tokens.  
It ensures that tokens are tamper-proof and can be safely sent to clients.

```java
@Service
public class JwtService {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expiration = 1000 * 60 * 60; // 1 hour

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}

```

### Step 3: Create JwtAuthenticationFilter

The `JwtAuthenticationFilter` is a Spring Security filter that intercepts incoming HTTP requests and validates JWT tokens.

**Responsibilities:**

- Check if the request contains an `Authorization` header with a Bearer token  
- Extract the JWT token from the header  
- Validate the token using `JwtService`  
- If the token is valid, set the authentication in the Spring Security `SecurityContext`  
- Allow the request to continue to the controller if authentication succeeds  

This filter runs **once per request** and ensures that only requests with a valid JWT can access protected endpoints.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
```

### Step 4: Configure Security

The `SecurityConfig` class configures Spring Security for the microservice and integrates the `JwtAuthenticationFilter`.

**Responsibilities:**

- Disable CSRF (Cross-Site Request Forgery) protection for simplicity, as the API is stateless  
- Allow public access to `/auth/**` endpoints (register and login)  
- Require authentication for all other endpoints  
- Configure the session management to be stateless, so the server does not store any session data  
- Add the `JwtAuthenticationFilter` before Spring Security's `UsernamePasswordAuthenticationFilter`  
  to validate JWT tokens for each request  

This configuration ensures that only requests with a valid JWT token can access protected endpoints, while allowing unauthenticated users to register or login.

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```
## 4. Using the Secured API

Once the JWT authentication is implemented, the API exposes the following endpoints:

### **Public Endpoints**

- **`POST /auth/register`** → Create a new user  
  - No authentication required  
  - Example request body:
    ```json
    {
      "username": "sabina",
      "password": "123456"
    }
    ```

- **`POST /auth/login`** → Login and get a JWT token  
  - No authentication required  
  - Example request body:
    ```json
    {
      "username": "sabina",
      "password": "123456"
    }
    ```
  - Response example:
    ```json
    {
      "token": "<JWT token>"
    }
    ```

### **Protected Endpoint**

- **`GET /api/users`** → Retrieve all users  
  - Requires a valid JWT token in the `Authorization` header

**Header for protected requests:**

```
Authorization: Bearer <your-token>
```


### **Example Flow Using Postman**

1. Register a new user: `POST /auth/register`  
2. Login to get JWT: `POST /auth/login`  
3. Use the returned JWT to access protected endpoint:

```
GET /api/users
(Header) Authorization: Bearer <JWT token>
```

## 5. Securing Through API Gateway

In a microservices architecture, an **API Gateway** is used to route requests from clients to the appropriate microservices.  

**Key points:**

- The API Gateway forwards incoming requests to the microservices based on the request path.  
- **JWT validation occurs at the microservice** (e.g., `user-service`). The gateway itself does not need to validate tokens.  
- The gateway can remain **stateless**, which simplifies scaling and reduces overhead.  
- This setup is **scalable**: new microservices can be added without modifying authentication logic, as each microservice independently validates JWT tokens.

**Example:**
```
Client → API Gateway → User Service (JWT validated) → Response
```


---

## 6. Conclusion

Securing REST APIs with JWT in a microservices architecture provides several benefits:

- **Stateless authentication**: no server-side session storage needed  
- **Scalability**: microservices can be added or removed without changing the authentication flow  
- **Security**: only requests with a valid JWT can access protected endpoints  
- **Simplicity**: the API Gateway only routes requests; authentication is handled by each service  

Using this setup, microservices are **secure, scalable, and maintainable**, ready for real-world applications.
