package com.bpdb.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Main application class for Document Management System (DMS)
 * 
 * This application provides a comprehensive solution for managing,
 * storing, and tracking organizational documents with advanced OCR
 * capabilities, automated metadata extraction, and intelligent
 * document lifecycle management.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.bpdb.dms.repository")
@EnableRedisRepositories(basePackages = "com.bpdb.dms.repository.redis")
public class DmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsApplication.class, args);
    }
}
