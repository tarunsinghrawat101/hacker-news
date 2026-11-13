package com.example.hackerNews.repository;

import com.example.hackerNews.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository  extends JpaRepository<User, Long> {
    User findByEmail(String email);

    public User findByResetPasswordToken(String token);
}

