package com.securebank.controller;

import com.securebank.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserProfileController {

    private final CurrentUserService currentUserService;

    public UserProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/me")
    public String getCurrentUserEmail() {
        return currentUserService.getCurrentUserEmail();
    }
}
