package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.dto.LoginRequest;
import com.gradientgeeks.ageis.backendapp.dto.LoginResponse;
import com.gradientgeeks.ageis.backendapp.dto.UserResponse;
import com.gradientgeeks.ageis.backendapp.entity.User;
import com.gradientgeeks.ageis.backendapp.exception.AuthenticationException;
import com.gradientgeeks.ageis.backendapp.repository.UserRepository;
import com.gradientgeeks.ageis.backendapp.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${app.jwt.expiration:86400}")
    private Long jwtExpirationInSeconds;
    
    @Autowired
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        
        // Find user by username
        User user = userRepository.findByUsernameAndIsActiveTrue(loginRequest.getUsername())
                .orElseThrow(() -> {
                    logger.warn("Login failed: User not found or inactive - {}", loginRequest.getUsername());
                    return new AuthenticationException("Invalid username or password");
                });
        
        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for user - {}", loginRequest.getUsername());
            throw new AuthenticationException("Invalid username or password");
        }
        
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        
        // Create user response
        UserResponse userResponse = UserResponse.fromUser(user);
        
        // Create and return login response
        LoginResponse loginResponse = new LoginResponse(token, jwtExpirationInSeconds, userResponse);
        
        logger.info("Login successful for user: {}", user.getUsername());
        return loginResponse;
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found"));
    }
    
    public boolean validateUser(String username) {
        return userRepository.findByUsernameAndIsActiveTrue(username).isPresent();
    }
}