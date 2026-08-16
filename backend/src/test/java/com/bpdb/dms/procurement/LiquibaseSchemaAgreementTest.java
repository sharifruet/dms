package com.bpdb.dms.procurement;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;

import org.hibernate.SessionFactory;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Does every column the code maps actually exist in the schema Liquibase builds?
 *
 * <p>This is the check that would have caught the defect that stopped the application
 * dead: {@code extracted_field_history}, {@code budget_consumption} and {@code ocr_page}
 * were each missing {@code created_at}, so every field capture, every budget posting and
 * every OCR page write failed against PostgreSQL with
 * {@code ERROR: column "created_at" does not exist}.
 *
 * <p>No amount of unit testing could have found it. The rest of the suite runs on H2 with
 * the schema generated from the entities, where the two agree by construction. Only the
 * real changelog can disagree, so only the real changelog can be checked.
 *
 * <p>Two assertions, for two different jobs. The procurement schema must agree outright —
 * that is the module under active development and it is clean today. The rest of the
 * application carries pre-existing drift that predates this work, so it is held as a
 * ratchet: the known tables are listed, and any table drifting that is <em>not</em> on the
 * list fails. That keeps the check useful now instead of red until a large legacy cleanup
 * happens.
 */
class LiquibaseSchemaAgreementTest extends PostgresLiquibaseTest {

    /**
     * Tables known to disagree with their entities before this check existed.
     *
     * <p>These are real defects, not noise — each is a feature that would fail the moment
     * it ran against PostgreSQL. They are quarantined rather than ignored: nothing new may
     * join the list, and the intent is for it to shrink. {@code document_versions} is the
     * one to look at first; it is on a path users actually reach, unlike the enterprise
     * features around it.
     */
    private static final Set<String> KNOWN_LEGACY_DRIFT = Set.of(
            // Tables that exist but are missing columns their entity maps.
            // document_versions has left this list - it was on a path users reach, and
            // changeset 043 fixed it.
            "backup_records",
            "dashboards",
            "integration_configs",
            "reports",
            "system_health_checks",
            "tenants",

            // Entities with no table at all: these features cannot function against a real
            // database, because the first query throws. Each has a controller, so each is
            // a reachable endpoint that returns a 500.
            //
            // Left as a decision rather than guessed at. Writing ~70 columns of schema for
            // four features outside the procurement revamp, none of them in the pilot
            // scope and none verifiable from here, is precisely the "pressure to integrate
            // everything" the plan calls out as R-5. The three that were genuinely dead -
            // no controller, no service, no reference anywhere - have been deleted instead.
            "document_templates",
            "ml_models",
            "optimization_tasks",
            "smart_folders",
            "webhooks");

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @Test
    void theProcurementSchemaAgreesWithItsEntities() throws Exception {
        List<Disagreement> problems = findDisagreements().stream()
                .filter(d -> isProcurementTable(d.table))
                .toList();

        assertTrue(problems.isEmpty(),
                "The procurement entities and the Liquibase schema disagree. Every line here "
                        + "is a statement that will fail at runtime against PostgreSQL:\n  "
                        + join(problems));
    }

    /**
     * Types, not just presence. A column that exists but holds the wrong kind of value is
     * the quieter half of the same problem — Hibernate's own {@code validate} found one in
     * the legacy schema ({@code analytics.metric_value} is NUMERIC where the entity expects
     * a float). Turning {@code validate} on globally is blocked until the legacy drift
     * above is cleared, so this covers the module we own in the meantime.
     */
    @Test
    void procurementColumnsHoldTheKindOfValueTheirFieldExpects() throws Exception {
        List<String> problems = new ArrayList<>();
        TreeMap<String, TreeMap<String, String>> types = readColumnTypes();

        for (String table : PROCUREMENT_TABLES) {
            TreeMap<String, String> columns = types.get(table);
            if (columns == null) {
                continue; // absence is the other test's job
            }
            columns.forEach((column, sqlType) -> {
                String kind = kindOf(sqlType);
                if (kind == null) {
                    return;
                }
                // Amounts must be exact. A money column stored as a float is a rounding
                // bug waiting for a large enough contract.
                if (column.endsWith("_amount") || column.equals("price_lac_bdt")
                        || column.endsWith("_cost") || column.endsWith("_value")) {
                    if ("float".equals(kind)) {
                        problems.add(table + "." + column + " holds money as " + sqlType
                                + "; it should be numeric so amounts stay exact");
                    }
                }
                if (column.endsWith("_date") && !"date".equals(kind) && !"timestamp".equals(kind)) {
                    problems.add(table + "." + column + " is " + sqlType + ", not a date type");
                }
                if (column.startsWith("is_") && !"boolean".equals(kind)) {
                    problems.add(table + "." + column + " is " + sqlType + ", not boolean");
                }
            });
        }

        assertTrue(problems.isEmpty(),
                "Procurement columns whose type does not match what the code puts in them:\n  "
                        + String.join("\n  ", problems));
    }

    @Test
    void noTableOutsideTheKnownLegacyListHasDrifted() throws Exception {
        List<Disagreement> unexpected = findDisagreements().stream()
                .filter(d -> !isProcurementTable(d.table))
                .filter(d -> !KNOWN_LEGACY_DRIFT.contains(d.table))
                .toList();

        assertTrue(unexpected.isEmpty(),
                "A table has drifted from its entity that was not drifting before. Either the "
                        + "changeset is missing a column or the entity gained one without a "
                        + "changeset:\n  " + join(unexpected));
    }

