package com.libmanage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    private String id;
    private String title;
    private String author;
    private String isbn;
    private int totalQuantity;
    private int availableQuantity;
    private String coverImageUrl;
    private String shelfLocation;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    public boolean isAvailable() {
        return availableQuantity > 0;
    }
}
