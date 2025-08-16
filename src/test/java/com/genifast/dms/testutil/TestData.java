package com.genifast.dms.testutil;

import java.time.Instant;
import java.util.List;
import java.util.StringJoiner;

/**
 * Centralized test fixtures and helpers for document sharing scenarios (1–3).
 * Values align with ai/kich ban.md and existing unit tests.
 */
public final class TestData {
    private TestData() {}

    // Documents used across scenarios
    public static final long DOC_INTERNAL_01 = 10101L; // doc-01 (INTERNAL, PENDING)
    public static final long DOC_INTERNAL_01_ALT = 10102L; // same doc space for negative case
    public static final long DOC_PRIVATE_07 = 10700L; // doc-07 (PRIVATE, APPROVED)
    public static final long DOC_PRIVATE_07_ALT = 10701L; // private negative case

    // Scenario 2 doc ids
    public static final long DOC_SC2_FWD_TIMEBOUND = 202L;
    public static final long DOC_SC2_EXTERNAL_FORBIDDEN = 203L;
    public static final long DOC_SC2_PUBLIC_SHAREABLE = 204L;

    // Scenario 3 doc ids
    public static final long DOC_SC3_EXTERNAL_READONLY = 301L;
    public static final long DOC_SC3_SHAREABLE = 302L;
    public static final long DOC_SC3_RECIPIENT_INACTIVE = 303L;
    public static final long DOC_SC3_PUBLIC_LINK_DL_FALSE = 304L;
    public static final long DOC_SC3_PUBLIC_LINK_DL_TRUE = 305L;

    // Scenario 4 doc ids
    public static final long DOC_SC4_SHAREABLE_TIMEBOUND = 401L; // doc-04 (INTERNAL, APPROVED)

    // Scenario 5 doc ids
    public static final long DOC_PUBLIC_06 = 10600L;   // doc-06 (PUBLIC, APPROVED)
    public static final long DOC_INTERNAL_08 = 10800L; // doc-08 (INTERNAL, DRAFT)

    // Scenario 6 doc ids
    public static final long DOC_EXTERNAL_05 = 10500L; // doc-05 (EXTERNAL, APPROVED)

    // Recipients / Users
    public static final long USER_GV = 7001L;   // giáo viên (internal)
    public static final long USER_PP = 7101L;   // phụ huynh (internal)
    public static final long USER_CB = 7201L;   // cán bộ không thuộc private_docs
    public static final long USER_SC2 = 2002L;  // scenario 2 internal
    public static final long USER_EXTERNAL = 99001L; // outside org
    public static final long USER_SC3 = 3002L;  // scenario 3 internal
    public static final long USER_INACTIVE = 8888L;  // inactive

    // Permission strings
    public static final String PERM_READONLY = "documents:share:readonly";
    public static final String PERM_FORWARDABLE = "documents:share:forwardable";
    public static final String PERM_TIMEBOUND = "documents:share:timebound";
    public static final String PERM_SHAREABLE = "documents:share:shareable";

    // Sample time window used in tests
    public static final Instant TIME_FROM_SAMPLE = Instant.parse("2025-08-05T00:00:00Z");
    public static final Instant TIME_TO_SAMPLE   = Instant.parse("2025-08-12T23:59:59Z");

    /**
     * Build JSON body for POST /api/v1/documents/{id}/share.
     * Any null optional field will be omitted from JSON.
     */
    public static String buildShareBody(Long recipientId,
                                        List<String> permissions,
                                        Instant fromDate,
                                        Instant toDate,
                                        Boolean isShareToExternal) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');

        // recipient_id
        sb.append("\"recipient_id\": ").append(recipientId);

        // permissions
        sb.append(", \"permissions\": [");
        StringJoiner joiner = new StringJoiner(", ");
        for (String p : permissions) {
            joiner.add("\"" + escape(p) + "\"");
        }
        sb.append(joiner).append(']');

        // from_date
        if (fromDate != null) {
            sb.append(", \"from_date\": \"")
              .append(fromDate.toString())
              .append("\"");
        }

        // to_date
        if (toDate != null) {
            sb.append(", \"to_date\": \"")
              .append(toDate.toString())
              .append("\"");
        }

        // is_share_to_external
        if (isShareToExternal != null) {
            sb.append(", \"is_share_to_external\": ")
              .append(isShareToExternal);
        }

        sb.append('}');
        return sb.toString();
    }

    /** Escape a JSON string value minimally for test purposes. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
