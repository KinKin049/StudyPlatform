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

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final long DEFAULT_USER_ID = 1L;
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/overview")
    public ProfileOverviewResponse getOverview(@RequestHeader(value = "X-Auth-User-Id", required = false) Long userId) {
        return profileService.getOverview(resolveUserId(userId));
    }

    @GetMapping("/user")
    public ProfileUserResponse getUserProfile(@RequestHeader(value = "X-Auth-User-Id", required = false) Long userId) {
        return profileService.getUserProfile(resolveUserId(userId));
    }

    @PatchMapping("/user")
    public ProfileUserResponse updateUserProfile(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody ProfileUserUpdateRequest request
    ) {
        return profileService.updateUserProfile(resolveUserId(userId), request);
    }

    @PostMapping("/avatar")
    public ProfileUserResponse updateAvatar(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        return profileService.updateAvatar(resolveUserId(userId), file);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLearningEvent(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody ProfileLearningEventRequest request
    ) {
        profileService.recordLearningEvent(resolveUserId(userId), request);
    }

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
