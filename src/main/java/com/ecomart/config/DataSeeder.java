package com.ecomart.config;

import com.ecomart.entity.Role;
import com.ecomart.entity.User;
import com.ecomart.repository.RoleRepository;
import com.ecomart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("CUSTOMER")
                        .description("Khách hàng của EcoMart")
                        .build()));

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ADMIN")
                        .description("Quản trị viên EcoMart")
                        .build()));

        if (!userRepository.existsByEmail("admin@ecomart.com")) {
            User admin = User.builder()
                    .fullName("Admin EcoMart")
                    .email("admin@ecomart.com")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .phoneNumber("0900000000")
                    .isActive(true)
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Khởi tạo tài khoản Admin mặc định thành công: admin@ecomart.com");
        }
    }
}
