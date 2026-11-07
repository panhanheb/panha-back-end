package org.example.panhabackend.mapper;

import org.springframework.util.StringUtils;

final class MapperUtils {

    private MapperUtils() {
    }

    /** Lower-cases and trims an email; blank values become null so the unique index allows many empties. */
    static String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
    }
}
