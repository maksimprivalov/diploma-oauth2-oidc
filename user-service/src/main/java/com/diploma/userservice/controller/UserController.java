package com.diploma.userservice.controller;

import com.diploma.userservice.dto.UpdateUserRequest;
import com.diploma.userservice.entity.AppUser;
import com.diploma.userservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<AppUser> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    AppUser newUser = new AppUser(
                        keycloakId,
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("name")
                    );
                    return ResponseEntity.ok(userRepository.save(newUser));
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUser> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return userRepository.findById(id)
                .map(user -> {
                    String keycloakId = jwt.getSubject();
                    boolean isAdmin = jwt.getClaimAsMap("realm_access") != null
                            && ((List<?>) jwt.getClaimAsMap("realm_access").get("roles"))
                                .contains("admin");

                    if (!user.getKeycloakId().equals(keycloakId) && !isAdmin) {
                        return ResponseEntity.status(403).<AppUser>build();
                    }

                    if (request.getEmail() != null) user.setEmail(request.getEmail());
                    if (request.getFullName() != null) user.setFullName(request.getFullName());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
