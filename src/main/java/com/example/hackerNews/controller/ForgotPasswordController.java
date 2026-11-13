package com.example.hackerNews.controller;
import com.example.hackerNews.service.PasswordResetService;
import com.example.hackerNews.utility.Utility;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

@Controller
public class ForgotPasswordController {
    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(PasswordResetService service) {
        passwordResetService=service;
    }

    @GetMapping("/forgotPassword")
    public String form(Model model) {
        model.addAttribute("pageTitle","Forgot Password");
        return "forgotPasswordForm";
    }

    @PostMapping("/forgotPassword")
    public String requestReset(HttpServletRequest request, Model model) {
        try {
            passwordResetService.sendResetLink(request.getParameter("email"), Utility.getSiteURL(request));
            model.addAttribute("message","Reset password link sent. Please check your email.");
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("pageTitle","Forgot Password");

        return "forgotPasswordForm";
    }
    @GetMapping("/resetPassword")
    public String resetForm(@RequestParam String token, Model model) {
        model.addAttribute("token",token);

        return passwordResetService.tokenExists(token) ? "resetPasswordForm" : "message";
    }
    @PostMapping("/resetPassword")
    public String reset(@RequestParam String token, @RequestParam String password, Model model) {
        if(!passwordResetService.resetPassword(token, password)) {
            model.addAttribute("message","Invalid or expired reset token.");
            return "message";
        }

        return "redirect:/login?passwordReset";
    }
}
