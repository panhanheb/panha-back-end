package org.example.panhabackend.service;

import org.example.panhabackend.dto.report.ReportTable;

/**
 * Renders a {@link ReportTable} into downloadable documents.
 */
public interface ExportService {

    byte[] toExcel(ReportTable table);

    byte[] toPdf(ReportTable table);
}
