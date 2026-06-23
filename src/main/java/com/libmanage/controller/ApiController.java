package com.libmanage.controller;

import com.libmanage.model.Book;
import com.libmanage.model.BorrowRecord;
import com.libmanage.model.User;
import com.libmanage.storage.LocalStorageManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final LocalStorageManager localStorageManager;

    @Value("${libmanage.loan.period-days:14}")
    private int defaultLoanPeriodDays;

    @Value("${libmanage.fine.per-day:5}")
    private double finePerDay;

    // ---------- Books ----------

    @GetMapping("/books/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam(required = false, defaultValue = "") String q) {
        String query = q.toLowerCase().trim();
        List<Book> results = localStorageManager.getBooks().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(query)
                        || b.getAuthor().toLowerCase().contains(query)
                        || b.getIsbn().toLowerCase().contains(query))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/books", consumes = "multipart/form-data")
    public ResponseEntity<?> addBook(@RequestParam String title,
                                      @RequestParam String author,
                                      @RequestParam String isbn,
                                      @RequestParam int totalQuantity,
                                      @RequestParam(required = false) String shelfLocation,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) MultipartFile coverImage,
                                      HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        // Validate ISBN unique
        boolean isbnExists = localStorageManager.getBooks().stream()
                .anyMatch(b -> b.getIsbn().equalsIgnoreCase(isbn));
        if (isbnExists) {
            return ResponseEntity.badRequest().body(errorBody("A book with this ISBN already exists."));
        }

        Book book = new Book();
        book.setId(UUID.randomUUID().toString());
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setTotalQuantity(totalQuantity);
        book.setAvailableQuantity(totalQuantity);
        book.setShelfLocation(shelfLocation);
        book.setDescription(description);
        book.setCreatedAt(LocalDateTime.now());
        // Set placeholder cover image URL
        book.setCoverImageUrl("https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=200");

        localStorageManager.saveBook(book);
        return ResponseEntity.ok(book);
    }

    @PutMapping(value = "/books/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateBook(@PathVariable String id,
                                         @RequestParam String title,
                                         @RequestParam String author,
                                         @RequestParam String isbn,
                                         @RequestParam int totalQuantity,
                                         @RequestParam(required = false) String shelfLocation,
                                         @RequestParam(required = false) String description,
                                         @RequestParam(required = false) MultipartFile coverImage,
                                         HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        Book existing = localStorageManager.findBookById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // Validate ISBN unique if changed
        if (!existing.getIsbn().equalsIgnoreCase(isbn)) {
            boolean isbnExists = localStorageManager.getBooks().stream()
                    .anyMatch(b -> b.getIsbn().equalsIgnoreCase(isbn));
            if (isbnExists) {
                return ResponseEntity.badRequest().body(errorBody("A book with this ISBN already exists."));
            }
        }

        int borrowedCount = existing.getTotalQuantity() - existing.getAvailableQuantity();
        if (totalQuantity < borrowedCount) {
            return ResponseEntity.badRequest().body(errorBody("Cannot decrease total quantity below currently borrowed amount (" + borrowedCount + ")."));
        }

        existing.setTitle(title);
        existing.setAuthor(author);
        existing.setIsbn(isbn);
        existing.setTotalQuantity(totalQuantity);
        existing.setAvailableQuantity(totalQuantity - borrowedCount);
        existing.setShelfLocation(shelfLocation);
        existing.setDescription(description);

        localStorageManager.saveBook(existing);
        return ResponseEntity.ok(existing);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        localStorageManager.deleteBook(id);
        return ResponseEntity.ok(Map.of("message", "Book deleted."));
    }

    // ---------- Members ----------

    @GetMapping("/members/search")
    public ResponseEntity<List<User>> searchMembers(@RequestParam(required = false, defaultValue = "") String q, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String query = q.toLowerCase().trim();
        List<User> results = localStorageManager.getUsers().stream()
                .filter(u -> "MEMBER".equals(u.getRole()))
                .filter(u -> u.getFullName().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query))
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/members")
    public ResponseEntity<?> addMember(@RequestBody Map<String, String> body, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        String fullName = body.get("fullName");
        String email = body.get("email");
        String password = body.get("password");
        if (fullName == null || fullName.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(errorBody("All fields are required."));
        }

        if (localStorageManager.findUserByEmail(email) != null) {
            return ResponseEntity.badRequest().body(errorBody("An account with this email already exists."));
        }

        User user = new User(UUID.randomUUID().toString(), fullName, email, password, "MEMBER");
        localStorageManager.saveUser(user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/members/{id}/block")
    public ResponseEntity<?> toggleBlock(@PathVariable String id, @RequestParam boolean active, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        User u = localStorageManager.findUserById(id);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        u.setActive(active);
        localStorageManager.saveUser(u);
        return ResponseEntity.ok(Map.of("id", u.getId(), "active", u.isActive()));
    }

    // ---------- Transactions ----------

    @PostMapping("/transactions/borrow")
    public ResponseEntity<?> borrow(@RequestBody Map<String, String> body, HttpSession session) {
        String sessionUserId = (String) session.getAttribute("userId");
        String sessionRole = (String) session.getAttribute("role");
        if (sessionUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Unauthorized. Please log in."));
        }

        String userId = body.get("userId");
        String bookId = body.get("bookId");
        String loanPeriodStr = body.get("loanPeriodDays");

        if (!"ADMIN".equals(sessionRole) && !sessionUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        User member = localStorageManager.findUserById(userId);
        if (member == null) {
            return ResponseEntity.badRequest().body(errorBody("Member not found."));
        }
        if (!member.isActive()) {
            return ResponseEntity.badRequest().body(errorBody("Member account is blocked."));
        }

        Book book = localStorageManager.findBookById(bookId);
        if (book == null) {
            return ResponseEntity.badRequest().body(errorBody("Book not found."));
        }
        if (book.getAvailableQuantity() <= 0) {
            return ResponseEntity.badRequest().body(errorBody("Book is out of stock."));
        }

        int loanDays = defaultLoanPeriodDays;
        if (loanPeriodStr != null && !loanPeriodStr.isBlank()) {
            try {
                loanDays = Integer.parseInt(loanPeriodStr);
            } catch (NumberFormatException e) {
                // Keep default
            }
        }

        // Decrement stock
        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        localStorageManager.saveBook(book);

        // Save borrow record
        LocalDateTime dueDate = LocalDateTime.now().plusDays(loanDays);
        BorrowRecord record = new BorrowRecord(UUID.randomUUID().toString(), userId, bookId, dueDate);
        localStorageManager.saveBorrowRecord(record);

        return ResponseEntity.ok(record);
    }

    @PostMapping("/transactions/return/{recordId}")
    public ResponseEntity<?> returnBook(@PathVariable String recordId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody("Access denied."));
        }

        BorrowRecord record = localStorageManager.findBorrowRecordById(recordId);
        if (record == null) {
            return ResponseEntity.badRequest().body(errorBody("Borrow record not found."));
        }
        if ("RETURNED".equals(record.getStatus())) {
            return ResponseEntity.badRequest().body(errorBody("Book has already been returned."));
        }

        LocalDateTime returnDate = LocalDateTime.now();
        record.setReturnDate(returnDate);
        record.setStatus("RETURNED");

        // Calculate fine
        if (returnDate.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            if (overdueDays > 0) {
                record.setFineAmount(BigDecimal.valueOf(overdueDays * finePerDay));
            }
        } else {
            record.setFineAmount(BigDecimal.ZERO);
        }

        // Restore book stock
        Book book = localStorageManager.findBookById(record.getBookId());
        if (book != null) {
            book.setAvailableQuantity(Math.min(book.getTotalQuantity(), book.getAvailableQuantity() + 1));
            localStorageManager.saveBook(book);
        }

        localStorageManager.saveBorrowRecord(record);
        return ResponseEntity.ok(record);
    }

    // ---------- Leaderboard ----------

    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard() {
        Map<String, Long> countsMap = localStorageManager.getBorrowRecords().stream()
                .collect(Collectors.groupingBy(BorrowRecord::getBookId, Collectors.counting()));

        List<AdminController.LeaderboardEntry> entries = countsMap.entrySet().stream()
                .map(entry -> {
                    Book book = localStorageManager.findBookById(entry.getKey());
                    return new AdminController.LeaderboardEntry(book, entry.getValue());
                })
                .filter(entry -> entry.getBook() != null)
                .sorted((e1, e2) -> Long.compare(e2.getBorrowCount(), e1.getBorrowCount()))
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> payload = entries.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getBook().getId());
            m.put("title", e.getBook().getTitle());
            m.put("author", e.getBook().getAuthor());
            m.put("coverImageUrl", e.getBook().getCoverImageUrl());
            m.put("availableQuantity", e.getBook().getAvailableQuantity());
            m.put("borrowCount", e.getBorrowCount());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(payload);
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message == null ? "Something went wrong." : message);
    }
}
