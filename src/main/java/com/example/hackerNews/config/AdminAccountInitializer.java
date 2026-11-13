package com.example.hackerNews.config;

import com.example.hackerNews.entity.Role;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

@Component
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminAccountInitializer implements CommandLineRunner {

    @Value("${app.bootstrap.admin.email:admin@gmail.com}")
    private String email;
    @Value("${app.bootstrap.admin.password:admin@123}")
    private String password;

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminAccountInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User existing = users.findByEmail(email);
        if (existing != null) {
            if (existing.getRoles().stream().noneMatch(role -> "ROLE_ADMIN".equals(role.getName()))) {
                throw new IllegalStateException("Admin bootstrap email is already used by a non-admin account: " + email);
            }
            return;
        }
        users.save(new User("Administrator", email, encoder.encode(password),
                Collections.singletonList(new Role("ROLE_ADMIN"))));
    }
}
