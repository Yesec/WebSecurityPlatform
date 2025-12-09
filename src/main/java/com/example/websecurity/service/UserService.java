package com.example.websecurity.service;

import com.example.websecurity.entity.OperationLog;
import com.example.websecurity.entity.User;
import com.example.websecurity.repository.OperationLogRepository;
import com.example.websecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String username, String password, String email, String fullName, String ipAddress, String userAgent) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole("USER");
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        operationLogRepository.save(new OperationLog(savedUser, "用户注册",
            String.format("新用户注册: %s (%s)", username, email), ipAddress, userAgent));

        return savedUser;
    }

    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findActiveUserByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findActiveUserByUsername(username);
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long userId, String email, String fullName, String role, Boolean isActive, String ipAddress, String userAgent) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被其他用户使用");
        }

        String oldEmail = user.getEmail();
        user.setEmail(email);
        user.setFullName(fullName);

        if (role != null && !role.isEmpty()) {
            user.setRole(role);
        }

        if (isActive != null) {
            user.setIsActive(isActive);
        }

        User updatedUser = userRepository.save(user);

        operationLogRepository.save(new OperationLog(user, "用户信息更新",
            String.format("用户信息更新: 邮箱 %s -> %s, 姓名: %s, 状态: %s", oldEmail, email, fullName, isActive != null ? isActive : "unchanged"),
            ipAddress, userAgent));

        return updatedUser;
    }

    public void deleteUser(Long userId, String operatorUsername, String ipAddress, String userAgent) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        user.setIsActive(false);
        userRepository.save(user);

        operationLogRepository.save(new OperationLog(user, "用户删除",
            String.format("用户 %s 被管理员 %s 删除", user.getUsername(), operatorUsername),
            ipAddress, userAgent));
    }

    public void changePassword(Long userId, String oldPassword, String newPassword, String ipAddress, String userAgent) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        operationLogRepository.save(new OperationLog(user, "密码修改",
            "用户修改密码", ipAddress, userAgent));
    }

    public User updateProfile(Long userId, String email, String fullName, String ipAddress, String userAgent) {
        return updateUser(userId, email, fullName, null, null, ipAddress, userAgent);
    }

    public void logOperation(User user, String operationType, String operationDetail, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        operationLogRepository.save(new OperationLog(user, operationType, operationDetail, ipAddress, userAgent));
    }

    public List<OperationLog> findRecentLogs(LocalDateTime since) {
        return operationLogRepository.findRecentLogs(since);
    }

    public List<OperationLog> findLogsByOperationType(String operationType) {
        return operationLogRepository.findByOperationTypeOrderByCreatedAtDesc(operationType);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}