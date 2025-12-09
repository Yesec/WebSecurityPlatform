package com.example.websecurity.controller;

import com.example.websecurity.entity.User;
import com.example.websecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        model.addAttribute("user", user);
        return "user/profile";
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        model.addAttribute("user", user);
        return "user/edit";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User user,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User currentUser = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            userService.updateProfile(currentUser.getId(), user.getEmail(), user.getFullName(), ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "个人信息更新成功");
            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/edit";
        }
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "user/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "新密码和确认密码不一致");
                return "redirect:/user/change-password";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            User user = userService.findUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            userService.changePassword(user.getId(), oldPassword, newPassword, ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "密码修改成功");
            return "redirect:/user/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/change-password";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                          @RequestParam String confirmPassword,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            if (!user.getPassword().equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "密码和确认密码不一致");
                return "redirect:/user/register";
            }

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            userService.registerUser(user.getUsername(), user.getPassword(),
                                   user.getEmail(), user.getFullName(), ipAddress, userAgent);

            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/register";
        }
    }
}