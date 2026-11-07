package org.example.panhabackend.dto.report;

import java.util.List;

/**
 * Format-neutral tabular report rendered to Excel or PDF by the export service.
 */
public record ReportTable(String title,
                          List<String> subtitleLines,
                          List<String> headers,
                          List<List<String>> rows) {
}
