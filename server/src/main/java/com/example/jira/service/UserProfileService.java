package com.example.jira.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.jira.dto.ChangePasswordRequest;
import com.example.jira.dto.UpdateProfileRequest;
import com.example.jira.model.EmailVerificationToken;
import com.example.jira.model.User;
import com.example.jira.repository.EmailVerificationTokenRepository;
import com.example.jira.repository.UserRepository;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,64}$"
            );

    public UserProfileService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(String userId) {

        User user = findUser(userId);

        user.setPassword(null);

        return user;
    }

    public User updateProfile(
            String userId,
            UpdateProfileRequest request) {

        User user = findUser(userId);

        if (request.getName() != null &&
                !request.getName().trim().isEmpty()) {

            user.setName(request.getName().trim());
        }

        if (request.getGroup() != null) {
            user.setGroup(request.getGroup().trim());
        }

        if (request.getEmail() != null &&
                !request.getEmail().equalsIgnoreCase(user.getEmail())) {

            String newEmail = request.getEmail().trim().toLowerCase();

            validateEmail(newEmail);

            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new RuntimeException("Email already exists");
            }

            createEmailVerificationToken(user, newEmail);

            user.setPassword(null);

            return user;
        }

        User saved = userRepository.save(user);
        saved.setPassword(null);

        return saved;
    }

   public User updateProfileImage(
        String userId,
        MultipartFile file) {

    if (file == null || file.isEmpty()) {
        throw new RuntimeException("Profile image is required");
    }

    if (file.getSize() > 5 * 1024 * 1024) {
        throw new RuntimeException(
                "Profile image must not exceed 5MB");
    }

    String contentType = file.getContentType();

    if (contentType == null ||
            !(contentType.equals("image/jpeg") ||
              contentType.equals("image/png") ||
              contentType.equals("image/webp"))) {

        throw new RuntimeException(
                "Only JPG, PNG and WEBP images are allowed");
    }

    try {
        String base64 = java.util.Base64
                .getEncoder()
                .encodeToString(file.getBytes());

        User user = findUser(userId);

        user.setAvatar(
                "data:" + contentType + ";base64," + base64
        );

        User saved = userRepository.save(user);

        saved.setPassword(null);

        return saved;

    } catch (Exception e) {
        e.printStackTrace();

        throw new RuntimeException(
                "Failed to upload profile image: "
                        + e.getMessage());
    }
}

    public void changePassword(
            String userId,
            ChangePasswordRequest request) {

        User user = findUser(userId);

        if (request.getCurrentPassword() == null ||
                request.getNewPassword() == null) {

            throw new RuntimeException(
                    "Current and new password are required");
        }

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Current password is incorrect");
        }

        validatePassword(request.getNewPassword());

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "New password must be different");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);
    }

    public void deactivateAccount(String userId) {

        User user = findUser(userId);

        user.setActive(false);

        userRepository.save(user);
    }

    public void activateAccount(String userId) {

        User user = findUser(userId);

        user.setActive(true);

        userRepository.save(user);
    }

    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid verification token"));

        if (verificationToken.getExpiresAt()
                .isBefore(Instant.now())) {

            tokenRepository.delete(verificationToken);

            throw new RuntimeException(
                    "Verification token has expired");
        }

        User user = findUser(
                verificationToken.getUserId()
        );

        user.setEmail(
                verificationToken.getNewEmail()
        );

        user.setEmailVerified(true);

        userRepository.save(user);

        tokenRepository.delete(verificationToken);
    }

    private void createEmailVerificationToken(
            User user,
            String newEmail) {

        tokenRepository.deleteByUserId(user.getId());

        EmailVerificationToken token =
                new EmailVerificationToken();

        token.setUserId(user.getId());
        token.setNewEmail(newEmail);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(
                Instant.now().plus(24, ChronoUnit.HOURS)
        );

        tokenRepository.save(token);

        System.out.println(
                "EMAIL VERIFICATION LINK: " +
                "http://localhost:3000/verify-email?token=" +
                token.getToken()
        );
    }

    private void validateEmail(String email) {

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Invalid email address");
        }
    }

    private void validatePassword(String password) {

        if (!PASSWORD_PATTERN.matcher(password).matches()) {

            throw new RuntimeException(
                    "Password must be 8-64 characters and contain " +
                    "uppercase, lowercase, number and special character"
            );
        }
    }

    private User findUser(String userId) {

        return userRepository.findById(
                new org.bson.types.ObjectId(userId)
        ).orElseThrow(() ->
                new RuntimeException("User not found"));
    }
}