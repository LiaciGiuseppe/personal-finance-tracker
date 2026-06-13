package com.portfolio.financetracker.controller;

import com.portfolio.financetracker.dto.ChangePasswordDto;
import com.portfolio.financetracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String profile(Model model) {
        if (!model.containsAttribute("changePasswordDto")) {
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
        }
        return "profile/index";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDto") ChangePasswordDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("changePasswordDto", dto);
            return "profile/index";
        }
        try {
            userService.changePassword(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("passwordChanged", true);
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("currentPassword", "error", e.getMessage());
            model.addAttribute("changePasswordDto", dto);
            return "profile/index";
        }
    }

    @PostMapping("/delete-account")
    public String deleteAccount(@RequestParam("currentPassword") String currentPassword,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.deleteAccount(userDetails.getUsername(), currentPassword);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/profile";
        }

        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, null);
        return "redirect:/login?deleted";
    }
}
