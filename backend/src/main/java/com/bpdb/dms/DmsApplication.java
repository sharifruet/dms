package com.bpdb.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Document Management System (DMS)
 *
 * This application provides a comprehensive solution for managing,
 * storing, and tracking organizational documents with advanced OCR
 * capabilities, automated metadata extraction, and intelligent
 * document lifecycle management.
 *
 * <p>Repository scanning and JPA auditing live in
 * {@link com.bpdb.dms.config.PersistenceConfig} and
 * {@link com.bpdb.dms.config.JpaAuditingConfig} rather than here, so that web-only test
 * slices are not forced to build a JPA layer they do not have.
 */
@SpringBootApplication
public class DmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsApplication.class, args);
    }
}
