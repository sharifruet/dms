package com.bpdb.dms.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "app_document_entries")
public class AppDocumentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    public AppDocumentEntry() {
    }

    public AppDocumentEntry(Document document, LocalDate entryDate, String title, BigDecimal amount) {
        this.document = document;
        this.entryDate = entryDate;
        this.title = title;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

