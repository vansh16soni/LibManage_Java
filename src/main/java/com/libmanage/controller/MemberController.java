package com.libmanage.controller;

import com.libmanage.model.Book;
import com.libmanage.model.BorrowRecord;
import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final LocalStorageManager localStorageManager;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("userId");
        if (!"MEMBER".equals(role) || userId == null) {
            return "redirect:/login";
        }

        List<BorrowRecord> activeBorrows = localStorageManager.getBorrowRecords().stream()
                .filter(r -> r.getUserId().equals(userId) && "BORROWED".equals(r.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("currentUserId", userId);
        model.addAttribute("activeBorrows", activeBorrows);
        return "member/dashboard";
    }

    @GetMapping("/my-books")
    public String myBooks(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        String userId = (String) session.getAttribute("userId");
        if (!"MEMBER".equals(role) || userId == null) {
            return "redirect:/login";
        }

        List<BorrowRecord> history = localStorageManager.getBorrowRecords().stream()
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> {
                    Book b = localStorageManager.findBookById(r.getBookId());
                    if (b != null) {
                        r.setBookTitle(b.getTitle());
                        r.setBookAuthor(b.getAuthor());
                        r.setBookCoverImageUrl(b.getCoverImageUrl());
                    }
                    return r;
                })
                .collect(Collectors.toList());

        model.addAttribute("history", history);
        return "member/my-books";
    }
}
