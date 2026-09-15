package com.example.jira.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.example.jira.dto.ChangePasswordRequest;
import com.example.jira.dto.UpdateProfileRequest;
import com.example.jira.model.User;
import com.example.jira.repository.UserRepository;
import com.example.jira.service.UserProfileService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class Usercontroller {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("/signup")
    public User signup(@RequestBody User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (user.getPassword() == null) {
            throw new RuntimeException("Password is required");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        user.setRole(
                user.getRole() == null
                        ? "USER"
                        : user.getRole()
        );

        user.setActive(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

        saved.setPassword(null);

        return saved;
    }

    @PostMapping("/login")
    public User login(@RequestBody User loginRequest) {

        User user = userRepository.findByEmail(
                loginRequest.getEmail()
        ).orElseThrow(() ->
                new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException(
                    "Account is deactivated");
        }

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid credentials");
        }

        user.setPassword(null);

        return user;
    }

    @GetMapping("/{id}")
    public User getProfile(@PathVariable String id) {

        return userProfileService.getProfile(id);
    }

    @PutMapping("/{id}")
    public User updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request) {

        return userProfileService.updateProfile(
                id,
                request
        );
    }
@PostMapping(
        value = "/{id}/avatar",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
    public User updateProfileImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {

        return userProfileService.updateProfileImage(
                id,
                file
        );
    }

    @PutMapping("/{id}/password")
    public String changePassword(
            @PathVariable String id,
            @RequestBody ChangePasswordRequest request) {

        userProfileService.changePassword(
                id,
                request
        );

        return "Password updated successfully";
    }

    @PutMapping("/{id}/deactivate")
    public String deactivateAccount(
            @PathVariable String id) {

        userProfileService.deactivateAccount(id);

        return "Account deactivated successfully";
    }

    @PutMapping("/{id}/activate")
    public String activateAccount(
            @PathVariable String id) {

        userProfileService.activateAccount(id);

        return "Account activated successfully";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String token) {

        userProfileService.verifyEmail(token);

        return "Email verified successfully";
    }
}