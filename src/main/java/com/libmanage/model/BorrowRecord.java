package com.libmanage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecord {
    private String id;
    private String userId;
    private String bookId;
    private LocalDateTime borrowDate = LocalDateTime.now();
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private BigDecimal fineAmount = BigDecimal.ZERO;
    private String status = "BORROWED"; // "BORROWED" or "RETURNED"

    // Transient, populated for view rendering only
    private String bookTitle;
    private String bookAuthor;
    private String bookCoverImageUrl;
    private String memberName;
    private String memberEmail;

    public BorrowRecord(String id, String userId, String bookId, LocalDateTime dueDate) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = LocalDateTime.now();
        this.dueDate = dueDate;
        this.fineAmount = BigDecimal.ZERO;
        this.status = "BORROWED";
    }
}
