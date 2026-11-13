package com.example.hackerNews.service;
import com.example.hackerNews.dto.UserRegistrationDto;
import com.example.hackerNews.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;

public interface UserService extends UserDetailsService {
    User findUserByEmail(String email);

    User getCurrentUser();

    boolean isAdmin();

    User register(UserRegistrationDto dto);

    List<User> getAllUser();

    void updateResetPasswordToken(String token, String email);

    User getByResetPasswordToken(String token);

    void updatePassword(User user, String password);
}
