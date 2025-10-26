package com.luxestay.hotel.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class để generate BCrypt hash cho passwords
 * Chạy main method này để tạo hash mới
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Generate hash cho các passwords
        String password1 = "admin123";
        String password2 = "123";
        String password3 = "customer123";

        System.out.println("=== Password Hashes ===");
        System.out.println();

        System.out.println("Password: " + password1);
        System.out.println("Hash: " + encoder.encode(password1));
        System.out.println();

        System.out.println("Password: " + password2);
        System.out.println("Hash: " + encoder.encode(password2));
        System.out.println();

        System.out.println("Password: " + password3);
        System.out.println("Hash: " + encoder.encode(password3));
        System.out.println();

        // Test verify
        String testHash = encoder.encode("123");
        System.out.println("=== Verify Test ===");
        System.out.println("Hash for '123': " + testHash);
        System.out.println("Verify '123' with hash: " + encoder.matches("123", testHash));
        System.out.println("Verify 'wrong' with hash: " + encoder.matches("wrong", testHash));
    }
}





