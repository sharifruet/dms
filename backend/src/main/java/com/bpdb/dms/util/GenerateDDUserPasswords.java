package com.bpdb.dms.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateDDUserPasswords {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hashes for DD users - using "dd123" as password
        String password = "dd123";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println();
        
        // Generate a few more for consistency check
        for (int i = 0; i < 3; i++) {
            String hash2 = encoder.encode(password);
            System.out.println("Hash " + (i+2) + ": " + hash2);
        }
    }
}

