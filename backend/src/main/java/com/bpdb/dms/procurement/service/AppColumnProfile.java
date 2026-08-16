package com.bpdb.dms.procurement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How to find each APP field in a workbook, per fiscal year (REQ-1.1).
 *
 * <p>Columns are located by <em>header label</em>, not by fixed offsets. The supplied
 * BPDB workbook happens to put Package No. in column B, but nothing here depends on that:
 * inserting a column, or reordering them, needs no change at all. A genuinely different
 * layout - different wording for a heading - is handled by adding an alias, which is the
 * "configuration rather than code change" REQ-1.1 asks for.
 *
 * <p>Profiles are keyed by fiscal year with a default fallback, so a future year that
 * renames its headings can be described without disturbing the years already imported.
 */
public final class AppColumnProfile {

    /** The fields Stage 1 needs from an APP row. */
    public enum Field {
        STATUS,
        PACKAGE_NUMBER,
        LOT_NUMBER,
        DESCRIPTION,
        UNIT,
        QUANTITY,
        PROCUREMENT_METHOD_TYPE,
        APPROVING_AUTHORITY,
        SOURCE_OF_FUND,
        UNIT_COST,
        TOTAL_COST
    }

    private final String name;
    private final Map<Field, List<String>> aliases;

    private AppColumnProfile(String name, Map<Field, List<String>> aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    public String getName() {
        return name;
    }

    public Map<Field, List<String>> getAliases() {
        return aliases;
    }

    /**
     * The layout of the BPDB APP workbook as supplied for FY 2022-23.
     *
     * <p>Two header rows are in play: the main one carries "Estd. Cost (Tk. in Lac)"
     * spanning two sub-columns, and the row beneath splits it into "Unit Cost" and
     * "Total Cost". Both rows are scanned, so either wording resolves.
     */
    public static final AppColumnProfile BPDB_DEFAULT = new AppColumnProfile("BPDB-DEFAULT",
            defaults());

    private static Map<Field, List<String>> defaults() {
        Map<Field, List<String>> m = new LinkedHashMap<>();
        m.put(Field.STATUS, List.of("status"));
        m.put(Field.PACKAGE_NUMBER, List.of("package no", "package number", "package"));
        m.put(Field.LOT_NUMBER, List.of("lot no", "lot number", "lot"));
        m.put(Field.DESCRIPTION, List.of(
                "description of the materials", "description of materials", "description",
                "particulars"));
        m.put(Field.UNIT, List.of("unit"));
        m.put(Field.QUANTITY, List.of("quantity", "qty"));
        m.put(Field.PROCUREMENT_METHOD_TYPE, List.of(
                "procurement method & type", "procurement method and type",
                "procurement method", "method & type"));
        m.put(Field.APPROVING_AUTHORITY, List.of(
                "contract approving authority", "approving authority", "authority"));
        m.put(Field.SOURCE_OF_FUND, List.of("source of fund", "source of funds", "fund source"));
        m.put(Field.UNIT_COST, List.of("unit cost"));
        // Strictly "total cost". NOT "estd. cost": in the BPDB layout that is the group
        // heading spanning both cost sub-columns, and it sits in the Unit Cost column -
        // matching it here silently reads unit cost as the package value, which is only
        // invisible because every row in the supplied file has quantity 1.
        m.put(Field.TOTAL_COST, List.of("total cost"));
        return m;
    }

    /**
     * Second-choice headings, used only when the strict pass found nothing.
     *
     * <p>A sheet that gives a single undivided cost column headed "Estd. Cost" is
     * legitimate - it just has no unit/total split - so that wording is honoured once it
     * is clear there is no "Total Cost" column to prefer.
     */
    private static final Map<Field, List<String>> FALLBACKS = Map.of(
            Field.TOTAL_COST, List.of("estd. cost", "estimated cost", "estd cost"));

    public List<String> fallbackAliases(Field field) {
        return FALLBACKS.getOrDefault(field, List.of());
    }

    /** Does this header text identify the field on the second-choice pass? */
    public boolean matchesFallback(Field field, String headerText) {
        String normalized = normalize(headerText);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String alias : fallbackAliases(field)) {
            if (normalized.equals(alias) || normalized.startsWith(alias)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The profile for a fiscal year. Only one layout is known so far, so every year
     * resolves to it; a year that diverges gets its own entry here rather than a change
     * to the parser.
     */
    public static AppColumnProfile forFiscalYear(Integer fiscalYear) {
        return BPDB_DEFAULT;
    }

    /** Does this header cell text identify the given field? */
    public boolean matches(Field field, String headerText) {
        if (headerText == null) {
            return false;
        }
        String normalized = normalize(headerText);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String alias : aliases.getOrDefault(field, List.of())) {
            // startsWith, not equals: real headings carry trailing units and footnotes,
            // e.g. "Estd. Cost (Tk. in Lac)" or "Package No."
            if (normalized.equals(alias) || normalized.startsWith(alias)) {
                return true;
            }
        }
        return false;
    }

    /** Lower-case, collapse whitespace, drop trailing punctuation and footnote markers. */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String out = s.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        // strip a trailing "(1)"-style column number or a stray colon
        out = out.replaceAll("\\s*\\(\\d+\\)$", "").replaceAll(":$", "").trim();
        return out;
    }
}
