package com.cupk.auth.service;

import com.cupk.auth.dto.AuthLoginRequest;
import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthRegisterRequest;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.dto.PasswordResetCodeRequest;
import com.cupk.auth.dto.PasswordResetConfirmRequest;
import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.auth.repository.AuthUserRepository.AuthUserRow;
import com.cupk.auth.repository.PasswordResetCodeRepository;
import com.cupk.auth.repository.PasswordResetCodeRepository.PasswordResetCodeRow;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 认证服务，处理用户注册、登录验证、入职引导和密码恢复等业务逻辑。
 */
@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\u4e00-\\u9fa5-]{2,64}$");
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESET_CODE_SEND_INTERVAL = Duration.ofSeconds(60);
    private static final int RESET_CODE_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthUserRepository authUserRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final String mailUsername;

    /**
     * 构造函数，注入依赖组件。
     *
     * @param authUserRepository 用户仓库，用于用户数据访问
     * @param passwordResetCodeRepository 密码重置码仓库，用于验证码管理
     * @param passwordEncoder 密码编码器，用于密码加密
     * @param mailSender 邮件发送器，用于发送验证码邮件
     * @param mailUsername 邮件发送账号
     */
    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordResetCodeRepository passwordResetCodeRepository,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailUsername = mailUsername == null ? "" : mailUsername.trim();
    }

    /**
     * 用户注册。验证用户名、邮箱和密码，创建新用户并同步用户资料。
     *
     * @param request 注册请求
     * @return 用户响应
     */
    public AuthUserResponse register(AuthRegisterRequest request) {
        String username = clean(request.username());
        String email = clean(request.email()).toLowerCase();
        String password = request.password() == null ? "" : request.password();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名只能包含中文、字母、数字、下划线或短横线，长度至少 2 位");
        }
        if (!password.equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次输入的密码不一致");
        }
        if (authUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被注册");
        }

        long userId = authUserRepository.insertUser(username, email, passwordEncoder.encode(password));
        return authUserRepository.findResponseById(userId);
    }

    /**
     * 用户登录。验证邮箱和密码，返回用户信息。
     *
     * @param request 登录请求
     * @return 用户响应
     */
    public AuthUserResponse login(AuthLoginRequest request) {
        String email = clean(request.account()).toLowerCase();
        AuthUserRow user = authUserRepository.findByEmail(email);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
        }
        return user.toResponse();
    }

    /**
     * 发送密码重置验证码。验证邮箱存在性和发送频率限制，生成验证码并发送邮件。
     *
     * @param request 验证码请求，包含邮箱地址
     */
    public void sendPasswordResetCode(PasswordResetCodeRequest request) {
        String email = clean(request.email()).toLowerCase();
        AuthUserRow user = authUserRepository.findByEmail(email);
        if (user == null) {
            return;
        }
        if (mailUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "邮箱发送服务未配置");
        }

        LocalDateTime now = LocalDateTime.now();
        PasswordResetCodeRow latest = passwordResetCodeRepository.findLatest(email);
        if (latest != null && latest.createdAt().plus(RESET_CODE_SEND_INTERVAL).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
        }

        String code = generateResetCode();
        sendResetCodeEmail(email, code);
        passwordResetCodeRepository.markActiveCodesUsed(email);
        passwordResetCodeRepository.insert(email, passwordEncoder.encode(code), now.plus(RESET_CODE_TTL));
    }

    /**
     * 重置密码。验证验证码有效性和新密码一致性，更新密码并标记验证码已使用。
     *
     * @param request 密码重置请求，包含邮箱、验证码和新密码
     */
    public void resetPassword(PasswordResetConfirmRequest request) {
        String email = clean(request.email()).toLowerCase();
        String code = clean(request.code());
        String password = request.password() == null ? "" : request.password();

        if (!password.equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }

        AuthUserRow user = authUserRepository.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }

        PasswordResetCodeRow resetCode = passwordResetCodeRepository.findLatestUsable(email, LocalDateTime.now());
        if (resetCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        if (resetCode.attemptCount() >= RESET_CODE_MAX_ATTEMPTS) {
            passwordResetCodeRepository.markUsed(resetCode.id());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误次数过多，请重新获取");
        }
        if (!passwordEncoder.matches(code, resetCode.codeHash())) {
            passwordResetCodeRepository.incrementAttempt(resetCode.id());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误");
        }

        authUserRepository.updatePassword(user.id(), passwordEncoder.encode(password));
        passwordResetCodeRepository.markUsed(resetCode.id());
    }

    /**
     * 保存入职引导信息。验证角色类型和必填字段，更新用户入职状态并同步用户资料。
     *
     * @param request 入职请求
     * @return 用户响应
     */
    public AuthUserResponse saveOnboarding(AuthOnboardingRequest request) {
        if (request == null || request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少用户信息");
        }
        String roleType = clean(request.roleType());
        if (!"student".equals(roleType) && !"teacher".equals(roleType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学生或教师身份");
        }
        if ("student".equals(roleType) && clean(request.learningGoal()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写学习目标");
        }
        if ("teacher".equals(roleType) && (clean(request.school()).isBlank() || clean(request.teacherName()).isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写所属学校和教师姓名");
        }
        if (clean(request.petKey()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择宠物");
        }

        authUserRepository.updateOnboarding(request, toJsonArray(request.interests()));
        return authUserRepository.findResponseById(request.userId());
    }

    /**
     * 生成6位数字的密码重置验证码。
     *
     * @return 6位数字验证码
     */
    private String generateResetCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    /**
     * 发送密码重置验证码邮件。
     *
     * @param email 收件人邮箱地址
     * @param code 验证码
     */
    private void sendResetCodeEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject("StudyPlatform 找回密码验证码");
        message.setText("""
                你正在找回 StudyPlatform 账号密码。

                验证码：%s

                验证码 10 分钟内有效。若不是你本人操作，请忽略这封邮件。
                """.formatted(code));
        try {
            mailSender.send(message);
        } catch (MailException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "验证码发送失败，请检查邮箱服务配置");
        }
    }

    /**
     * 清理字符串，去除首尾空格。
     *
     * @param value 待清理的字符串
     * @return 清理后的字符串，空值返回空字符串
     */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 将字符串列表转换为JSON数组格式。
     *
     * @param values 字符串列表
     * @return JSON数组字符串
     */
    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> "\"" + escapeJson(value.trim()) + "\"")
                .reduce("[", (left, right) -> "[".equals(left) ? left + right : left + "," + right)
                + "]";
    }

    /**
     * 转义JSON字符串中的特殊字符。
     *
     * @param value 待转义的字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
