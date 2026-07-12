package com.cupk.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API速率限制过滤器，对登录、注册、密码重置和文件上传等敏感接口进行请求频率控制。
 * 基于客户端IP和请求路径进行限流，使用滑动窗口算法统计请求数量。
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 执行速率限制检查，超出限制时返回429状态码。
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        LimitRule rule = ruleFor(request);
        if (rule != null && !allow(request, rule)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 判断当前请求是否被允许通过，基于滑动窗口统计请求数量。
     *
     * @param request HTTP请求对象
     * @param rule 速率限制规则
     * @return 允许通过返回true，超出限制返回false
     */
    private boolean allow(HttpServletRequest request, LimitRule rule) {
        String key = rule.name() + ":" + clientIp(request);
        long now = Instant.now().getEpochSecond();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.resetAt()) {
                return new Bucket(1, now + rule.windowSeconds());
            }
            return new Bucket(existing.count() + 1, existing.resetAt());
        });
        return bucket.count() <= rule.maxRequests();
    }

    /**
     * 根据请求路径匹配对应的速率限制规则。
     *
     * @param request HTTP请求对象
     * @return 匹配的速率限制规则，无匹配返回null
     */
    private LimitRule ruleFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && path.matches("/api/auth/(login|register)$")) {
            return new LimitRule("auth", 10, 60);
        }
        if ("POST".equals(method) && path.startsWith("/api/auth/password-reset/")) {
            return new LimitRule("password-reset", 5, 300);
        }
        if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))
                && path.matches("/api/(profile/avatar|academy/online-open-courses.*)")) {
            return new LimitRule("upload", 20, 3600);
        }
        return null;
    }

    /**
     * 获取客户端真实IP地址，优先从X-Forwarded-For请求头获取。
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** 速率限制规则，包含规则名称、最大请求数和时间窗口（秒）。 */
    private record LimitRule(String name, int maxRequests, int windowSeconds) {
    }

    /** 请求计数桶，记录当前窗口内的请求数量和重置时间。 */
    private record Bucket(int count, long resetAt) {
    }
}
