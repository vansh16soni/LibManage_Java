package com.libmanage.controller;

import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class BookController {

    private final LocalStorageManager localStorageManager;

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        localStorageManager.deleteBook(id);
        return "redirect:/admin/books";
    }
}
