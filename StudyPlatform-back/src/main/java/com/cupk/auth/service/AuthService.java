package com.cupk.auth.service;

import com.cupk.auth.dto.AuthLoginRequest;
import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthRegisterRequest;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.auth.repository.AuthUserRepository.AuthUserRow;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles account registration, credential checks, and onboarding choices.
 */
@Service
public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\u4e00-\\u9fa5-]{2,64}$");

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    public AuthUserResponse login(AuthLoginRequest request) {
        String email = clean(request.account()).toLowerCase();
        AuthUserRow user = authUserRepository.findByEmail(email);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
        }
        return user.toResponse();
    }

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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

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

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
