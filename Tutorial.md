# Securing REST APIs with JWT in Java Spring Boot

## 1. Introduction

In modern microservices, securing endpoints is essential to prevent unauthorized access and protect sensitive data.  

This guide demonstrates how **JWT (JSON Web Token)** is used in our project to secure REST APIs across `web-server` and `user-service`.

**Example JWT token:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzYWJpbmEiLCJpYXQiOjE2NzM2NzI3MjMsImV4cCI6MTY3MzY3NjMyM30.d0U8R5pIYZT3hJ3zL3fZqA4H2X3-6OQwVcXzM7bY4pA
```

---


## 2. Authentication Flow (Login)

### 1. Register a user

**Endpoint:**  
```
 POST /auth/register
```
**Request body:**
```json
{
  "username": "sabina",
  "password": "123456",
  "email": "sabina@example.com",
  "name": "Sabina Radu"
}
```

### 2. Log in to get JWT

**Endpoint:**  
```
 POST /auth/login
```
**Request body:**
```json
{
  "username": "sabina",
  "password": "123456"
}
```
**Response:**
```json
{
  "token": "<JWT token>"
}
```

### 3. Access protected endpoint
**Endpoint:**  
```
 GET /api/users
```
**Headers:**
```
Authorization: Bearer <JWT token>
```

Only requests with a valid JWT token will succeed.

## 3. Generating JWT in User Service

The `user-service` is responsible for generating JWT tokens after successful login.

### Token Configuration
- **Subject**: username
- **Issued At**: current time
- **Expiration**: 24 hours
- **Signature Algorithm**: HMAC-SHA with secret key

### JwtService
```java
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String SECRET;

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
```

## 4. Validating JWT in Web Server
The web-server validates JWT tokens before allowing access to protected endpoints.

**Key Points**
- Protects all endpoints except `/auth/**`.
- Allows public access to authentication endpoints
- Requires JWT token for all other endpoints
- Returns 403 Forbidden for invalid or missing tokens

**JwtAuthenticationFilter**
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
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // allow /auth/** endpoints without JWT
        if (path.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```
The `JwtAuthenticationFilter` runs inside the microservice, intercepts each request, checks the JWT token, and ensures only requests with a valid token can access protected endpoints.

## 5. JwtGatewayFilter (API Gateway)
The JwtGatewayFilter is a Global Filter used in Spring Cloud Gateway to intercept requests before they reach the microservices.

**Responsibilities:**
- Intercepts all incoming requests at the gateway level, before routing to microservices.
- Optionally validates JWT tokens to quickly reject unauthorized requests.
- Reduces unnecessary traffic: requests with missing or invalid tokens can be blocked without hitting the microservice.
- Acts as a first line of defense, but does not replace authentication inside microservices. Each microservice should still validate JWTs independently.

The gateway filter is mainly for efficiency and early blocking. The real security and authentication always happens inside each microservice using the `JwtAuthenticationFilter`.

## 6. Securing Endpoints in Spring Security
The security configuration integrates the JWT filter.

**Key Points**
- Stateless authentication (no server session)
- `/auth/**` endpoints are public
- All other endpoints require a valid JWT token
- JWT filter runs before the authentication filter

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

## 7. JwtService (Web Server)
This service is responsible for JWT parsing and validation.

**Features**
- Extract username from token
- Validate token expiration
- Validate token signature
```java
public boolean isTokenValid(String token) {
    try {
        Claims claims = extractAllClaims(token);
        return !isTokenExpired(claims);
    } catch (Exception e) {
        return false;
    }
}
```

## 8. Conclusion
This setup ensures secure, scalable, and maintainable REST APIs in a microservices architecture.

**Key Points**
- JWT enables stateless authentication
- Microservices can independently validate tokens
- Only authenticated requests access protected resources
- API Gateway can route requests without handling sessions