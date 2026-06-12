package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.dto.UserRegistrationDto;
import com.portfolio.financetracker.exception.PasswordMismatchException;
import com.portfolio.financetracker.exception.UsernameAlreadyExistsException;
import com.portfolio.financetracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto dto,
                               BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(dto);
        } catch (UsernameAlreadyExistsException e) {
            bindingResult.rejectValue("username", "error.user", e.getMessage());
            return "auth/register";
        } catch (PasswordMismatchException e) {
            bindingResult.rejectValue("confirmPassword", "error.user", e.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
