package com.libmanage.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String fullName;
    private String email;
    private String password; // plain text password for this simple refactor
    private String role = "MEMBER"; // "ADMIN" or "MEMBER"
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();

    public User(String id, String fullName, String email, String password, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }
}
