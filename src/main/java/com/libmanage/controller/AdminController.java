package com.libmanage.controller;

import com.libmanage.model.Book;
import com.libmanage.model.BorrowRecord;
import com.libmanage.model.User;
import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LocalStorageManager localStorageManager;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        long totalBooks = localStorageManager.getBooks().size();
        long totalMembers = localStorageManager.getUsers().stream()
                .filter(u -> "MEMBER".equals(u.getRole()))
                .count();
        long currentlyBorrowed = localStorageManager.getBorrowRecords().stream()
                .filter(r -> "BORROWED".equals(r.getStatus()))
                .count();
        long pendingReturns = localStorageManager.getBorrowRecords().stream()
                .filter(r -> "BORROWED".equals(r.getStatus()) && r.getDueDate().isBefore(LocalDateTime.now()))
                .count();

        // Calculate Leaderboard
        Map<String, Long> countsMap = localStorageManager.getBorrowRecords().stream()
                .collect(Collectors.groupingBy(BorrowRecord::getBookId, Collectors.counting()));

        List<LeaderboardEntry> leaderboard = countsMap.entrySet().stream()
                .map(entry -> {
                    Book book = localStorageManager.findBookById(entry.getKey());
                    return new LeaderboardEntry(book, entry.getValue());
                })
                .filter(entry -> entry.getBook() != null)
                .sorted((e1, e2) -> Long.compare(e2.getBorrowCount(), e1.getBorrowCount()))
                .limit(5)
                .collect(Collectors.toList());

        // Enriched active borrows
        List<BorrowRecord> activeBorrows = localStorageManager.getBorrowRecords().stream()
                .filter(r -> "BORROWED".equals(r.getStatus()))
                .map(r -> {
                    Book b = localStorageManager.findBookById(r.getBookId());
                    User u = localStorageManager.findUserById(r.getUserId());
                    if (b != null) {
                        r.setBookTitle(b.getTitle());
                        r.setBookAuthor(b.getAuthor());
                        r.setBookCoverImageUrl(b.getCoverImageUrl());
                    }
                    if (u != null) {
                        r.setMemberName(u.getFullName());
                        r.setMemberEmail(u.getEmail());
                    }
                    return r;
                })
                .collect(Collectors.toList());

        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("currentlyBorrowed", currentlyBorrowed);
        model.addAttribute("pendingReturns", pendingReturns);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("activeBorrows", activeBorrows);

        return "admin/dashboard";
    }

    @GetMapping("/books")
    public String books(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        List<Book> books = localStorageManager.getBooks();
        model.addAttribute("books", books);
        model.addAttribute("newBook", new Book());
        return "admin/books";
    }


    @GetMapping("/members")
    public String members(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        List<User> members = localStorageManager.getUsers().stream()
                .filter(u -> "MEMBER".equals(u.getRole()))
                .collect(Collectors.toList());
        model.addAttribute("members", members);
        return "admin/members";
    }

    @GetMapping("/members/block/{id}")
    public String toggleBlock(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        User u = localStorageManager.findUserById(id);
        if (u != null) {
            u.setActive(!u.isActive());
            localStorageManager.saveUser(u);
        }
        return "redirect:/admin/members";
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return "redirect:/login";
        }

        List<BorrowRecord> activeBorrows = localStorageManager.getBorrowRecords().stream()
                .filter(r -> "BORROWED".equals(r.getStatus()))
                .map(r -> {
                    Book b = localStorageManager.findBookById(r.getBookId());
                    User u = localStorageManager.findUserById(r.getUserId());
                    if (b != null) {
                        r.setBookTitle(b.getTitle());
                        r.setBookAuthor(b.getAuthor());
                        r.setBookCoverImageUrl(b.getCoverImageUrl());
                    }
                    if (u != null) {
                        r.setMemberName(u.getFullName());
                        r.setMemberEmail(u.getEmail());
                    }
                    return r;
                })
                .collect(Collectors.toList());

        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("books", localStorageManager.getBooks());
        return "admin/transactions";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private Book book;
        private long borrowCount;
    }
}
