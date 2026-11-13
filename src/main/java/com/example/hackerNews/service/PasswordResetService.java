package com.example.hackerNews.service;

public interface PasswordResetService {
    void sendResetLink(String email, String siteUrl);
    boolean resetPassword(String token, String password);
    boolean tokenExists(String token);
}
