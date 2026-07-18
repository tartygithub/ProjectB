package com.finance.app.service;

import com.finance.app.config.AppSecurityProperties;
import com.finance.app.model.User;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSecurityProperties appSecurityProperties;

    public void validatePassword(String password) {
        String regex = appSecurityProperties.getBasicValidationRegex();
        if (regex == null || regex.isEmpty()) {
            return;
        }
        if (!Pattern.compile(regex).matcher(password).matches()) {
            throw new IllegalArgumentException(appSecurityProperties.getBasicValidationMessage());
        }
    }

    public User registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }
        validatePassword(password);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build();
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
