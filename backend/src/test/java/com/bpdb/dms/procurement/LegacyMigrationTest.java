package com.bpdb.dms.procurement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bpdb.dms.procurement.entity.ProcurementPackage;
import com.bpdb.dms.procurement.repository.ExtractedFieldRepository;
import com.bpdb.dms.procurement.repository.ProcurementPackageRepository;
import com.bpdb.dms.procurement.service.LegacyMigrationService;
import com.bpdb.dms.procurement.service.LegacyMigrationService.MigrationReport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7 migration, against the real schema.
 *
 * <p>The legacy tables are empty in every environment reachable from here, so the rows are
 * synthetic — this proves the mechanics, not that BPDB's data survives the trip. That
 * second thing needs a production copy in staging and is what the plan means by a
 * signed-off reconciliation report.
 *
 * <p>What is worth pinning regardless: the dry run writes nothing, running twice is
 * harmless, and rows the migration cannot place are reported rather than dropped. Those
 * are the properties that make a migration safe to run on data you cannot get back.
 */
class LegacyMigrationTest extends PostgresLiquibaseTest {

    @Autowired private LegacyMigrationService migrationService;
    @Autowired private ProcurementPackageRepository packageRepository;
    @Autowired private ExtractedFieldRepository fieldRepository;
    @Autowired private JdbcTemplate jdbc;

    private long userId() {
        return jdbc.queryForObject("SELECT id FROM users ORDER BY id LIMIT 1", Long.class);
    }

    private long insertDocument(String name, String extractedText) {
        return jdbc.queryForObject(
                "INSERT INTO documents (file_name, file_path, file_size, document_type, "
                        + "uploaded_by, is_active, is_archived, extracted_text, created_at, updated_at) "
                        + "VALUES (?, ?, 1024, 'OTHER', ?, true, false, ?, now(), now()) RETURNING id",
                Long.class, name, "uploads/" + name, userId(), extractedText);
    }

    private ProcurementPackage aPackage(String number) {
        ProcurementPackage pkg = new ProcurementPackage();
        pkg.setPackageNumber(number);
        pkg.setDepartment("BPDB");
        pkg.setFiscalYear(2026);
        return packageRepository.save(pkg);
    }

    // ------------------------------------------- document_metadata -> extracted_field

    @Test
    void metadataOnALinkedDocumentBecomesACapturedField() {
        ProcurementPackage pkg = aPackage("MIG-" + System.nanoTime());
        long documentId = insertDocument("legacy.pdf", "");
        jdbc.update("INSERT INTO document_link (document_id, entity_type, entity_id, package_id, "
                + "stage_code, doc_role, link_origin, created_at) "
                + "VALUES (?, 'PACKAGE', ?, ?, 1, 'MIGRATED', 'MANUAL', now())",
                documentId, pkg.getId(), pkg.getId());
        jdbc.update("INSERT INTO document_metadata (document_id, metadata_key, metadata_value, "
                + "source, confidence, created_at, updated_at) "
                + "VALUES (?, ?, 'Supply of transformers', 'OCR', 0.91, now(), now())",
                documentId, "legacy_description_" + documentId);

        MigrationReport report = migrationService.migrateDocumentMetadata(false, userId());

        assertTrue(report.migrated >= 1, report.toString());
        assertTrue(report.reconciles(), "every row must be accounted for: " + report);
        assertTrue(fieldRepository.findByPackageId(pkg.getId()).stream()
                        .anyMatch(f -> "Supply of transformers".equals(f.displayValue())),
                "the value should have arrived in the capture store");
    }

    @Test
    void migratedValuesArriveAsSuggestionsNotAsConfirmed() {
        // Nobody ever reviewed these. Importing them as verified would fabricate a review
        // that never happened, and the verify screen would have nothing left to ask.
        ProcurementPackage pkg = aPackage("MIG-" + System.nanoTime());
        long documentId = insertDocument("legacy2.pdf", "");
        jdbc.update("INSERT INTO document_link (document_id, entity_type, entity_id, package_id, "
                + "stage_code, doc_role, link_origin, created_at) "
                + "VALUES (?, 'PACKAGE', ?, ?, 1, 'MIGRATED', 'MANUAL', now())",
                documentId, pkg.getId(), pkg.getId());
        String key = "legacy_authority_" + documentId;
        jdbc.update("INSERT INTO document_metadata (document_id, metadata_key, metadata_value, "
                + "source, confidence, created_at, updated_at) "
                + "VALUES (?, ?, 'Member, Distribution', 'OCR', 0.88, now(), now())",
                documentId, key);

        migrationService.migrateDocumentMetadata(false, userId());

        assertTrue(fieldRepository.findByPackageId(pkg.getId()).stream()
                        .filter(f -> key.equals(f.getFieldKey()))
                        .noneMatch(f -> f.isConfirmed()),
                "a migrated value must still be reviewed by somebody");
    }

