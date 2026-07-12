package com.cupk.config;

import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.auth.repository.AuthUserRepository.AuthUserRow;
import com.cupk.auth.service.AuthTokenService;
import com.cupk.auth.service.AuthTokenService.TokenClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 安全配置类，配置Spring Security的认证授权规则、CORS跨域、安全响应头和速率限制过滤器。
 */
@Configuration
public class SecurityConfig {

    private final AuthUserRepository authUserRepository;
    private final AuthTokenService authTokenService;
    private final RateLimitFilter rateLimitFilter;
    private final List<String> allowedOrigins;

    /**
     * 构造函数，注入依赖组件并解析允许的跨域来源。
     *
     * @param authUserRepository 用户数据访问层
     * @param authTokenService 认证令牌服务
     * @param rateLimitFilter 速率限制过滤器
     * @param allowedOrigins 允许的跨域来源列表，从配置文件读取
     */
    public SecurityConfig(
            AuthUserRepository authUserRepository,
            AuthTokenService authTokenService,
            RateLimitFilter rateLimitFilter,
            @Value("${app.security.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}") String allowedOrigins
    ) {
        this.authUserRepository = authUserRepository;
        this.authTokenService = authTokenService;
        this.rateLimitFilter = rateLimitFilter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    /**
     * 配置安全过滤链，定义认证授权规则、CORS跨域、安全响应头和过滤器顺序。
     *
     * @param http Spring Security的HTTP安全配置构建器
     * @return 配置完成的安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'self'; object-src 'none'; base-uri 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(policy -> policy.policy("geolocation=(), microphone=(), camera=(), payment=()"))
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new BearerTokenAuthenticationFilter(authUserRepository, authTokenService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/password-reset/code",
                                "/api/auth/password-reset/confirm"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/oj/categories").permitAll()
                        .requestMatchers("/api/admin/oj/problems/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/admin/oj/problems/check").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/onboarding", "/api/auth/pet").authenticated()
                        .requestMatchers("/api/profile/**", "/api/rewards/**", "/api/ai-pet/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/academy/home").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/online-open-courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/general-courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/micro-major-courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/textbooks/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/textbook-payments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/*/categories").permitAll()
                        .requestMatchers("/api/academy/teacher/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/academy/question-bank/subjects").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/question-bank/problems/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/question-bank/course-catalog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/question-bank/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academy/question-bank/type-warrior/words").permitAll()
                        .requestMatchers("/api/academy/question-bank/import/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/academy/question-bank/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/academy/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/academy/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/academy/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/academy/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/oj/problems/**").permitAll()
                        .requestMatchers("/api/oj/submissions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/oj/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/oj/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/oj/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/games/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/well-log/template/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .build();
    }

    /**
     * 创建密码编码器Bean，使用BCrypt算法加密用户密码。
     *
     * @return BCrypt密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建CORS配置源Bean，配置允许的跨域来源、请求方法和请求头。
     *
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Accept", "Authorization", "Content-Type", "X-Auth-User-Id"));
        configuration.setExposedHeaders(List.of());
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bearer令牌认证过滤器，从请求头中提取JWT令牌并验证用户身份。
     */
    private static final class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
        private final AuthUserRepository authUserRepository;
        private final AuthTokenService authTokenService;

        private BearerTokenAuthenticationFilter(AuthUserRepository authUserRepository, AuthTokenService authTokenService) {
            this.authUserRepository = authUserRepository;
            this.authTokenService = authTokenService;
        }

        /**
         * 执行令牌认证逻辑，验证Bearer令牌并设置安全上下文。
         */
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            AuthenticatedUser authenticatedUser = null;
            String token = resolveBearerToken(request);
            TokenClaims claims = authTokenService.verify(token);
            if (SecurityContextHolder.getContext().getAuthentication() == null && claims != null) {
                AuthUserRow user = authUserRepository.findById(claims.userId());
                if (user != null && claims.role().equals(authTokenService.normalizeRole(user.roleType()))) {
                    String role = claims.role();
                    authenticatedUser = new AuthenticatedUser(user.id(), user.email(), role);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(new AuthenticatedUserRequest(request, authenticatedUser), response);
        }

        /**
         * 从Authorization请求头中提取Bearer令牌。
         *
         * @param request HTTP请求对象
         * @return Bearer令牌字符串，不存在返回null
         */
        private String resolveBearerToken(HttpServletRequest request) {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                return null;
            }
            return header.substring(7).trim();
        }
    }

    /** 已认证用户记录，包含用户ID、邮箱和角色。 */
    private record AuthenticatedUser(Long id, String email, String role) {
    }

    /**
     * 已认证用户请求包装器，将认证后的用户ID注入到X-Auth-User-Id请求头中。
     */
    private static final class AuthenticatedUserRequest extends HttpServletRequestWrapper {
        private final AuthenticatedUser user;

        private AuthenticatedUserRequest(HttpServletRequest request, AuthenticatedUser user) {
            super(request);
            this.user = user;
        }

        @Override
        public String getHeader(String name) {
            if ("X-Auth-User-Id".equalsIgnoreCase(name)) {
                return user == null ? null : String.valueOf(user.id());
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if ("X-Auth-User-Id".equalsIgnoreCase(name)) {
                return user == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(String.valueOf(user.id())));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> existing = super.getHeaderNames();
            while (existing != null && existing.hasMoreElements()) {
                String name = existing.nextElement();
                if (!"X-Auth-User-Id".equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            if (user != null) {
                names.add("X-Auth-User-Id");
            }
            return Collections.enumeration(names);
        }
    }
}
