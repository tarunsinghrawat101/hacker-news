package com.example.hackerNews.controller;
import com.example.hackerNews.dto.UserRegistrationDto;
import com.example.hackerNews.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/registration")
public class UserRegistrationController {
    private final UserService userService;

    public UserRegistrationController(UserService userService) {
        this.userService=userService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("user", new UserRegistrationDto());

        return "registration";
    }

    @PostMapping
    public String register(@ModelAttribute("user") UserRegistrationDto dto) {
        userService.register(dto);
        return "redirect:/login?success"; }
}
