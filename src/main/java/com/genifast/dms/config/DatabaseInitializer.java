package com.genifast.dms.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.genifast.dms.entity.User;
import com.genifast.dms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> STARTING DATABASE INITIALIZATION <<<");
        long countUsers = this.userRepository.count();

        if (countUsers == 0) {
            User adminUser = new User();
            adminUser.setEmail("admin@gmail.com");
            adminUser.setPassword(this.passwordEncoder.encode("123456"));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("Admin");
            adminUser.setGender(false);
            adminUser.setStatus(1);
            adminUser.setIsAdmin(true);

            this.userRepository.save(adminUser);
        }

        if (countUsers > 0) {
            System.out.println(">>> DATABASE INITIALIZATION SKIPPED <<<");

        } else {
            System.out.println(">>> DATABASE INITIALIZATION COMPLETED <<<");
        }
    }

}
