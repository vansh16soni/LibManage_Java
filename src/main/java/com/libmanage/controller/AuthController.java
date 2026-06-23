package com.libmanage.controller;

import com.libmanage.model.User;
import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final LocalStorageManager localStorageManager;

    @GetMapping("/")
    public String root(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if ("ADMIN".equals(role)) {
            return "redirect:/admin/dashboard";
        } else if ("MEMBER".equals(role)) {
            return "redirect:/member/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String logout,
                             @RequestParam(required = false) String blocked,
                             @RequestParam(required = false) String registered,
                             Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (blocked != null) {
            model.addAttribute("errorMessage", "Your account has been blocked. Contact the library admin.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration successful! Please log in.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        User user = localStorageManager.findUserByEmail(email);
        if (user == null || !user.getPassword().equals(password)) {
            return "redirect:/login?error=true";
        }
        if (!user.isActive()) {
            return "redirect:/login?blocked=true";
        }

        // Set session attributes
        session.setAttribute("userId", user.getId());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("role", user.getRole());

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/member/dashboard";
        }
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/logout", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("fullName", "");
        model.addAttribute("email", "");
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam String confirmPassword,
                            Model model) {
        if (fullName == null || fullName.isBlank()) {
            model.addAttribute("errorMessage", "Full name is required.");
            model.addAttribute("fullName", "");
            model.addAttribute("email", email);
            return "register";
        }
        if (email == null || email.isBlank()) {
            model.addAttribute("errorMessage", "Email is required.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", "");
            return "register";
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            model.addAttribute("errorMessage", "Invalid email address format.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (localStorageManager.findUserByEmail(email) != null) {
            model.addAttribute("errorMessage", "An account with this email already exists.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }
        if (password.length() < 6) {
            model.addAttribute("errorMessage", "Password must be at least 6 characters.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        User newUser = new User(null, fullName, email, password, "MEMBER");
        localStorageManager.saveUser(newUser);

        return "redirect:/login?registered=true";
    }
}
