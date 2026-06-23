package com.libmanage.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.libmanage.model.Book;
import com.libmanage.model.BorrowRecord;
import com.libmanage.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class LocalStorageManager {

    private static final String FILE_PATH = "database.json";
    private final ObjectMapper objectMapper;
    private Database database;

    public LocalStorageManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        loadData();
    }

    public synchronized void loadData() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try {
                this.database = objectMapper.readValue(file, Database.class);
                if (this.database == null) {
                    this.database = new Database();
                }
            } catch (IOException e) {
                System.err.println("Error reading database.json, initializing empty: " + e.getMessage());
                this.database = new Database();
            }
        } else {
            this.database = new Database();
            seedDefaultData();
        }
    }

    public synchronized void saveData() {
        try {
            objectMapper.writeValue(new File(FILE_PATH), this.database);
        } catch (IOException e) {
            System.err.println("Error writing to database.json: " + e.getMessage());
        }
    }

    private void seedDefaultData() {
        // Seed default Admin
        User admin = new User(
                UUID.randomUUID().toString(),
                "Library Admin",
                "admin@lib.com",
                "admin123",
                "ADMIN"
        );
        this.database.getUsers().add(admin);

        // Seed a default Member for testing
        User member = new User(
                UUID.randomUUID().toString(),
                "John Doe",
                "member@lib.com",
                "member123",
                "MEMBER"
        );
        this.database.getUsers().add(member);

        // Seed 3 books
        Book book1 = new Book(
                UUID.randomUUID().toString(),
                "The Great Gatsby",
                "F. Scott Fitzgerald",
                "9780743273565",
                5,
                5,
                "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&q=80&w=200",
                "A-12",
                "A classic novel of the Jazz Age, exploring themes of wealth, love, and the American Dream.",
                LocalDateTime.now()
        );
        Book book2 = new Book(
                UUID.randomUUID().toString(),
                "To Kill a Mockingbird",
                "Harper Lee",
                "9780446310789",
                3,
                3,
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&q=80&w=200",
                "B-03",
                "A powerful story of racial injustice and the destruction of innocence in the American South.",
                LocalDateTime.now()
        );
        Book book3 = new Book(
                UUID.randomUUID().toString(),
                "1984",
                "George Orwell",
                "9780451524935",
                4,
                4,
                "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&q=80&w=200",
                "C-08",
                "A dystopian social science fiction novel depicting a totalitarian regime and the search for truth.",
                LocalDateTime.now()
        );
        this.database.getBooks().add(book1);
        this.database.getBooks().add(book2);
        this.database.getBooks().add(book3);

        saveData();
    }

    public synchronized List<User> getUsers() {
        return database.getUsers();
    }

    public synchronized List<Book> getBooks() {
        return database.getBooks();
    }

    public synchronized List<BorrowRecord> getBorrowRecords() {
        return database.getBorrowRecords();
    }

    // Helper functions for easy CRUD operations

    public synchronized void saveUser(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }
        List<User> users = database.getUsers();
        int index = -1;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            users.set(index, user);
        } else {
            users.add(user);
        }
        saveData();
    }

    public synchronized User findUserById(String id) {
        return database.getUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public synchronized User findUserByEmail(String email) {
        return database.getUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public synchronized void saveBook(Book book) {
        if (book.getId() == null || book.getId().isEmpty()) {
            book.setId(UUID.randomUUID().toString());
        }
        List<Book> books = database.getBooks();
        int index = -1;
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getId().equals(book.getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            books.set(index, book);
        } else {
            books.add(book);
        }
        saveData();
    }

    public synchronized Book findBookById(String id) {
        return database.getBooks().stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public synchronized void deleteBook(String id) {
        database.getBooks().removeIf(b -> b.getId().equals(id));
        // Remove borrow records associated with this book as per deletion instructions
        database.getBorrowRecords().removeIf(r -> r.getBookId().equals(id));
        saveData();
    }

    public synchronized void deleteUser(String id) {
        database.getUsers().removeIf(u -> u.getId().equals(id));
        // Remove borrow records associated with this user
        database.getBorrowRecords().removeIf(r -> r.getUserId().equals(id));
        saveData();
    }

    public synchronized void saveBorrowRecord(BorrowRecord record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString());
        }
        List<BorrowRecord> records = database.getBorrowRecords();
        int index = -1;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId().equals(record.getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            records.set(index, record);
        } else {
            records.add(record);
        }
        saveData();
    }

    public synchronized BorrowRecord findBorrowRecordById(String id) {
        return database.getBorrowRecords().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // Database Wrapper Class
    public static class Database {
        private List<User> users = new ArrayList<>();
        private List<Book> books = new ArrayList<>();
        private List<BorrowRecord> borrowRecords = new ArrayList<>();

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }

        public List<Book> getBooks() {
            return books;
        }

        public void setBooks(List<Book> books) {
            this.books = books;
        }

        public List<BorrowRecord> getBorrowRecords() {
            return borrowRecords;
        }

        public void setBorrowRecords(List<BorrowRecord> borrowRecords) {
            this.borrowRecords = borrowRecords;
        }
    }
}
