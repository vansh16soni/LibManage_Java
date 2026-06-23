package com.libmanage.controller;

import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class MemberDeleteController {

    private final LocalStorageManager localStorageManager;

    @GetMapping("/delete/{id}")
    public String deleteMember(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        localStorageManager.deleteUser(id);
        return "redirect:/admin/members";
    }
}
