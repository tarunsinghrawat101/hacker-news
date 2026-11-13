package com.example.hackerNews.service;
import com.example.hackerNews.dto.UserRegistrationDto;
import com.example.hackerNews.entity.Role;
import com.example.hackerNews.entity.User;
import com.example.hackerNews.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public User findUserByEmail(String email) {
        return repository.findByEmail(email);
    }
    @Override
    public User getCurrentUser() {
        var auth=SecurityContextHolder.getContext().getAuthentication();

        return auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal()) ? null : repository.findByEmail(auth.getName());
    }

    @Override
    public boolean isAdmin() {
        var auth=SecurityContextHolder.getContext().getAuthentication();

        return auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Override
    @Transactional
    public User register(UserRegistrationDto dto) {
        if(repository.findByEmail(dto.getEmail())!=null)
            throw new IllegalArgumentException("Email is already registered");

        return repository.save(new User(dto.getName(),dto.getEmail(),encoder.encode(dto.getPassword()),Collections.singletonList(new Role("ROLE_USER"))));
    }

    @Override
    public List<User> getAllUser() {
        return repository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user=repository.findByEmail(email);
        if(user == null)
            throw new UsernameNotFoundException("Invalid username or password");

        return new org.springframework.security.core.userdetails.User(user.getEmail(),user.getPassword(),user.getRoles()
                .stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.getName()))
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public void updateResetPasswordToken(String token,String email) {
        User user=repository.findByEmail(email);
        if(user == null)
            throw new UsernameNotFoundException("No account found for this email");

        user.setResetPasswordToken(token);
    }

    @Override
    public User getByResetPasswordToken(String token) {
        return repository.findByResetPasswordToken(token);
    }

    @Override
    @Transactional
    public void updatePassword(User user,String password) {
        if(password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters");

        User managed = repository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        managed.setPassword(encoder.encode(password));
        managed.setResetPasswordToken(null);
    }
}