    @Test
    void metadataOnAnUnlinkedDocumentIsReportedRatherThanDropped() {
        // Every captured field hangs off a package. A value with no package has nowhere to
        // go - the migration says so instead of quietly losing it.
        long documentId = insertDocument("unlinked.pdf", "");
        jdbc.update("INSERT INTO document_metadata (document_id, metadata_key, metadata_value, "
                + "source, created_at, updated_at) VALUES (?, ?, 'orphan value', 'OCR', now(), now())",
                documentId, "orphan_key_" + documentId);

        MigrationReport report = migrationService.migrateDocumentMetadata(true, userId());

        assertTrue(report.needsAttention.stream().anyMatch(s -> s.contains(String.valueOf(documentId))),
                "the orphan should be named in the report: " + report.needsAttention);
        assertTrue(report.reconciles(), report.toString());
    }

    @Test
    void aDryRunWritesNothing() {
        ProcurementPackage pkg = aPackage("MIG-" + System.nanoTime());
        long documentId = insertDocument("dryrun.pdf", "");
        jdbc.update("INSERT INTO document_link (document_id, entity_type, entity_id, package_id, "
                + "stage_code, doc_role, link_origin, created_at) "
                + "VALUES (?, 'PACKAGE', ?, ?, 1, 'MIGRATED', 'MANUAL', now())",
                documentId, pkg.getId(), pkg.getId());
        jdbc.update("INSERT INTO document_metadata (document_id, metadata_key, metadata_value, "
                + "source, created_at, updated_at) VALUES (?, ?, 'not written', 'OCR', now(), now())",
                documentId, "dryrun_key_" + documentId);

        MigrationReport report = migrationService.migrateDocumentMetadata(true, userId());

        assertTrue(report.dryRun);
        assertTrue(report.migrated >= 1, "the report should say what it would do");
        assertTrue(fieldRepository.findByPackageId(pkg.getId()).isEmpty(),
                "but nothing should have been written");
    }

    @Test
    void runningTwiceDoesNotDuplicate() {
        // It will be run twice: once in staging, once for real. And if the first run is
        // interrupted, someone will start it again.
        ProcurementPackage pkg = aPackage("MIG-" + System.nanoTime());
        long documentId = insertDocument("twice.pdf", "");
        jdbc.update("INSERT INTO document_link (document_id, entity_type, entity_id, package_id, "
                + "stage_code, doc_role, link_origin, created_at) "
                + "VALUES (?, 'PACKAGE', ?, ?, 1, 'MIGRATED', 'MANUAL', now())",
                documentId, pkg.getId(), pkg.getId());
        jdbc.update("INSERT INTO document_metadata (document_id, metadata_key, metadata_value, "
                + "source, created_at, updated_at) VALUES (?, ?, 'once only', 'OCR', now(), now())",
                documentId, "twice_key_" + documentId);

        migrationService.migrateDocumentMetadata(false, userId());
        long afterFirst = fieldRepository.findByPackageId(pkg.getId()).size();
        MigrationReport second = migrationService.migrateDocumentMetadata(false, userId());

        assertEquals(afterFirst, fieldRepository.findByPackageId(pkg.getId()).size(),
                "the second run must not add anything");
        assertTrue(second.skipped >= 1, "and should report what it left alone");
    }

    // ------------------------------------------------------- document re-linking

    @Test
    void anOrphanDocumentNamingOnePackageIsLinkedToIt() {
        ProcurementPackage pkg = aPackage("RELINK-" + System.nanoTime());
        long documentId = insertDocument("names-one.pdf",
                "Contract agreement for package " + pkg.getPackageNumber() + " dated 2026-01-01");

        migrationService.relinkOrphanDocuments(false, userId());

        Long linked = jdbc.queryForObject(
                "SELECT count(*) FROM document_link WHERE document_id = ? AND package_id = ?",
                Long.class, documentId, pkg.getId());
        assertEquals(1L, linked);
    }

    @Test
    void anOrphanNamingTwoPackagesIsLeftForAPerson() {
        // A wrong link is worse than an absent one: nobody goes looking for a document
        // that is already filed somewhere plausible
        ProcurementPackage a = aPackage("AMBIG-A-" + System.nanoTime());
        ProcurementPackage b = aPackage("AMBIG-B-" + System.nanoTime());
        long documentId = insertDocument("names-two.pdf",
                "Comparison of " + a.getPackageNumber() + " and " + b.getPackageNumber());

        MigrationReport report = migrationService.relinkOrphanDocuments(false, userId());

        Long linked = jdbc.queryForObject(
                "SELECT count(*) FROM document_link WHERE document_id = ?", Long.class, documentId);
        assertEquals(0L, linked, "an ambiguous document must not be guessed at");
        assertTrue(report.needsAttention.stream().anyMatch(s -> s.contains(String.valueOf(documentId))),
                "and it must appear on the list for a person: " + report.needsAttention);
    }

    @Test
    void theReportReconcilesOrSaysSo() {
        MigrationReport report = migrationService.relinkOrphanDocuments(true, userId());
        assertTrue(report.reconciles(), report.toString());
        assertFalse(report.toString().contains("DO NOT RECONCILE"));
    }
}