    @Test
    void theKnownLegacyDriftListDoesNotOutliveTheProblem() throws Exception {
        Set<String> stillDrifting = new TreeSet<>();
        for (Disagreement d : findDisagreements()) {
            stillDrifting.add(d.table);
        }
        Set<String> fixedButStillListed = new TreeSet<>(KNOWN_LEGACY_DRIFT);
        fixedButStillListed.removeAll(stillDrifting);

        assertTrue(fixedButStillListed.isEmpty(),
                "These tables no longer drift and should be removed from KNOWN_LEGACY_DRIFT, "
                        + "so the list keeps shrinking and cannot quietly hide a regression: "
                        + fixedButStillListed);
    }

    // ------------------------------------------------------------------ internals

    private static boolean isProcurementTable(String table) {
        return PROCUREMENT_TABLES.contains(table);
    }

    /** The tables this module owns; everything else belongs to the wider DMS. */
    private static final Set<String> PROCUREMENT_TABLES = Set.of(
            "procurement_package", "package_stage", "document_link", "tender", "tender_opening",
            "evaluation", "ber_bidder", "contract_approval", "noa", "performance_security",
            "contract", "contract_package", "letter_of_credit", "lc_amendment", "price_schedule",
            "price_schedule_line", "production_schedule", "progress_report", "inspection_event",
            "delivery", "delivery_line", "invoice", "invoice_delivery_link", "payment",
            "payment_invoice_link", "warranty", "contract_closure", "extracted_field",
            "extracted_field_history", "ocr_job", "ocr_result", "ocr_page", "budget_entry",
            "budget_consumption", "department_budget", "stage_document_requirement");

    private record Disagreement(String table, String detail) {
    }

    private static String join(List<Disagreement> problems) {
        List<String> lines = new ArrayList<>();
        problems.forEach(p -> lines.add(p.detail));
        return String.join("\n  ", lines);
    }

    private List<Disagreement> findDisagreements() throws Exception {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        MappingMetamodelImplementor metamodel =
                (MappingMetamodelImplementor) sessionFactory.getMetamodel();
        TreeMap<String, Set<String>> actual = readSchema();
        List<Disagreement> problems = new ArrayList<>();

        for (EntityType<?> entity : entityManagerFactory.getMetamodel().getEntities()) {
            AbstractEntityPersister persister;
            try {
                persister = (AbstractEntityPersister)
                        metamodel.getEntityDescriptor(entity.getJavaType());
            } catch (RuntimeException e) {
                continue;
            }

            String table = persister.getTableName().toLowerCase();
            String bare = table.contains(".") ? table.substring(table.lastIndexOf('.') + 1) : table;
            Set<String> columns = actual.get(bare);
            if (columns == null) {
                problems.add(new Disagreement(bare, String.format(
                        "table %-30s is mapped by %s but does not exist", bare, entity.getName())));
                continue;
            }

            for (Attribute<?, ?> attribute : entity.getAttributes()) {
                if (attribute.isCollection() || attribute.isAssociation()) {
                    continue;
                }
                String[] mapped;
                try {
                    mapped = persister.getPropertyColumnNames(attribute.getName());
                } catch (RuntimeException e) {
                    continue;
                }
                for (String column : mapped) {
                    if (column == null || column.isBlank()) {
                        continue;
                    }
                    String bareColumn = column.replace("\"", "").toLowerCase();
                    if (!columns.contains(bareColumn)) {
                        problems.add(new Disagreement(bare, String.format(
                                "%-30s is missing %-22s (mapped by %s.%s)",
                                bare, bareColumn, entity.getName(), attribute.getName())));
                    }
                }
            }

            for (String idColumn : persister.getIdentifierColumnNames()) {
                if (!columns.contains(idColumn.toLowerCase())) {
                    problems.add(new Disagreement(bare, String.format(
                            "%-30s is missing identifier column %s", bare, idColumn)));
                }
            }
        }
        return problems;
    }

    /** Broad family of a PostgreSQL type, which is all these checks need to distinguish. */
    private static String kindOf(String sqlType) {
        String t = sqlType.toLowerCase();
        if (t.startsWith("numeric") || t.startsWith("decimal")) return "numeric";
        if (t.contains("double") || t.startsWith("real") || t.startsWith("float")) return "float";
        if (t.startsWith("timestamp")) return "timestamp";
        if (t.equals("date")) return "date";
        if (t.startsWith("bool")) return "boolean";
        if (t.contains("char") || t.equals("text")) return "text";
        if (t.startsWith("int") || t.startsWith("bigint") || t.startsWith("smallint")) return "integer";
        return null;
    }

    private TreeMap<String, TreeMap<String, String>> readColumnTypes() throws Exception {
        TreeMap<String, TreeMap<String, String>> tables = new TreeMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "public", "%", "%")) {
                while (rs.next()) {
                    tables.computeIfAbsent(rs.getString("TABLE_NAME").toLowerCase(),
                                    t -> new TreeMap<>())
                            .put(rs.getString("COLUMN_NAME").toLowerCase(),
                                    rs.getString("TYPE_NAME").toLowerCase());
                }
            }
        }
        return tables;
    }

    private TreeMap<String, Set<String>> readSchema() throws Exception {
        TreeMap<String, Set<String>> tables = new TreeMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(null, "public", "%", "%")) {
                while (rs.next()) {
                    tables.computeIfAbsent(rs.getString("TABLE_NAME").toLowerCase(),
                            t -> new HashSet<>()).add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
        }
        return tables;
    }
}
