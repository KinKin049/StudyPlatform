package com.cupk.academy.controller;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileLearningTimeRecordRequest;
import com.cupk.academy.dto.ProfileOverviewResponse;
import com.cupk.academy.dto.ProfileUserResponse;
import com.cupk.academy.dto.ProfileUserUpdateRequest;
import com.cupk.academy.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户个人资料控制器
 * 提供用户概览、个人信息、头像、学习事件和学习时间记录等相关接口
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final long DEFAULT_USER_ID = 1L;
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * 获取用户学习概览
     * @param userId 用户ID，从请求头获取，可选
     * @return 用户学习概览响应
     */
    @GetMapping("/overview")
    public ProfileOverviewResponse getOverview(@RequestHeader(value = "X-Auth-User-Id", required = false) Long userId) {
        return profileService.getOverview(resolveUserId(userId));
    }

    /**
     * 获取用户个人信息
     * @param userId 用户ID，从请求头获取，可选
     * @return 用户个人信息响应
     */
    @GetMapping("/user")
    public ProfileUserResponse getUserProfile(@RequestHeader(value = "X-Auth-User-Id", required = false) Long userId) {
        return profileService.getUserProfile(resolveUserId(userId));
    }

    /**
     * 更新用户个人信息
     * @param userId 用户ID，从请求头获取，可选
     * @param request 用户信息更新请求
     * @return 更新后的用户个人信息响应
     */
    @PatchMapping("/user")
    public ProfileUserResponse updateUserProfile(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody ProfileUserUpdateRequest request
    ) {
        return profileService.updateUserProfile(resolveUserId(userId), request);
    }

    /**
     * 更新用户头像
     * @param userId 用户ID，从请求头获取，可选
     * @param file 头像文件
     * @return 更新后的用户个人信息响应
     */
    @PostMapping("/avatar")
    public ProfileUserResponse updateAvatar(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        return profileService.updateAvatar(resolveUserId(userId), file);
    }

    /**
     * 记录学习事件
     * @param userId 用户ID，从请求头获取，可选
     * @param request 学习事件记录请求
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLearningEvent(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody ProfileLearningEventRequest request
    ) {
        profileService.recordLearningEvent(resolveUserId(userId), request);
    }

    /**
     * 记录学习时长
     * @param userId 用户ID，从请求头获取，可选
     * @param request 学习时长记录请求
     */
    @PostMapping("/learning-time")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLearningTime(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody ProfileLearningTimeRecordRequest request
    ) {
        profileService.recordLearningTime(resolveUserId(userId), request);
    }

    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
