package com.cupk.academy.controller;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileOverviewResponse;
import com.cupk.academy.dto.ProfileUserResponse;
import com.cupk.academy.dto.ProfileUserUpdateRequest;
import com.cupk.academy.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/overview")
    public ProfileOverviewResponse getOverview() {
        return profileService.getOverview();
    }

    @GetMapping("/user")
    public ProfileUserResponse getUserProfile() {
        return profileService.getUserProfile();
    }

    @PatchMapping("/user")
    public ProfileUserResponse updateUserProfile(@RequestBody ProfileUserUpdateRequest request) {
        return profileService.updateUserProfile(request);
    }

    @PostMapping("/avatar")
    public ProfileUserResponse updateAvatar(@RequestParam("file") MultipartFile file) {
        return profileService.updateAvatar(file);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordLearningEvent(@RequestBody ProfileLearningEventRequest request) {
        profileService.recordLearningEvent(request);
    }
}
