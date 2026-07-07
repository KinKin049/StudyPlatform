package com.cupk.auth.controller;

import com.cupk.auth.dto.AuthLoginRequest;
import com.cupk.auth.dto.AuthMessageResponse;
import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthRegisterRequest;
import com.cupk.auth.dto.AuthUserResponse;
import com.cupk.auth.dto.PasswordResetCodeRequest;
import com.cupk.auth.dto.PasswordResetConfirmRequest;
import com.cupk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器，提供用户注册、登录、密码重置和入职引导等账户管理端点。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册接口。
     *
     * @param request 注册请求，包含用户名、邮箱、密码等信息
     * @return 用户响应，包含注册成功后的用户信息
     */
    @PostMapping("/register")
    public AuthUserResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    /**
     * 用户登录接口。
     *
     * @param request 登录请求，包含账号和密码
     * @return 用户响应，包含登录成功后的用户信息
     */
    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    /**
     * 发送密码重置验证码接口。
     *
     * @param request 验证码请求，包含邮箱地址
     * @return 消息响应，提示验证码发送状态
     */
    @PostMapping("/password-reset/code")
    public AuthMessageResponse sendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request) {
        authService.sendPasswordResetCode(request);
        return new AuthMessageResponse("如果邮箱存在，验证码已发送");
    }

    /**
     * 确认密码重置接口。
     *
     * @param request 密码重置请求，包含邮箱、验证码和新密码
     * @return 消息响应，提示密码重置成功
     */
    @PostMapping("/password-reset/confirm")
    public AuthMessageResponse resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request);
        return new AuthMessageResponse("密码已重置，请返回登录");
    }

    /**
     * 保存入职引导信息接口。
     *
     * @param request 入职请求，包含角色类型、学习目标、兴趣等信息
     * @return 用户响应，包含更新后的用户信息
     */
    @PostMapping("/onboarding")
    public AuthUserResponse saveOnboarding(@RequestBody AuthOnboardingRequest request) {
        return authService.saveOnboarding(request);
    }
}
