package com.example.hackerNews.service;

import com.example.hackerNews.entity.User;
import net.bytebuddy.utility.RandomString;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    private final UserService users;
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}")
    private String fromAddress;

    public PasswordResetServiceImpl(UserService users,JavaMailSender mailSender) {
        this.users=users; this.mailSender=mailSender;
    }
    @Override
    @Transactional
    public void sendResetLink(String email,String siteUrl) {
        String token=RandomString.make(48);
        users.updateResetPasswordToken(token,email);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true, StandardCharsets.UTF_8.name());
            if(fromAddress == null || fromAddress.trim().isEmpty())
                throw new IllegalStateException("MAIL_USERNAME must be configured to send password reset emails");
            helper.setFrom(fromAddress,"Hacker News Clone Support");
            helper.setTo(email);
            helper.setSubject("Password reset request");
            helper.setText("<p>You requested a password reset.</p><p><a href=\""
                    +siteUrl+"/resetPassword?token="+token+
                    "\">Reset password</a></p><p>If you did not request this, ignore this email.</p>",true);

            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send password reset email",ex);
        }
    }
    @Override
    public boolean tokenExists(String token) {
        return users.getByResetPasswordToken(token)!=null;
    }

    @Override
    @Transactional
    public boolean resetPassword(String token,String password) {
        User user=users.getByResetPasswordToken(token);
        if(user==null)
            return false;

        users.updatePassword(user,password); return true; }
}
