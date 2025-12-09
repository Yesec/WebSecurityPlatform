package com.example.websecurity.config;

import com.example.websecurity.entity.User;
import com.example.websecurity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setFullName("系统管理员");
            admin.setRole("ADMIN");
            admin.setIsActive(true);

            userRepository.save(admin);
            System.out.println("Admin用户已创建: admin/admin123");
        }

        if (!userRepository.existsByUsername("testuser")) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("123456"));
            testUser.setEmail("user@example.com");
            testUser.setFullName("测试用户");
            testUser.setRole("USER");
            testUser.setIsActive(true);

            userRepository.save(testUser);
            System.out.println("测试用户已创建: testuser/123456");
        }
    }
}