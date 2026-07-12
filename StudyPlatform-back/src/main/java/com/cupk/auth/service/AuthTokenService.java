package com.cupk.auth.service;

import com.cupk.auth.repository.AuthUserRepository.AuthUserRow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 认证令牌服务，负责令牌的签发、校验与角色标准化处理。
 */
@Service
public class AuthTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long ttlSeconds;

    /**
     * 构造认证令牌服务。
     *
     * @param secret      令牌签名密钥，长度不足32位时将随机生成
     * @param ttlSeconds  令牌有效期（秒），最小为300秒
     */
    public AuthTokenService(
            @Value("${app.security.token-secret:}") String secret,
            @Value("${app.security.token-ttl-seconds:86400}") long ttlSeconds
    ) {
        this.secret = resolveSecret(secret);
        this.ttlSeconds = Math.max(300L, ttlSeconds);
    }

    /**
     * 为指定用户签发认证令牌。
     *
     * @param user  用户信息
     * @return 签名后的认证令牌字符串
     */
    public String issue(AuthUserRow user) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = "%d:%s:%d".formatted(user.id(), normalizeRole(user.roleType()), expiresAt);
        String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    /**
     * 校验令牌并解析其中的声明信息。
     *
     * @param token  待校验的令牌字符串
     * @return 解析后的令牌声明；若令牌无效或已过期则返回 null
     */
    public TokenClaims verify(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        String expectedSignature = sign(parts[0]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String payload;
        try {
            payload = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String[] values = payload.split(":", -1);
        if (values.length != 3) {
            return null;
        }
        try {
            long userId = Long.parseLong(values[0]);
            long expiresAt = Long.parseLong(values[2]);
            if (userId <= 0 || expiresAt < Instant.now().getEpochSecond()) {
                return null;
            }
            return new TokenClaims(userId, normalizeRole(values[1]), expiresAt);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 将角色类型标准化为 ADMIN、TEACHER 或 STUDENT。
     *
     * @param roleType  原始角色类型字符串
     * @return 标准化后的角色字符串
     */
    public String normalizeRole(String roleType) {
        String role = roleType == null ? "" : roleType.trim().toUpperCase();
        return switch (role) {
            case "ADMIN" -> "ADMIN";
            case "TEACHER" -> "TEACHER";
            default -> "STUDENT";
        };
    }

    /**
     * 使用 HMAC-SHA256 对载荷进行签名。
     *
     * @param encodedPayload  Base64 编码后的载荷
     * @return Base64 编码后的签名
     */
    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign authentication token", ex);
        }
    }

    /**
     * 解析配置的密钥，若不合法则随机生成一个32字节密钥。
     *
     * @param configuredSecret  配置的密钥字符串
     * @return 用于签名的密钥字节数组
     */
    private byte[] resolveSecret(String configuredSecret) {
        if (configuredSecret != null && configuredSecret.length() >= 32) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        byte[] generatedSecret = new byte[32];
        new SecureRandom().nextBytes(generatedSecret);
        return generatedSecret;
    }

    /**
     * 令牌声明信息，包含用户ID、角色和过期时间。
     *
     * @param userId     用户ID
     * @param role       标准化后的角色
     * @param expiresAt  过期时间（Unix 时间戳，秒）
     */
    public record TokenClaims(long userId, String role, long expiresAt) {
    }
}
